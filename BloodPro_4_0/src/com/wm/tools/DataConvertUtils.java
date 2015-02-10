package com.wm.tools;

/**
 * 
 * �����ݽ��н��Ƶ�ת��
 * @author Like
 *
 */
public class DataConvertUtils {
	
	/**
	 * 16����ת10����
	 * @param hex ʮ�������ַ���
	 * @return ת���õ�ʮ�����ַ���
	 */
	public static String hexToDecimal(String hex) {
		return Integer.valueOf(hex, 16).toString();
	}
	
	/**
	 * 10����ת16����
	 * @param decimal ʮ�����ַ���
	 * @return ת���õ�ʮ�������ַ���
	 */
	public static String decimalToHex(String decimal) {
		int decNumber = Integer.valueOf(decimal);
		return Integer.toHexString(decNumber);
	}

}
