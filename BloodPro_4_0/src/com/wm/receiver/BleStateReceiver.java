package com.wm.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BleStateReceiver extends BroadcastReceiver{
	private BluetoothAdapter mBluetoothAdapter;

	@Override
	public void onReceive(Context context, Intent intent) {
		final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			System.out.println("À¶ÑÀ¹Ø±Õ");
			
		}
	}
}
