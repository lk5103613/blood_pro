package com.wm.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBService {
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase db;

	public DBService(Context context) {
		databaseHelper = new DatabaseHelper(context, "", null, 1);
	}

	/**
	 * save data
	 * 
	 * @return
	 */
	public synchronized long insertModleData(BloodInfo materialInfo) {
		db = databaseHelper.getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseHelper.date,
				materialInfo.getDate());
		contentValues.put(DatabaseHelper.xinlv,
				materialInfo.getXinlv());
		contentValues.put(DatabaseHelper.shousuo,
				materialInfo.getShousuo());
		contentValues.put(DatabaseHelper.shuzhang,
				materialInfo.getShuzhang());
		 long l=db.insert(DatabaseHelper.TABLE_NAME_MODLE, null, contentValues);
		 db.close();
		 return l;
	}
	
	/**
	 * query data
	 * @return
	 */
	public long selectModleData()
	{
		db = databaseHelper.getReadableDatabase();
		Cursor cursor =
		db.query(DatabaseHelper.TABLE_NAME_MODLE, null, null, null, null, null, null);
		cursor.moveToFirst();
		int count= cursor.getCount();
		cursor.close();
		db.close();
		return count;
		
	}
	
	
	/**
	 * get all data
	 * @return
	 */
	public ArrayList<BloodInfo> getAllModle()
	{
		ArrayList<BloodInfo> lisMaterialInfos = new ArrayList<BloodInfo>();
		db = databaseHelper.getReadableDatabase();
		Cursor cursor =
		db.query(DatabaseHelper.TABLE_NAME_MODLE, null,null,null, null, null, null);
		cursor.moveToFirst();
		for(int i=0;i<cursor.getCount();i++)
		{
			cursor.moveToPosition(i);
			BloodInfo info = new BloodInfo();
			info.setDate(cursor.getString(cursor.getColumnIndex(DatabaseHelper.date)));
			info.setID(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ID)));
			info.setShousuo(cursor.getString(cursor.getColumnIndex(DatabaseHelper.shousuo)));
			info.setXinlv(cursor.getString(cursor.getColumnIndex(DatabaseHelper.xinlv)));
			info.setShuzhang(cursor.getString(cursor.getColumnIndex(DatabaseHelper.shuzhang)));
			lisMaterialInfos.add(info);    
		}
		cursor.close();
		db.close();
		return lisMaterialInfos;
		
	}
	
}