package com.wm.tools;

import android.content.SharedPreferences;

public class SharePreferencesUtils {
	
	public static void write(SharedPreferences sp, String key, String value) {
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(key, value);
		editor.commit();
	}

}
