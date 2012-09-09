package com.rax.lightmeter;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class ActivityMain extends Activity {
	
	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;
	
	private static final int MENU_SETTING = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST + 1;

	private TextView mTextAccuracy;
	private TextView mTextValue1;
	private TextView mTextValue2;
	private TextView mTextValue3;

	private SensorManager mSensorManager;
	private Sensor mLightSensor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTextAccuracy = (TextView) findViewById(R.id.main_accuracy);
		mTextValue1 = (TextView) findViewById(R.id.main_value1);
		mTextValue2 = (TextView) findViewById(R.id.main_value2);
		mTextValue3 = (TextView) findViewById(R.id.main_value3);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	}

	@Override
	protected void onPause() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onPause");
		mSensorManager.unregisterListener(mSensorListener, mLightSensor);
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onResume");
		mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onPrepareOptionsMenu");
		menu.add(0, MENU_SETTING, 0, R.string.menu_setting).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		return super.onCreateOptionsMenu(menu);
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		//return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onPrepareOptionsMenu");
		return super.onPrepareOptionsMenu(menu);
	}

	private SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::SensorEventListener::onAccuracyChanged");
			mTextAccuracy.setText(String.valueOf(accuracy));
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::SensorEventListener::onSensorChanged");
			float[] values = event.values;
			mTextValue1.setText(String.valueOf(values[0]));
			mTextValue2.setText(String.valueOf(values[1]));
			mTextValue3.setText(String.valueOf(values[2]));
		}

	};
}
