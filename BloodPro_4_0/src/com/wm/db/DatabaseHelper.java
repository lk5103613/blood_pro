package com.wm.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	
    private static final int DATABASE_VERSION = 1;//db version
    private static final String DATABASE_NAME = "BLOOD.db"; //db name
    public static final String TABLE_NAME_MODLE = "blood_data";//table name
    
    
	/***********************table columns**************************/
	public static String ID="id";
	public static String xinlv="xinlv";
	public static String shousuo="shousuo";
	public static String shuzhang="shuzhang";
	public static String date="date";
	
	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, DATABASE_NAME, factory, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuffer sBuffer = new StringBuffer();
		 sBuffer.append("CREATE TABLE "+TABLE_NAME_MODLE);
	     sBuffer.append("("+ID+" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
	     sBuffer.append(xinlv+" TEXT,");
	     sBuffer.append(shousuo+" TEXT,");
	     sBuffer.append(shuzhang+" TEXT,");
	     sBuffer.append(date+" TEXT);");
        
         db.execSQL(sBuffer.toString());//execute sql to create table
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		
	}

}
