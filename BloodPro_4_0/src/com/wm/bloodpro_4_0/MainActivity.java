package com.wm.bloodpro_4_0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import com.wm.tools.ProgressWheel;

public class MainActivity extends Activity {
	// request code to open bluetooth
	public static int REQUEST_ENABLE_BT = 1;
	
	@InjectView(R.id.progress_bar)
	ProgressWheel progress;
	
	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler;
	private Context mContext;
	// after SCAN_PERIOD ms, stop scan.
	private static final long SCAN_PERIOD = 10000;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);
		// init params
		mHandler = new Handler();
		mContext = MainActivity.this;
		// if the device doesn't support ble, close the app.
		if(!checkSupport()) {
			String remindStr = getResources().getString(R.string.remind_device_not_support);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			finish();
		}
		// if the ble is closed, request user to open.
		requestBluetooth();
	}
	
	// check the device support ble or not
	private boolean checkSupport() {
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}
		return true;
	}
	
	// request the access to the bluetooth
	private void requestBluetooth() {
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
		
	
	@OnClick(R.id.progress_bar)
	public void clickProgress(View v) {
		if(progress.isSpinning()) {
			progress.stopSpinning();
			progress.setText("¿ªÊ¼¼ì²â");
		} else {
			progress.spin();
			progress.setText("Í£Ö¹¼ì²â");
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BT) {
			// if user don't open ble, close the app
			if(resultCode == RESULT_CANCELED) {
				String remindStr = getResources().getString(R.string.remind_ble_must_open);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}
	
}
