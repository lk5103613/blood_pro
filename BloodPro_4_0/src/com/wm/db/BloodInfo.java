package com.wm.db;

public class BloodInfo {

	private int ID;
	private String heartRate;
	private String systolic;
	private String diastolic;
	private String date;
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	public String getHeartRate() {
		return heartRate;
	}
	public void setHeartRate(String xinlv) {
		this.heartRate = xinlv;
	}
	public String getSystolic() {
		return systolic;
	}
	public void setSystolic(String shousuo) {
		this.systolic = shousuo;
	}
	public String getDiastolic() {
		return diastolic;
	}
	public void setDiastolic(String shuzhang) {
		this.diastolic = shuzhang;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	
	
	
}
