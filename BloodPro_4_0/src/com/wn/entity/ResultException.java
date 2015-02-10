package com.wn.entity;

import java.util.Locale;

/**
 * 
 * ������쳣ʵ����
 * @author Like
 *
 */
public class ResultException {
	
	public String errorCode;
	public String description;
	
	public ResultException(String result) {
		String[] items = result.split(" ");
		switch (items[3].toLowerCase(Locale.getDefault())) {
			case "0e":
				errorCode = "E-E";
				description = "EEPROM�쳣";
				break;
			case "01":
				errorCode = "E-1";
				description = "���������ź�̫С��ѹ��ͻ��";
				break;
			case "02":
				errorCode = "E-2";
				description = "����Ѷ����";
				break;
			case "03":
				errorCode = "E-3";
				description = "��������쳣����Ҫ�ز�";
				break;
			case "04":
				errorCode = "E-4";
				description = "��õĽ���쳣";
				break;
			case "0f":
				errorCode = "E-P";
				description = "����ʱ�����";
				break;
			case "0b":
				errorCode = "E-B";
				description = "��Դ�͵�ѹ";
				break;
			case "0d":
				errorCode = "E-D";
				description = "��ѹ";
				break;
			default:
				break;
		}
	}
	
}
