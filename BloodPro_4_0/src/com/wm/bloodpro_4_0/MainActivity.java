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
 * Ӧ�õ�������
 * 
 * @author Like
 * 
 */
public class MainActivity extends Activity {

	// request code to open bluetooth
	public static int REQUEST_ENABLE_BT = 1;

	// �����豸���ʱ��
	private static final int CONNECT_TIME = 10000;
	// �����������
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
		// ��ʼ������
		mContext = MainActivity.this;
		mHandler = new Handler();
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		mScanner = DeviceScanner.getInstance(mBluetoothAdapter, mCallback);
		mFlashing = (AnimationDrawable) getResources().getDrawable(R.drawable.connect_flashing);
		// ����豸��֧��BLE����ʾ���ر�Ӧ��
		if (!checkSupport()) {
			String remindStr = getResources().getString(
					R.string.remind_device_not_support);
			Toast.makeText(mContext, remindStr, Toast.LENGTH_LONG).show();
			finish();
		}
		// ��ʼɨ��
		beginScan();
		// ����������
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	// ��ʼɨ��
	private void beginScan() {
		// ��ʼɨ���豸
		mScanner.scanLeDevice(true);
		mCurrentState = STATE_CONNECTING;
		// ��ʼ�������ӵ���˸��������������ʾ
		mImgConnect.setBackground(mFlashing);
		mFlashing.start();
		String msg = getResources().getString(R.string.scan_and_connect);
		Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// ����豸�����ǹر�״̬�������
		requestBluetooth();
		// ע��㲥������
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		registerReceiver(mBleStateReceiver, makeBleStateIntentFilter());
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// ��ʱ�뿪������ʱ���Ͽ�������������
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
	
	// ʹ����ؼ��ظ���ʼ״̬
	private void reset() {
		scanFinish();
		mRetryTime = 0;
		mDeviceAddress = null;
		mImgConnect.setBackgroundResource(R.drawable.ic_unconnect);
		if(mFlashing != null) 
			mFlashing.stop();
		mLblCurrentPressure.setText(mPressureInitValue);
	}

	// ����豸�Ƿ�֧��BLE
	private boolean checkSupport() {
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}
		return true;
	}

	// ���������
	private void requestBluetooth() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@OnClick(R.id.progress_bar)
	public void clickProgress(View v) {
		// �����ʼ�������ؽ������
		if (this.mResultContent.getVisibility() == View.VISIBLE) {
			hideResult();
		}
		// ����������ӣ���ʾ�������ӣ������ĵȴ�
		if (mCurrentState == STATE_CONNECTING) {
			String msg = getResources().getString(R.string.connecting_now);
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
			return;
		}
		if (mCurrentState != STATE_CONNECTED) {
			// ���û���豸���ӣ���ʾ��ѡ���豸����
			String msg = getResources()
					.getString(R.string.connect_device_first);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			return;
		}
		if (mProgress.isSpinning()) {
			// �����ʱ���ڼ�⣬�����ֹͣ���
			scanFinish();
		} else {
			// �����ʼ���
			if (!beginDetect()) {
				// ���ɨ���쳣��ֹͣɨ��
				scanFinish();
			}
		}
	}
	
	@OnClick(R.id.img_connect)
	public void reconnect(View v) {
		// ���������ʧ��״̬�����������ɨ�貢����
		if(mCurrentState != STATE_DISCONNECTED)
			return;
		beginScan();
	}

	// ��ʼ���
	private boolean beginDetect() {
		mProgress.spin();
		mProgress.setText("ֹͣ���");
		if (mInforCharacteristic == null) {
			// �������ݵ�characteristicΪ�գ����ʧ��
			return false;
		}
		mBluetoothLeService.setCharacteristicNotification(mInforCharacteristic,
				true);
		mNotifyCharacteristic = mInforCharacteristic;
		return true;
	}

	// ɨ���������ֹͣɨ��
	private void scanFinish() {
		mProgress.stopSpinning();
		mProgress.setText("��ʼ���");
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

	// ��������չʾ���
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

	// ����չʾ���
	private void hideResult() {
		mResultContent.setVisibility(View.GONE);
	}

	@Override
	public void onBackPressed() {
		// ����������η��ؼ����˳�����
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
			// ����û���ͬ����������ر�Ӧ��
			if (resultCode == RESULT_CANCELED) {
				String remindStr = getResources().getString(
						R.string.remind_ble_must_open);
				Toast.makeText(mContext, remindStr, Toast.LENGTH_SHORT).show();
				finish();
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
			mDeviceAddress = device.getAddress();
			// ɨ���豸�ɹ��󣬿�ʼ����
			if (mBluetoothLeService != null) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						mBluetoothLeService.connect(mDeviceAddress);
					}
				}).start();
				// ����ʱ������Ӧ���϶����ӳ�ʱ
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
				// ���ӳɹ�����ʾ�û����ӳɹ�
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
				// ���ӶϿ�������
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
				// ��BLE�豸������ݲ�չʾ
				String extraData = intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA);
				handleData(extraData);
			}
		}
	};

	// ��������ʧ�ܵ����������������Դ������ж�����ʧ�ܣ�������������
	private void handleConnectFail() {
		if (mRetryTime >= MAX_RETRY_TIME)
			handleRetryOvertime();
		else
			reconnect();
	}

	// ���������Դ����������������
	private void handleRetryOvertime() {
		reset();
		mCurrentState = STATE_DISCONNECTED;
		String msg = getResources().getString(R.string.connect_failed);
		Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
	}

	// ��������
	private void reconnect() {
		mRetryTime += 1;
		mBluetoothLeService.disconnect();
		mScanner.scanLeDevice(true);
	}

	// �����ֻ�����״̬�������;�ر��������ٴ�����
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

	// ����Pressureֵ
	private String getPressureValue(String data) {
		String[] items = data.split(" ");
		int pressureH = Integer
				.valueOf(DataConvertUtils.hexToDecimal(items[2]));
		int pressureL = Integer
				.valueOf(DataConvertUtils.hexToDecimal(items[1]));
		return pressureH * 256 + pressureL + "";
	}

	// ����BLE�豸��õ��豸չʾ��Ӧ���У���������������������ݿ�
	private void handleData(String data) {
		if (data.trim().length() == 38) {
			// �ɹ����Ѫѹ���ʵ�����
			mNeedNewData = false;
			mResultInfo = new ResultInfo(data);
			showResult(mResultInfo.systolic, mResultInfo.diastolic,
					mResultInfo.heartRate);
			// �����ݴ������ݿ�
			new InsertResultTask(MainActivity.this, mResultInfo).execute();
		} else if (data.trim().length() == 29) {
			// ����쳣��Ϣ
			mNeedNewData = false;
			mResultException = new ResultException(data);
			Toast.makeText(mContext, mResultException.description,
					Toast.LENGTH_LONG).show();
		} else if (data.trim().length() == 8) {
			// ���Pressureֵ
			mNeedNewData = true;
			String currentPressure = getPressureValue(data);
			this.mLblCurrentPressure.setText(currentPressure);
		}
		if (!mNeedNewData) {
			scanFinish();
		}
	}

	// ����������ӷ���true�����򷵻�false
	private boolean isConnecting() {
		if (this.mBluetoothLeService == null) {
			return false;
		}
		return this.mBluetoothLeService.getConnectState() == BluetoothLeService.STATE_CONNECTING;
	}

	// �����ǰ״̬Ϊ�����ӣ�����true�����򷵻�false
	private boolean isConnected() {
		if (this.mBluetoothLeService == null) {
			return false;
		}
		return this.mBluetoothLeService.getConnectState() == BluetoothLeService.STATE_CONNECTED;
	}

}
