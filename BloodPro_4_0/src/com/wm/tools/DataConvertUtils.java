package com.wm.tools;

/**
 * 
 * 对数据进行进制的转换
 * @author Like
 *
 */
public class DataConvertUtils {
	
	/**
	 * 16进制转10进制
	 * @param hex 十六进制字符串
	 * @return 转换好的十进制字符串
	 */
	public static String hexToDecimal(String hex) {
		return Integer.valueOf(hex, 16).toString();
	}
	
	/**
	 * 10进制转16进制
	 * @param decimal 十进制字符串
	 * @return 转换好的十六进制字符串
	 */
	public static String decimalToHex(String decimal) {
		int decNumber = Integer.valueOf(decimal);
		return Integer.toHexString(decNumber);
	}

}
