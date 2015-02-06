package com.wm.bloodpro_4_0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
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
	@InjectView(R.id.result_content)
	LinearLayout mResultContent;
	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;
	private int mBackClickTimes = 0;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);
		// init params
		mContext = MainActivity.this;
		// if the device doesn't support ble, close the app.
		if(!checkSupport()) {
			String remindStr = getResources().getString(R.string.remind_device_not_support);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			finish();
		}
		// if the ble is closed, request user to open.
		requestBluetooth();
		showResult();
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
	
	@OnClick(R.id.btn_detect_again)
	public void detectAgain(View v) {
		hideResult();
	}
	
	// after dectective finished, show the result view
	private void showResult() {
		Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f,
				800.0f, 0.0f);
		translateAnimation.setDuration(1500);
		mResultContent.startAnimation(translateAnimation);
		mResultContent.setVisibility(View.VISIBLE);
	}
	
	// after click the button, hide the result view
	private void hideResult() {
		Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f,
				0.0f, 800.0f);
		translateAnimation.setDuration(1500);
		mResultContent.startAnimation(translateAnimation);
		mResultContent.setVisibility(View.GONE);
	}
	
	@OnClick(R.id.img_connect)
	public void showDeviceList(View v) {
		Intent intent = new Intent(mContext, DeviceListActivity.class);
		startActivity(intent);
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
	
	@Override
	public void onBackPressed() {
		if(mBackClickTimes == 0) {
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
	
}
