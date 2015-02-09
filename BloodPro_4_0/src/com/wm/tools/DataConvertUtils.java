package com.wm.tools;

public class DataConvertUtils {
	
	public static String hexToDecimal(String hex) {
		int hexNumber = Integer.valueOf(hex);
		return Integer.valueOf("FFFF", hexNumber).toString();
	}
	
	public static String decimalToHex(String decimal) {
		int decNumber = Integer.valueOf(decimal);
		return Integer.toHexString(decNumber);
	}

}
