package com.wn.entity;

import java.util.Locale;
import java.util.Map;

import com.wm.tools.ASCIIData;
import com.wm.tools.DataConvertUtils;

/**
 * 
 * 检测结果实体类
 * @author Like
 *
 */
public class ResultInfo {
	
	public static int HEART_RATE_STATE_NORMAL = 0;
	public static int HEART_RATE_STATE_NOT_NORMAL = 1;
	
	public String systolic;
	public String diastolic;
	public String heartRate;
	public int heartRateState = HEART_RATE_STATE_NORMAL;
	
	public ResultInfo(String result) {
		Map<String, String> AsciiTable = ASCIIData.getASCIITable();
		Locale defloc = Locale.getDefault();
		result = result.toUpperCase(defloc);
		String[] items = result.split(" ");
		systolic = DataConvertUtils.hexToDecimal(AsciiTable.get(items[4]) + AsciiTable.get(items[3]));
		diastolic = DataConvertUtils.hexToDecimal(AsciiTable.get(items[6]) + AsciiTable.get(items[5]));
		heartRate = DataConvertUtils.hexToDecimal(AsciiTable.get(items[8]) + AsciiTable.get(items[7]));
		heartRateState = HEART_RATE_STATE_NORMAL;
		if(items[9].toLowerCase(defloc).equals("55")) {
			heartRateState = HEART_RATE_STATE_NORMAL;
		} else if(items[9].toLowerCase(defloc).equals("aa")) {
			heartRateState = HEART_RATE_STATE_NOT_NORMAL;
		}
	}
	
}
