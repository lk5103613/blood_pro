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
 * �����豸�����ӶϿ��ķ��� 
 * @author Like
 * 
 */
public class BluetoothLeService extends Service {
	
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	@SuppressWarnings("unused")
	private int mConnectionState = STATE_DISCONNECTED;
	private final IBinder mBinder = new LocalBinder();
	
	// ����״̬
	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	
	public final static String ACTION_GATT_CONNECTED = "com.wm.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.wm.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.wm.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.wm.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.wm.bluetooth.le.EXTRA_DATA";
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		// ������״̬�����ı�ʱ����
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				// ������ӳɹ���ͨ���㲥��ʽ��֪MainAcivity
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);
				mBluetoothGatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				// ������ӶϿ���ͨ���㲥��ʽ��֪MainActivity
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				broadcastUpdate(intentAction);
			}
		}
		
		// ����serviceʱ����
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				System.out.println("onServicesDiscovered received: " + status);
			}
		};
		
		// ��characteristicʱ����
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		// ��descriptorд������ʱ����
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
		}

		// ���ݴ�characteristic����ʱ����
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}

		// ����characteristicд��ʱ����
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
		};
		
	};
	
	// ���͹㲥
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}
	
	// ������ת����16���ƺ��͹㲥
	private void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		final byte[] data = characteristic.getValue();
		// ������ת����16����
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
	
	// ��ʼ������
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
	
	// ����address�����豸
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			return false;
		}
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null) {
			return false;
		}
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}
	
	// �������������Ҫ�ر����Ӻͷ���
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.disconnect();
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}
	
	// ��characteristicд��value
	public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.writeCharacteristic(characteristic);
	}
	
	// ��ȡcharacteristic�е�value
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}
	
	// �������service
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;
		return mBluetoothGatt.getServices();
	}
	
	// ����UUID��ȡservice
	public BluetoothGattService getServiceByUuid(String uuid) {
		if(mBluetoothGatt == null) {
			return null;
		}
		return mBluetoothGatt.getService(UUID.fromString(uuid));
	}
	
	// �Ͽ�����
	public void disconnect() {
		if(mBluetoothGatt == null) {
			return;
		}
		this.mBluetoothGatt.disconnect();
	}
	
	// ������ر�characteristic��֪
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
}
