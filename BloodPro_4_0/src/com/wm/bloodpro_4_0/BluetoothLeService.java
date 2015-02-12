package com.wm.bloodpro_4_0;

import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * 
 * 控制设备的连接断开的服务 
 * @author Like
 * 
 */
public class BluetoothLeService extends Service {
	
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;
	private final IBinder mBinder = new LocalBinder();
	
	// 连接状态
	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;
	
	public final static String ACTION_GATT_CONNECTED = "com.wm.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.wm.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.wm.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.wm.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.wm.bluetooth.le.EXTRA_DATA";
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		// 当连接状态发生改变时触发
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				// 如果连接成功，通过广播方式告知MainAcivity
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);
				mBluetoothGatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				// 如果连接断开，通过广播方式告知MainActivity
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				broadcastUpdate(intentAction);
			}
		}
		
		// 发现service时触发
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			}
		};
		
		// 读characteristic时触发
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		// 向descriptor写入数据时触发
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
		}

		// 数据从characteristic返回时触发
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}

		// 在向characteristic写入时触发
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
		};
		
	};
	
	// 发送广播
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}
	
	// 将数据转换成16进制后发送广播
	private void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		final byte[] data = characteristic.getValue();
		// 将数据转换成16进制
		if (data != null && data.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(
					data.length);
			for (byte byteChar : data) {
				stringBuilder.append(String.format("%02x ", byteChar));
			}
			intent.putExtra(EXTRA_DATA, stringBuilder.toString().trim());
		}
		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		close();
		return super.onUnbind(intent);
	}
	
	// 初始化参数
	public boolean initialize() {
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			return false;
		}

		return true;
	}
	
	// 根据address连接设备
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			mConnectionState = STATE_DISCONNECTED;
			return false;
		}
		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null) {
			mConnectionState = STATE_DISCONNECTED;
			return false;
		}
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				mConnectionState = STATE_DISCONNECTED;
				return false;
			}
		}
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}
	
	// 在做完操作后，需要关闭连接和服务
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.disconnect();
		mConnectionState = STATE_DISCONNECTED;
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}
	
	// 向characteristic写入value
	public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.writeCharacteristic(characteristic);
	}
	
	// 读取characteristic中的value
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}
	
	// 获得所有service
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;
		return mBluetoothGatt.getServices();
	}
	
	// 根据UUID获取service
	public BluetoothGattService getServiceByUuid(String uuid) {
		if(mBluetoothGatt == null) {
			return null;
		}
		return mBluetoothGatt.getService(UUID.fromString(uuid));
	}
	
	// 断开连接
	public void disconnect() {
		this.mConnectionState = STATE_DISCONNECTED;
		if(mBluetoothGatt == null) {
			return;
		}
		this.mBluetoothGatt.disconnect();
	}
	
	// 开启或关闭characteristic告知
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
				.fromString("00002902-0000-1000-8000-00805f9b34fb"));
		if (descriptor != null) {
			descriptor
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
	}
	
	public int getConnectState() {
		return mConnectionState;
	}
}
