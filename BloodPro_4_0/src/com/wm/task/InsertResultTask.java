package com.wm.task;

import java.util.Calendar;

import com.wm.db.BloodInfo;
import com.wm.db.DBService;
import com.wn.entity.ResultInfo;

import android.content.Context;
import android.os.AsyncTask;

/**
 * @author MGC01
 *
 */
public class InsertResultTask extends AsyncTask<String, Integer, Long>{
	private BloodInfo bloodInfo;
	private DBService dbService;
	private ResultInfo mResultInfo;
	private Context context;
	
	public InsertResultTask(Context context,  ResultInfo resultInfo){
		this.context = context;
		this.mResultInfo = resultInfo;
	}

	@Override
	protected Long doInBackground(String... params) {
		dbService = new DBService(context);
		bloodInfo = new BloodInfo();
		bloodInfo.setHeartRate(mResultInfo.heartRate);
		bloodInfo.setSystolic(mResultInfo.systolic);
		bloodInfo.setDiastolic(mResultInfo.diastolic);
		Calendar nowss = Calendar.getInstance();
		String datestr = nowss.get(Calendar.MONTH) + 1 + "."
				+ nowss.get(Calendar.DAY_OF_MONTH);
		bloodInfo.setDate(datestr);
		long result = dbService.insertModleData(bloodInfo);
		return result;
	}

}
