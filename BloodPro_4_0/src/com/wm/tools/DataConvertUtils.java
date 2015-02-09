package com.wm.tools;

public class DataConvertUtils {
	
	public static String hexToDecimal(String hex) {
		return Integer.valueOf(hex, 16).toString();
	}
	
	public static String decimalToHex(String decimal) {
		int decNumber = Integer.valueOf(decimal);
		return Integer.toHexString(decNumber);
	}

}
