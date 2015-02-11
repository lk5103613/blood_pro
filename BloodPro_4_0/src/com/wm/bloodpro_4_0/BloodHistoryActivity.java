package com.wm.bloodpro_4_0;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.github.mikephil.charting.charts.BarLineChartBase.BorderPosition;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Legend;
import com.github.mikephil.charting.utils.Legend.LegendPosition;
import com.wm.db.BloodInfo;
import com.wm.db.DBService;

public class BloodHistoryActivity extends ActionBarActivity {
	@InjectView(R.id.blood_history_toolbar)
	Toolbar mToolbar;
	@InjectView(R.id.blood_history_line_chart)
	LineChart mLineChart;
	@InjectView(R.id.blood_history_pie)
	PieChart mPieChart;

	private DBService dbService;
	private List<BloodInfo> list;
	private String[] mParties;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.blood_history);
		ButterKnife.inject(this);

		mToolbar.setTitle(getResources().getString(R.string.blood_cartogram));
		setSupportActionBar(mToolbar);
		mToolbar.setNavigationContentDescription(getResources().getString(
				R.string.blood_cartogram));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mToolbar.setNavigationIcon(R.drawable.ic_action_previous_item);

		mParties = getResources().getStringArray(R.array.result_level);
		dbService = new DBService(BloodHistoryActivity.this);

		list = dbService.getAllModle();// ��ȡ��ʷ��¼
		initPieChart();// ��ʼ����ͼ
		initLineChart();// ��ʼ������ͼ

		if (!list.isEmpty()) {
			addEmptyData();
			setLineChartData();//Ϊ����ͼ��������
			initPieChartCurt(list.size() - 1);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isFinishing()) {
			overridePendingTransition(R.anim.scale_fade_in,
					R.anim.slide_out_to_right);
		}
	}

	/**
	 * ��ʼ����ͼ
	 */
	private void initPieChart() {
		mPieChart.setHoleColor(Color.rgb(235, 235, 235));

		Typeface tf = Typeface.createFromAsset(getAssets(),
				"OpenSans-Regular.ttf");

		mPieChart.setValueTypeface(tf);
		mPieChart.setCenterTextTypeface(Typeface.createFromAsset(getAssets(),
				"OpenSans-Light.ttf"));

		mPieChart.setHoleRadius(60f);
		mPieChart.setDescription("");
		mPieChart.setDrawYValues(true);
		mPieChart.setDrawCenterText(true);
		mPieChart.setDrawHoleEnabled(true);
		mPieChart.setRotationAngle(0);

		// draws the corresponding description value into the slice
		mPieChart.setDrawXValues(true);
		mPieChart.setRotationEnabled(true);// enable rotation of the chart by
											// touch
		mPieChart.setUsePercentValues(true);// display percentage values
		mPieChart.setCenterText(getResources().getString(R.string.idea_level));
		mPieChart.setCenterTextSize(20);
		mPieChart.setEnabled(false);
		setData(5);

		mPieChart.animateXY(1500, 1500);

		Legend l = mPieChart.getLegend();
		l.setPosition(LegendPosition.RIGHT_OF_CHART);
		l.setXEntrySpace(7f);
		l.setYEntrySpace(5f);
		mPieChart.setDrawYValues(false);
		mPieChart.setUsePercentValues(false);
		mPieChart.setDrawXValues(false);
	}

	/**
	 * ���ñ�ͼ����
	 * 
	 * @param count
	 */
	private void setData(int count) {

		ArrayList<Entry> yVals1 = new ArrayList<Entry>();

		/*
		 * IMPORTANT: In a PieChart, no values (Entry) should have the same
		 * xIndex (even if from different DataSets), since no values can be
		 * drawn above each other.
		 */
		for (int i = 0; i < count + 1; i++) {
			yVals1.add(new Entry(72, i));
		}

		ArrayList<String> xVals = new ArrayList<String>();

		for (int i = 0; i < count + 1; i++)
			xVals.add(mParties[i % mParties.length]);

		PieDataSet set1 = new PieDataSet(yVals1, "");
		set1.setSliceSpace(3f);

		//�����ɫ
		ArrayList<Integer> colors = new ArrayList<Integer>();

		for (int c : ColorTemplate.VORDIPLOM_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.JOYFUL_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.COLORFUL_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.LIBERTY_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.PASTEL_COLORS)
			colors.add(c);

		colors.add(ColorTemplate.getHoloBlue());

		set1.setColors(colors);

		PieData data = new PieData(xVals, set1);
		mPieChart.setData(data);
		mPieChart.setOverScrollMode(1);
		mPieChart.highlightValues(null);
		mPieChart.invalidate();
	}

	/**
	 * ��ʼ������ͼ
	 */
	private void initLineChart() {
		mLineChart = (LineChart) findViewById(R.id.blood_history_line_chart);
		mLineChart
				.setOnChartValueSelectedListener(new MChartValueSelectedListener());
		mLineChart.setDrawYValues(false);
		mLineChart.setDrawGridBackground(true);
		mLineChart.setDescription("");
		mLineChart.setDragEnabled(true);
		mLineChart.setSaveEnabled(true);
		mLineChart.setStartAtZero(false);
		mLineChart.setScaleMinima(list.size() / 7, 1);// �������ű���
		
		mLineChart.setDrawBorder(true);
		mLineChart.setBorderPositions(new BorderPosition[] {
				BorderPosition.BOTTOM, BorderPosition.LEFT });
	}

	/**
	 * ���X��
	 */
	public void addEmptyData() {
		ArrayList<String> xVals = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) {
			xVals.add(list.get(i).getDate());
		}
		
		//������������٣��ֶ����x���ֵ
		if (list.size() < 8) {
			for (int i = 1; i < 8-list.size(); i++) {
				Calendar nowss = Calendar.getInstance();
				String datestr = nowss.get(Calendar.MONTH) + 1 + "."
						+ (nowss.get(Calendar.DAY_OF_MONTH)+i);
				xVals.add(datestr);
			}
		}

		LineData data = new LineData(xVals);// ���� x���ֵ
		mLineChart.setData(data);
		mLineChart.invalidate();
	}

	/**
	 * �������ͼ����
	 */
	public void setLineChartData() {

		//����ѹ���ݼ�
		ArrayList<Entry> shouSuoVals = new ArrayList<Entry>();
		for (int i = 0; i < list.size(); i++) {
			shouSuoVals.add(new Entry(Float.parseFloat(list.get(i)
					.getSystolic()), i));
		}
		
		LineDataSet shouSoSet = new LineDataSet(shouSuoVals, getResources()
				.getString(R.string.systolic));
		shouSoSet.setLineWidth(2.5f);
		shouSoSet.setCircleSize(3f);
		shouSoSet.setColor(getResources().getColor(R.color.green));
		shouSoSet.setCircleColor(getResources().getColor(R.color.green));
		shouSoSet.setHighLightColor(getResources().getColor(R.color.green));

		mLineChart.getData().addDataSet(shouSoSet);

		//����ѹ���ݼ�
		ArrayList<Entry> shuZhangVals = new ArrayList<Entry>();
		for (int i = 0; i < list.size(); i++) {
			shuZhangVals.add(new Entry(Float.parseFloat(list.get(i)
					.getDiastolic()), i));
		}

		LineDataSet shuZhangSet = new LineDataSet(shuZhangVals, getResources()
				.getString(R.string.diastolic));
		shuZhangSet.setLineWidth(2.5f);
		shuZhangSet.setCircleSize(3f);
		shuZhangSet.setColor(getResources().getColor(R.color.yellow));
		shuZhangSet.setCircleColor(getResources().getColor(R.color.yellow));
		shuZhangSet.setHighLightColor(getResources().getColor(R.color.yellow));

		mLineChart.getData().addDataSet(shuZhangSet);

		//�������ݼ�
		ArrayList<Entry> xinLvVals = new ArrayList<Entry>();
		for (int i = 0; i < list.size(); i++) {
			xinLvVals.add(new Entry(Float
					.parseFloat(list.get(i).getHeartRate()), i));
		}

		LineDataSet xinLvSet = new LineDataSet(xinLvVals, getResources()
				.getString(R.string.heart_rate));
		xinLvSet.setLineWidth(2.5f);
		xinLvSet.setCircleSize(3f);
		xinLvSet.setColor(getResources().getColor(R.color.sky_blue));
		xinLvSet.setCircleColor(getResources().getColor(R.color.sky_blue));
		xinLvSet.setHighLightColor(getResources().getColor(R.color.sky_blue));

		mLineChart.getData().addDataSet(xinLvSet);
		mLineChart.animateY(1500);//����Y�ᶯ�� ����
	}

	/**
	 * ��ʼ����ǰ ����ͼ�������Ӧ�� ��ͼ
	 * @param i, ����ͼ �����������
	 */
	private void initPieChartCurt(int i) {
		BloodInfo bloodIn = list.get(i);
		int j = 0;
		int shousuoValue = Integer.parseInt(bloodIn.getSystolic());
		int shuzhangValue = Integer.parseInt(bloodIn.getDiastolic());
		mPieChart.setCenterText(getResources().getString(R.string.idea_level));

		if (90 < shousuoValue && shousuoValue < 120 && 60 < shuzhangValue
				&& shuzhangValue <= 80) {// ����Ѫѹ
			j = 0;
		} else if (100 < shousuoValue && shousuoValue < 130
				&& 60 < shuzhangValue && shuzhangValue < 90) {// ����Ѫѹ
			j = 1;
			mPieChart.setCenterText(getResources().getString(
					R.string.normal_level));
		} else if (129 < shousuoValue && shousuoValue < 140
				|| 84 < shuzhangValue && shuzhangValue < 90) {// ��Ѫѹǰ��
			j = 3;
			mPieChart.setCenterText(getResources().getString(
					R.string.before_high_pressure));
		} else if (140 < shousuoValue && 90 < shuzhangValue) {// ��Ѫѹ
			j = 4;
			mPieChart.setCenterText(getResources().getString(
					R.string.high_pressure));
		} else if (shousuoValue <= 90 && shuzhangValue <= 60) {// ��Ѫѹ
			j = 2;
			mPieChart.setCenterText(getResources().getString(
					R.string.low_pressure));
		} else if (140 < shousuoValue && shousuoValue < 160
				&& 90 < shuzhangValue && shuzhangValue < 95) {// �ٽ��Ѫѹ
			j = 5;
			mPieChart.setCenterText(getResources().getString(
					R.string.pre_high_pressure));
		}
		mPieChart.highlightValue(j, 0);
	}

	/**
	 * ����ͼ ������ ����¼�
	 * @author MGC01
	 *
	 */
	private class MChartValueSelectedListener implements
			OnChartValueSelectedListener {

		@Override
		public void onValueSelected(Entry e, int dataSetIndex) {
			if (e == null)
				return;

			Toast.makeText(
					BloodHistoryActivity.this,
					"����ѹ:" + list.get(e.getXIndex()).getDiastolic()
							+ "   ����ѹ :"
							+ list.get(e.getXIndex()).getSystolic() + "   ����:"
							+ list.get(e.getXIndex()).getHeartRate(),
					Toast.LENGTH_LONG).show();
			initPieChartCurt(e.getXIndex());
		}

		@Override
		public void onNothingSelected() {
		}

	}
}
