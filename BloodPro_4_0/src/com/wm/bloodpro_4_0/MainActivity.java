package com.wm.bloodpro_4_0;

import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import com.wm.db.BloodInfo;
import com.wm.db.DBService;
import com.wm.task.InsertResultTask;
import com.wm.tools.DataConvertUtils;
import com.wm.tools.DeviceScanner;
import com.wm.tools.DeviceScanner.ScanCallback;
import com.wm.tools.ProgressWheel;
import com.wm.tools.Uuids;
import com.wn.entity.ResultException;
import com.wn.entity.ResultInfo;

/**
 * 
 * 应用的主界面
 * 
 * @author Like
 * 
 */
public class MainActivity extends Activity {

	// request code to open bluetooth
	public static int REQUEST_ENABLE_BT = 1;

	// 连接设备的最长时间
	private static final int CONNECT_TIME = 10000;
	// 最多重连次数
	private static final int MAX_RETRY_TIME = 5;

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
	
	private final static int STATE_CONNECTED = 0;
	private final static int STATE_DISCONNECTED = 1;
	private final static int STATE_CONNECTING = 2;

	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;
	private String mDeviceAddress;
	private int mBackClickTimes = 0;
	private BluetoothLeService mBluetoothLeService;
	private ResultInfo mResultInfo = null;
	private ResultException mResultException = null;
	private boolean mNeedNewData = true;
	private BluetoothGattCharacteristic mNotifyCharacteristic = null;
	private BluetoothGattCharacteristic mInforCharacteristic = null;
	private final String mPressureInitValue = "000";
	private Handler mHandler;
	private DeviceScanner mScanner;
	private int mRetryTime = 0;
	private AnimationDrawable mFlashing = null;
	private int mCurrentState = STATE_DISCONNECTED;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);
		// 初始化参数
		mContext = MainActivity.this;
		mHandler = new Handler();
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		mScanner = DeviceScanner.getInstance(mBluetoothAdapter, mCallback);
		mFlashing = (AnimationDrawable) getResources().getDrawable(R.drawable.connect_flashing);
		// 如果设备不支持BLE，提示并关闭应用
		if (!checkSupport()) {
			String remindStr = getResources().getString(
					R.string.remind_device_not_support);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			finish();
		}
		// 开始扫描
		if(isBluetoothOpen())
			beginScan();
		// 绑定蓝牙服务
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	// 开始扫描
	private void beginScan() {
		// 开始扫描设备
		mDeviceAddress = null;
		mScanner.scanLeDevice(true);
		mCurrentState = STATE_CONNECTING;
		// 开始正在连接的闪烁动画，并给出提示
		mImgConnect.setBackground(mFlashing);
		mFlashing.start();
		String msg = getResources().getString(R.string.scan_and_connect);
		Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 如果设备蓝牙是关闭状态，请求打开
		requestBluetooth();
		// 注册广播接受者
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		registerReceiver(mBleStateReceiver, makeBleStateIntentFilter());
	}

	@Override
	protected void onPause() {
		super.onPause();
		// 暂时离开主界面时，断开与蓝牙的连接
		unregisterReceiver(mGattUpdateReceiver);
		unregisterReceiver(mBleStateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBluetoothLeService != null && mServiceConnection != null) {
			if (isConnected())
				this.mBluetoothLeService.disconnect();
			unbindService(mServiceConnection);
		}
		mBluetoothLeService = null;
	}
	
	// 使界面控件回复初始状态
	private void reset() {
		scanFinish();
		hideResult();
		mRetryTime = 0;
		mDeviceAddress = null;
		mImgConnect.setBackgroundResource(R.drawable.ic_unconnect);
		if(mFlashing != null) 
			mFlashing.stop();
		mLblCurrentPressure.setText(mPressureInitValue);
	}

	// 检查设备是否支持BLE
	private boolean checkSupport() {
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}
		return true;
	}
	
	/**
	 * 判断蓝牙是否开启
	 * @return 开启返回true，否则返回false
	 */
	private boolean isBluetoothOpen() {
		return !(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled());
	}

	// 请求打开蓝牙
	private void requestBluetooth() {
		if (!isBluetoothOpen()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@OnClick(R.id.progress_bar)
	public void clickProgress(View v) {
		// 点击开始检测后，隐藏结果界面
		if (this.mResultContent.getVisibility() == View.VISIBLE) {
			hideResult();
		}
		// 如果正在连接，显示正在连接，请耐心等待
		if (mCurrentState == STATE_CONNECTING) {
			String msg = getResources().getString(R.string.connecting_now);
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
			return;
		}
		if (mCurrentState != STATE_CONNECTED) {
			// 如果没有设备连接，提示请选择设备连接
			String msg = getResources()
					.getString(R.string.connect_device_first);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			return;
		}
		if (mProgress.isSpinning()) {
			// 当点击时正在检测，点击后停止检测
			scanFinish();
		} else {
			// 点击后开始检测
			if (!beginDetect()) {
				// 如果扫描异常，停止扫描
				scanFinish();
			}
		}
	}
	
	@OnClick(R.id.img_connect)
	public void reconnect(View v) {
		// 如果是连接失败状态，点击后重新扫描并连接
		if(mCurrentState != STATE_DISCONNECTED)
			return;
		beginScan();
	}

	// 开始检测
	private boolean beginDetect() {
		mProgress.spin();
		mProgress.setText("停止检测");
		if (mInforCharacteristic == null) {
			// 包含数据的characteristic为空，检测失败
			return false;
		}
		mBluetoothLeService.setCharacteristicNotification(mInforCharacteristic,
				true);
		mNotifyCharacteristic = mInforCharacteristic;
		return true;
	}

	// 扫描结束或者停止扫描
	private void scanFinish() {
		mProgress.stopSpinning();
		mProgress.setText("开始检测");
		if (mNotifyCharacteristic != null)
			mBluetoothLeService.setCharacteristicNotification(
					mNotifyCharacteristic, false);
		mNotifyCharacteristic = null;
	}

	@OnClick({R.id.btn_history, R.id.result_btn_history})
	public void showHistory(View v) {
		hideResult();
		this.mLblCurrentPressure.setText(mPressureInitValue);
		List<BloodInfo> infos = new DBService(mContext).getAllModle();
		if (infos == null || infos.size() == 0) {
			String msg = getResources().getString(R.string.no_history_data);
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = new Intent(mContext, BloodHistoryActivity.class);
		startActivity(intent);
	}

	// 检测结束后，展示结果
	private void showResult(String systolic, String diastolic, String heartRate) {
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
				mDeviceAddress = null;
				finish();
				System.exit(0);
			} else {
				beginScan();
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
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private ScanCallback mCallback = new ScanCallback() {
		@Override
		public void onScanSuccess(BluetoothDevice device) {
			System.out.println("on scan success");
			if(mDeviceAddress != null) {
				System.out.println("mdevice address is not null");
				return;
			}
			mDeviceAddress = device.getAddress();
			// 扫描设备成功后，开始连接
			if (mBluetoothLeService != null) {
				System.out.println("m bluetoothleservice is not null");
				new Thread(new Runnable() {
					@Override
					public void run() {
						mBluetoothLeService.connect(mDeviceAddress);
						
					}
				}).start();
				// 超过时间无响应后认定连接超时
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (isConnecting()) {
							handleConnectFail();
						}
					}
				}, CONNECT_TIME);
			}
		}

		@Override
		public void onScanFailed() {
			mCurrentState = STATE_DISCONNECTED;
			reset();
			String msg = getResources().getString(R.string.scan_failed);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onScanStateChange(int scanState) {

		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				// 连接成功后，提示用户连接成功
				mRetryTime = 0;
				if(mCurrentState != STATE_CONNECTED) {
					mImgConnect.setBackgroundResource(R.drawable.ic_connected);
					mFlashing.stop();
					String remindStr = getResources().getString(
							R.string.connect_success);
					Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
				}
				mCurrentState = STATE_CONNECTED;
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				// 连接断开，重试
				handleConnectFail();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				BluetoothGattService service = mBluetoothLeService
						.getServiceByUuid(Uuids.RESULT_INFO_SERVICE);
				if (service == null) {
					return;
				}
				mInforCharacteristic = service.getCharacteristic(UUID
						.fromString(Uuids.RESULT_INFO));
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				// 从BLE设备获得数据并展示
				String extraData = intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA);
				handleData(extraData);
			}
		}
	};

	// 处理连接失败的情况，如果超过重试次数，判定连接失败，否则重新连接
	private void handleConnectFail() {
		if (mRetryTime >= MAX_RETRY_TIME)
			handleRetryOvertime();
		else
			reconnect();
	}

	// 当连接重试次数到达后所做处理
	private void handleRetryOvertime() {
		reset();
		mCurrentState = STATE_DISCONNECTED;
		String msg = getResources().getString(R.string.connect_failed);
		Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
	}

	// 重新连接
	private void reconnect() {
		mRetryTime += 1;
		mBluetoothLeService.disconnect();
		mDeviceAddress = null;
		mScanner.scanLeDevice(true);
	}

	// 监听手机蓝牙状态，如果中途关闭蓝牙，再次请求
	private final BroadcastReceiver mBleStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			requestBluetooth();
		}
	};

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
		intentFilter
				.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
		return intentFilter;
	}

	// 计算Pressure值
	private String getPressureValue(String data) {
		String[] items = data.split(" ");
		int pressureH = Integer
				.valueOf(DataConvertUtils.hexToDecimal(items[2]));
		int pressureL = Integer
				.valueOf(DataConvertUtils.hexToDecimal(items[1]));
		return pressureH * 256 + pressureL + "";
	}

	// 将从BLE设备获得的设备展示到应用中，如果数据正常，存入数据库
	private void handleData(String data) {
		if (data.trim().length() == 38) {
			// 成功获得血压心率等数据
			mNeedNewData = false;
			mResultInfo = new ResultInfo(data);
			showResult(mResultInfo.systolic, mResultInfo.diastolic,
					mResultInfo.heartRate);
			// 将数据存入数据库
			new InsertResultTask(MainActivity.this, mResultInfo).execute();
		} else if (data.trim().length() == 29) {
			// 获得异常信息
			mNeedNewData = false;
			mResultException = new ResultException(data);
			Toast.makeText(mContext, mResultException.description,
					Toast.LENGTH_LONG).show();
		} else if (data.trim().length() == 8) {
			// 获得Pressure值
			mNeedNewData = true;
			String currentPressure = getPressureValue(data);
			this.mLblCurrentPressure.setText(currentPressure);
		}
		if (!mNeedNewData) {
			scanFinish();
		}
	}

	// 如果正在连接返回true，否则返回false
	private boolean isConnecting() {
		if (this.mBluetoothLeService == null) {
			return false;
		}
		return this.mBluetoothLeService.getConnectState() == BluetoothLeService.STATE_CONNECTING;
	}

	// 如果当前状态为已连接，返回true，否则返回false
	private boolean isConnected() {
		if (this.mBluetoothLeService == null) {
			return false;
		}
		return this.mBluetoothLeService.getConnectState() == BluetoothLeService.STATE_CONNECTED;
	}

}
