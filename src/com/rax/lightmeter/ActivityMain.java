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
	private TextView mTextLux;
	private TextView mTextEv;
	private TextView mTextIso;
	private TextView mTextAperture;
	private TextView mTextShutter;

	private SensorManager mSensorManager;
	private Sensor mLightSensor;
	private LightMeter mMeter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTextAccuracy = (TextView) findViewById(R.id.main_accuracy);
		mTextLux = (TextView) findViewById(R.id.main_lux);
		mTextEv = (TextView) findViewById(R.id.main_ev);
		mTextIso = (TextView) findViewById(R.id.main_iso);
		mTextAperture = (TextView) findViewById(R.id.main_aperture);
		mTextShutter = (TextView) findViewById(R.id.main_shutter);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mMeter = new LightMeter();
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
			float lux = event.values[0];
			double ev = mMeter.caculateEv(lux);
			
			mTextLux.setText(String.valueOf(lux));
			mTextEv.setText(String.valueOf(ev));
		}

	};
}
