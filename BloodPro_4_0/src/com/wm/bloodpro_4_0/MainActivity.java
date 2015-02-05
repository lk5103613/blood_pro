package com.wm.bloodpro_4_0;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import com.wm.tools.ProgressWheel;

public class MainActivity extends Activity {
	
	@InjectView(R.id.progress_bar)
	ProgressWheel progress;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.inject(this);
	}
		
	
	@OnClick(R.id.progress_bar)
	public void clickProgress(View v) {
		if(progress.isSpinning()) {
			progress.stopSpinning();
			progress.setText("¿ªÊ¼¼ì²â");
		} else {
			progress.spin();
			progress.setText("Í£Ö¹¼ì²â");
		}
	}
	
}
