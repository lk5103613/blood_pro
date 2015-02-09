package com.wm.bloodpro_4_0;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import com.wm.tools.ProgressWheel;
import com.wm.tools.Uuids;
import com.wn.entity.ResultException;
import com.wn.entity.ResultInfo;

public class MainActivity extends Activity {
	
	public static String TAG = "test";
	
	// request code to open bluetooth
	public static int REQUEST_ENABLE_BT = 1;
	// request code of connect device
	public static int REQUEST_GET_DEVICE = 2;

	@InjectView(R.id.progress_bar)
	ProgressWheel progress;
	@InjectView(R.id.result_content)
	LinearLayout mResultContent;
	@InjectView(R.id.img_connect)
	ImageView imgConnect;
	
	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;
	private String mDeviceAddress;
	private int mBackClickTimes = 0;
	private BluetoothLeService mBluetoothLeService;
	private boolean mConnected = false;
	private ResultInfo mResultInfo = null;
	private ResultException mResultException = null;
	
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			if(!result) {
				String connectFailedStr = getResources().getString(R.string.connect_failed);
				Toast.makeText(mContext, connectFailedStr, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);
		// init params
		mContext = MainActivity.this;
		// if the device doesn't support ble, close the app.
		if (!checkSupport()) {
			String remindStr = getResources().getString(
					R.string.remind_device_not_support);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			finish();
		}
		// if the ble is closed, request user to open.
		requestBluetooth();
		
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		getApplicationContext().bindService(gattServiceIntent, mServiceConnection,
				BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
		if(mConnected)
			this.mBluetoothLeService.disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
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
		if (progress.isSpinning()) {
			progress.stopSpinning();
			progress.setText("¿ªÊ¼¼ì²â");
		} else {
			progress.spin();
			progress.setText("Í£Ö¹¼ì²â");
		}
	}

	@OnClick(R.id.btn_detect_again)
	public void detectAgain(View v) {
		hideResult();
	}
	
	@OnClick(R.id.img_connect)
	public void showDeviceList(View v) {
		if(mConnected) {
			this.mBluetoothLeService.disconnect();
			String remindStr = getResources().getString(R.string.connect_broken);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
		} else {
			Intent intent = new Intent(mContext, DeviceListActivity.class);
			startActivityForResult(intent, REQUEST_GET_DEVICE);
		}
	}

	@OnClick(R.id.btn_history)
	public void showHistory(View v) {
		Intent intent = new Intent(mContext, BloodHistoryActivity.class);
		startActivity(intent);
	}

	// after detective finished, show the result view
	private void showResult() {
		Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f,
				800.0f, 0.0f);
		translateAnimation.setDuration(1500);
		mResultContent.startAnimation(translateAnimation);
		mResultContent.setVisibility(View.VISIBLE);
	}

	// after click the button, hide the result view
	private void hideResult() {
		Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f, 0.0f,
				800.0f);
		translateAnimation.setDuration(1500);
		mResultContent.startAnimation(translateAnimation);
		mResultContent.setVisibility(View.GONE);
	}

	@Override
	public void onBackPressed() {
		if (mBackClickTimes == 0) {
			String str = getResources().getString(R.string.ask_when_exit);
			Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();
			mBackClickTimes = 1;
			new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						mBackClickTimes = 0;
					}
				}
			}.start();
			return;
		} else {
			finish();
			System.exit(0);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT) {
			// if user don't open ble, close the app
			if (resultCode == RESULT_CANCELED) {
				String remindStr = getResources().getString(
						R.string.remind_ble_must_open);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
		if (requestCode == REQUEST_GET_DEVICE) {
			if (resultCode == RESULT_OK) {
				mDeviceAddress = data.getExtras().getString(
						DeviceListActivity.DEVICE_ADDRESS);
			} else {
				String str = getResources().getString(
						R.string.connect_device_first);
				Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				imgConnect.setImageResource(R.drawable.ic_connected);
				String remindStr = getResources().getString(R.string.connect_success);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
				System.out.println("connected");
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				imgConnect.setImageResource(R.drawable.ic_unconnect);
				String remindStr = getResources().getString(R.string.connect_broken);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
				System.out.println("disconnected");
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				System.out.println("discovery");
				displayGattServices(mBluetoothLeService
						.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				String extraData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
				displayData(extraData);
			}
		}
	};
	
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		for (BluetoothGattService gattService : gattServices) {
			for(BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
				String uuid = characteristic.getUuid().toString();
				if(uuid.equals(Uuids.RESULT_INFO)) {
					mBluetoothLeService.readCharacteristic(characteristic);
				}
			}
		}
	}
	
	private void displayData(String data) {
		System.out.println(data.trim().length());
		if(data.trim().length() == 38) {
			mResultInfo = new ResultInfo(data);
			System.out.println(mResultInfo.systolic + "   " + mResultInfo.diastolic + "   " + mResultInfo.heartRate);
		} else if(data.trim().length() == 29) {
			mResultException = new ResultException(data);
			System.out.println(mResultException.errorCode + "    " + mResultException.description);
		}
	}
	
	

}
