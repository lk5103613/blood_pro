package com.wm.bloodpro_4_0;

import java.util.UUID;

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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import com.wm.task.InsertResultTask;
import com.wm.tools.DataConvertUtils;
import com.wm.tools.ProgressWheel;
import com.wm.tools.Uuids;
import com.wn.entity.ResultException;
import com.wn.entity.ResultInfo;

/**
 * 
 * 应用的主界面
 * @author Like
 * 
 */
public class MainActivity extends Activity {
	
	public static String TAG = "test";
	
	// request code to open bluetooth
	public static int REQUEST_ENABLE_BT = 1;
	// request code of connect device
	public static int REQUEST_GET_DEVICE = 2;

	@InjectView(R.id.progress_bar)
	ProgressWheel mProgress;
	@InjectView(R.id.result_content)
	LinearLayout mResultContent;
	@InjectView(R.id.img_connect)
	ImageView mImgConnect;
	@InjectView(R.id.txt_heart_rate_value)
	TextView mLblHeartRate;
	@InjectView(R.id.txt_systolic_value)
	TextView mLblSystolic;
	@InjectView(R.id.txt_diastolic_value)
	TextView mLblDiastolic;
	@InjectView(R.id.lbl_current_pressure)
	TextView mLblCurrentPressure;
	
	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;
	private String mDeviceAddress;
	private int mBackClickTimes = 0;
	private BluetoothLeService mBluetoothLeService;
	private boolean mConnected = false;
	private ResultInfo mResultInfo = null;
	private ResultException mResultException = null;
	private boolean mNeedNewData = true;
	private BluetoothGattCharacteristic mNotifyCharacteristic = null;
	private BluetoothGattCharacteristic mInforCharacteristic = null;
	private boolean mIsConnecting = false;
	private final String mPressureInitValue = "000";
	
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		registerReceiver(mBleStateReceiver, makeBleStateIntentFilter());
		if (mBluetoothLeService != null) {
			// 尝试连接BLE设备
			mBluetoothLeService.connect(mDeviceAddress);
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
	
	private static IntentFilter makeBleStateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
		return intentFilter;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);
		// 初始化参数
		mContext = MainActivity.this;
		// 如果设备不支持BLE，提示并关闭应用
		if (!checkSupport()) {
			String remindStr = getResources().getString(
					R.string.remind_device_not_support);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			finish();
		}
		// 如果设备蓝牙是关闭状态，请求打开
		requestBluetooth();
		// 绑定蓝牙服务
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection,
				BIND_AUTO_CREATE);
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// 暂时离开主界面时，断开与蓝牙的连接
		unregisterReceiver(mGattUpdateReceiver);
		unregisterReceiver(mBleStateReceiver);
		if(mConnected)
			this.mBluetoothLeService.disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mBluetoothLeService != null && mServiceConnection != null)
			unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	// 检查设备是否支持BLE
	private boolean checkSupport() {
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}
		return true;
	}

	// 请求打开蓝牙
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
		// 点击开始检测后，隐藏结果界面
		if(this.mResultContent.getVisibility() == View.VISIBLE) {
			hideResult();
		}
		if(!mConnected) {
			// 如果没有设备连接，提示请选择设备连接
			String msg = getResources().getString(R.string.connect_device_first);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			return;
		}
		if (mProgress.isSpinning()) {
			// 当点击时正在检测，点击后停止检测
			scanFinish();
		} else {
			// 点击后开始检测
			mProgress.spin();
			mProgress.setText("停止检测");
			if(!beginDetect()) {
				// 如果扫描异常，停止扫描
				scanFinish();
			}
		}
	}
	
	// 开始检测
	private boolean beginDetect() {
		if(mInforCharacteristic == null) {
			// 包含数据的characteristic为空，检测失败
			return false;
		}
		mBluetoothLeService.setCharacteristicNotification(
				mInforCharacteristic, true);
		mNotifyCharacteristic = mInforCharacteristic;
		return true;
	}
	
	// 扫描结束或者停止扫描
	private void scanFinish() {
		mProgress.stopSpinning();
		mProgress.setText("开始检测");
		if(mNotifyCharacteristic != null)
			mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
		mNotifyCharacteristic = null;
	}

	@OnClick(R.id.btn_detect_again)
	public void detectAgain(View v) {
		hideResult();
		this.mLblCurrentPressure.setText(mPressureInitValue);
	}
	
	@OnClick(R.id.img_connect)
	public void showDeviceList(View v) {
		if(this.mResultContent.getVisibility() == View.VISIBLE) {
			hideResult();
		}
		if(mIsConnecting) {
			String msg = getResources().getString(R.string.connecting_now);
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
			return;
		}
		if(mConnected) {
			scanFinish();
			this.mLblCurrentPressure.setText(mPressureInitValue);
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
		scanFinish();
		Intent intent = new Intent(mContext, BloodHistoryActivity.class);
		startActivity(intent);
	}

	// 检测结束后，展示结果
	private void showResult(String systolic, String diastolic, String heartRate ) {
		Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f,
				800.0f, 0.0f);
		translateAnimation.setDuration(1500);
		mResultContent.startAnimation(translateAnimation);
		this.mLblHeartRate.setText(heartRate);
		this.mLblSystolic.setText(systolic);
		this.mLblDiastolic.setText(diastolic);
		mResultContent.setVisibility(View.VISIBLE);
	}

	// 隐藏展示结果
	private void hideResult() {
		Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f, 0.0f,
				800.0f);
		translateAnimation.setDuration(1500);
		mResultContent.startAnimation(translateAnimation);
		mResultContent.setVisibility(View.GONE);
	}

	@Override
	public void onBackPressed() {
		// 连续点击两次返回键，退出程序
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
			// 如果用户不同意打开蓝牙，关闭应用
			if (resultCode == RESULT_CANCELED) {
				String remindStr = getResources().getString(
						R.string.remind_ble_must_open);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
		if (requestCode == REQUEST_GET_DEVICE) {
			if (resultCode == RESULT_OK) {
				// 用户选择设备后，获取设备的address
				mDeviceAddress = data.getExtras().getString(
						DeviceListActivity.DEVICE_ADDRESS);
				mIsConnecting = true;
			} else {
				// 用户没有选择设备，提示用户选择设备连接
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
				// 连接成功后，提示用户连接成功
				mConnected = true;
				mIsConnecting = false;
				mImgConnect.setImageResource(R.drawable.ic_connected);
				String remindStr = getResources().getString(R.string.connect_success);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				// 连接断开，提示用户连接断开
				mConnected = false;
				mIsConnecting = false;
				mImgConnect.setImageResource(R.drawable.ic_unconnect);
				scanFinish();
				String remindStr = getResources().getString(R.string.connect_broken);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				BluetoothGattService service = mBluetoothLeService.getServiceByUuid(Uuids.RESULT_INFO_SERVICE);
				if(service == null) {
					return;
				}
				mInforCharacteristic = service.getCharacteristic(UUID.fromString(Uuids.RESULT_INFO));
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				// 从BLE设备获得数据并展示
				String extraData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
				displayData(extraData);
			}
		}
	};
	
	private final BroadcastReceiver mBleStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			requestBluetooth();
		}
		
	};
	
	// 计算Pressure值
	private String getPressureValue(String data) {
		String[] items = data.split(" ");
		int pressureH = Integer.valueOf(DataConvertUtils.hexToDecimal(items[2]));
		int pressureL = Integer.valueOf(DataConvertUtils.hexToDecimal(items[1]));
		return pressureH * 256 + pressureL + "";
	}
	
	// 将从BLE设备获得的设备展示到应用中
	private void displayData(String data) {
		if(data.trim().length() == 38) {
			// 成功获得血压心率等数据
			mNeedNewData = false;
			mResultInfo = new ResultInfo(data);
			showResult(mResultInfo.systolic, mResultInfo.diastolic, mResultInfo.heartRate);
			new InsertResultTask(MainActivity.this, mResultInfo).execute();//insert to db
		} else if(data.trim().length() == 29) {
			// 获得异常信息
			mNeedNewData = false;
			mResultException = new ResultException(data);
			Toast.makeText(mContext, mResultException.description, Toast.LENGTH_LONG).show();
		} else if(data.trim().length() == 8) {
			// 获得Pressure值
			mNeedNewData = true;
			String currentPressure = getPressureValue(data);
			this.mLblCurrentPressure.setText(currentPressure);
		}
		if(!mNeedNewData) {
			scanFinish();
		}
	}
	
}
