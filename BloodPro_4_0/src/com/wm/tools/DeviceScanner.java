package com.wm.tools;

import java.util.Locale;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

public class DeviceScanner {
	
	public final static int STATE_BEGIN_SCAN = 0;
	public final static int STATE_END_SCAN = 1;

	// ɨ��ʱ�䣬�������ʱ��û��ɨ�赽�豸���϶�ɨ��ʧ��
	private final static int SCAN_PERIOD = 10000;
	private static DeviceScanner mDeviceScanner = null;
	// Ѫѹ�Ƶ��豸����
	private final static String BLOOD_PRESSURE_NAME = "simplebleperipheral";
	// ���ɨ�赽���豸
	private BluetoothDevice mDevice = null;
	private Handler mHandler = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private boolean mScanning = false;
	private ScanCallback mCallback = null;

	private DeviceScanner(BluetoothAdapter bluetoothAdapter,
			ScanCallback callback) {
		this.mHandler = new Handler();
		this.mBluetoothAdapter = bluetoothAdapter;
		this.mCallback = callback;
	}

	public static DeviceScanner getInstance(BluetoothAdapter bluetoothAdapter,
			ScanCallback callback) {
		if (mDeviceScanner == null)
			mDeviceScanner = new DeviceScanner(bluetoothAdapter, callback);
		return mDeviceScanner;
	}

	public interface ScanCallback {
		
		void onScanStateChange(int scanState);

		void onScanSuccess(BluetoothDevice device);

		void onScanFailed();

	}

	/**
	 * ����ɨ��Ŀ�ʼ�����
	 * 
	 * @param enable
	 *            ���Ϊtrue��ʼɨ�裬false����ɨ��
	 */
	@SuppressWarnings("deprecation")
	public void scanLeDevice(final boolean enable) {
		if (enable) {
			mDevice = null;
			mCallback.onScanStateChange(STATE_BEGIN_SCAN);
			// ��ָ��ʱ��֮��ֹͣɨ��
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					scanLeDevice(false);
				}
			}, SCAN_PERIOD);
			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mCallback.onScanStateChange(STATE_END_SCAN);
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			if(mDevice == null) 
				mCallback.onScanFailed();
			else
				mCallback.onScanSuccess(mDevice);
		}
	}

	private LeScanCallback mLeScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (isBloodPressure(device)) {
				mDevice = device;
				scanLeDevice(false);
			}
		}
	};

	/**
	 * �����ж�ɨ�赽���豸�Ƿ���Ѫѹ��
	 * 
	 * @param device
	 *            ɨ�赽���豸
	 * @return �����Ѫѹ�Ʒ���true�� ���򷵻�false
	 */
	public boolean isBloodPressure(BluetoothDevice device) {
		if (device.getName().toLowerCase(Locale.getDefault())
				.equals(BLOOD_PRESSURE_NAME)) {
			return true;
		}
		return false;
	}
	
	/**
	 * �����ж��Ƿ�������ɨ��״̬
	 * @return �������ɨ�践��true�����ڷ���false
	 */
	public boolean isScanning() {
		return mScanning;
	}

}
