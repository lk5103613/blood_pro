package com.wm.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * °üº¬¹þÏ£¶ÔÕÕ±í
 * @author Like
 *
 */
public final class ASCIIData {
	
	private static final Map<String, String> ASCII_TABLE = new HashMap<>();
	
	public static Map<String, String> getASCIITable() {
		if(ASCII_TABLE.keySet().size() == 0) {
			ASCII_TABLE.put("30", "0");
			ASCII_TABLE.put("31", "1");
			ASCII_TABLE.put("32", "2");
			ASCII_TABLE.put("33", "3");
			ASCII_TABLE.put("34", "4");
			ASCII_TABLE.put("35", "5");
			ASCII_TABLE.put("36", "6");
			ASCII_TABLE.put("37", "7");
			ASCII_TABLE.put("38", "8");
			ASCII_TABLE.put("39", "9");
	    	ASCII_TABLE.put("3A",":");
	    	ASCII_TABLE.put("3B",";");
	    	ASCII_TABLE.put("3C","<");
	    	ASCII_TABLE.put("3D","=");
	    	ASCII_TABLE.put("3E",">");
	    	ASCII_TABLE.put("3F","?");
	    	ASCII_TABLE.put("40","@");
	    	ASCII_TABLE.put("41","A");
	    	ASCII_TABLE.put("42","B");
	    	ASCII_TABLE.put("43","C");
	    	ASCII_TABLE.put("44","D");
	    	ASCII_TABLE.put("45","E");
	    	ASCII_TABLE.put("46","F");
	    	ASCII_TABLE.put("47","G");
	    	ASCII_TABLE.put("48","H");
	    	ASCII_TABLE.put("49","I");
	    	ASCII_TABLE.put("4A","J");
	    	ASCII_TABLE.put("4B","K");
	    	ASCII_TABLE.put("4C","L");
	    	ASCII_TABLE.put("4D","M");
	    	ASCII_TABLE.put("4E","N");
	    	ASCII_TABLE.put("4F","O");
	    	ASCII_TABLE.put("50","P");
	    	ASCII_TABLE.put("51","Q");
	    	ASCII_TABLE.put("52","R");
	    	ASCII_TABLE.put("53","S");
	    	ASCII_TABLE.put("54","T");
	    	ASCII_TABLE.put("55","U");
	    	ASCII_TABLE.put("56","V");
	    	ASCII_TABLE.put("57","W");
	    	ASCII_TABLE.put("58","X");
	    	ASCII_TABLE.put("59","Y");
	    	ASCII_TABLE.put("5A","Z");
	    	ASCII_TABLE.put("5B","[");
	    	ASCII_TABLE.put("5C","'\'");
	    	ASCII_TABLE.put("5D","]");
	    	ASCII_TABLE.put("5E","^");
	    	ASCII_TABLE.put("5F","_");
	    	ASCII_TABLE.put("60","'");
	    	ASCII_TABLE.put("61","a");
	    	ASCII_TABLE.put("62","b");
	    	ASCII_TABLE.put("63","c");
	    	ASCII_TABLE.put("64","d");
	    	ASCII_TABLE.put("65","e");
	    	ASCII_TABLE.put("66","f");
	    	ASCII_TABLE.put("67","g");
	    	ASCII_TABLE.put("68","h");
	    	ASCII_TABLE.put("69","i");
	    	ASCII_TABLE.put("6A","j");
	    	ASCII_TABLE.put("6B","k");
	    	ASCII_TABLE.put("6C","l");
	    	ASCII_TABLE.put("6D","m");
	    	ASCII_TABLE.put("6E","n");
	    	ASCII_TABLE.put("6F","o");
		}
		return ASCII_TABLE;
	}

}
