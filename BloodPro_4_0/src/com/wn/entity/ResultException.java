package com.wn.entity;

import java.util.Locale;

/**
 * 
 * 检测结果异常实体类
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
				description = "EEPROM异常";
				break;
			case "01":
				errorCode = "E-1";
				description = "人体心跳信号太小或压力突降";
				break;
			case "02":
				errorCode = "E-2";
				description = "有杂讯干扰";
				break;
			case "03":
				errorCode = "E-3";
				description = "测量结果异常，需要重测";
				break;
			case "04":
				errorCode = "E-4";
				description = "测得的结果异常";
				break;
			case "0f":
				errorCode = "E-P";
				description = "充气时间过长";
				break;
			case "0b":
				errorCode = "E-B";
				description = "电源低电压";
				break;
			case "0d":
				errorCode = "E-D";
				description = "过压";
				break;
			default:
				break;
		}
	}
	
}
