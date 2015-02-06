package com.wm.bloodpro_4_0;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class DeviceListActivity extends Activity implements OnItemClickListener {
	
	@InjectView(R.id.device_list)
	ListView mDeviceList; 
	@InjectView(R.id.progress_scanning)
	ProgressBar mProgressScanning;
	@InjectView(R.id.lbl_found_device)
	TextView lblFoundDevice;
	
	public static String DEVICE_ADDRESS = "device_address";
	
	// after SCAN_PERIOD ms, stop scan.
	private static final long SCAN_PERIOD = 10000;
	private Handler mHandler;
	private boolean mScanning;
	private BluetoothAdapter mBluetoothAdapter;
	private Context mContext;
	private ArrayAdapter<String> mDeviceAdapter;
	private List<BluetoothDevice> mDevices;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_device_list);
		ButterKnife.inject(this);
		// init params
		mHandler = new Handler();
		this.mContext = DeviceListActivity.this;
		mDevices = new ArrayList<BluetoothDevice>();
		mDeviceAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		mDeviceList.setAdapter(mDeviceAdapter);
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		mDeviceList.setOnItemClickListener(this);
		// begin scan
		scanLeDevice(true);
	}
	
	@SuppressWarnings("deprecation")
	private void scanLeDevice(final boolean enable) {
		if(enable) {
			lblFoundDevice.setVisibility(View.GONE);
			mDeviceAdapter.clear();
        	mDeviceAdapter.notifyDataSetChanged();
        	mProgressScanning.setVisibility(View.VISIBLE);
			mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mProgressScanning.setVisibility(View.GONE);
                    if(mDeviceAdapter.getCount() == 0) {
                    	String notFoundDevice = getResources().getString(R.string.not_found_device);
                    	mDeviceAdapter.add(notFoundDevice);
                    	mDeviceAdapter.notifyDataSetChanged();
                    }
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mDevices.clear();
			lblFoundDevice.setVisibility(View.GONE);
			mDeviceAdapter.clear();
        	mDeviceAdapter.notifyDataSetChanged();
        	mProgressScanning.setVisibility(View.VISIBLE);
			mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mProgressScanning.setVisibility(View.GONE);
            if(mDeviceAdapter.getCount() == 0) {
            	String notFoundDevice = getResources().getString(R.string.not_found_device);
            	mDeviceAdapter.add(notFoundDevice);
            	mDeviceAdapter.notifyDataSetChanged();
            }
		}
	}
	
	private LeScanCallback mLeScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			final BluetoothDevice newDevice = device;
			final String deviceName = device.getName();
			final String address = device.getAddress();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(lblFoundDevice.getVisibility() == View.GONE) {
						lblFoundDevice.setVisibility(View.VISIBLE);
					}
					if(!mDevices.contains(newDevice)) {
						mDevices.add(newDevice);
						mDeviceAdapter.add(deviceName + "\n" + address);
						mDeviceAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	};
	
	@OnClick(R.id.btn_scan_device)
	public void scan(View v) {
		if(mScanning) {
			String remindStr = getResources().getString(R.string.scanning_now);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
		} else {
			scanLeDevice(true);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		scanLeDevice(false);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String str = getResources().getString(R.string.not_found_device);
		String content = mDeviceAdapter.getItem(position);
		if(content.trim().equals(str)) {
			return;
		}
		BluetoothDevice device = mDevices.get(position);
		Intent resultIntent = new Intent();
		resultIntent.putExtra(DEVICE_ADDRESS, device.getAddress());
		setResult(RESULT_OK, resultIntent);
		finish();
	}
	
}
