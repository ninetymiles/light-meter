package com.rax.lightmeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class ActivityMain extends Activity implements OnClickListener {
	
	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;
	
	private static final int MENU_SETTING = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST + 1;

	private static final int DIALOG_QUIT_CONFIRM = 1;
	
	private TextView mTextLux;
	private TextView mTextEv;
	private TextView mTextIso;
	private TextView mTextAperture;
	private TextView mTextShutter;
	
	private Button mBtnMeasure;

	private SensorManager mSensorManager;
	private Sensor mLightSensor;
	private LightMeter mMeter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mTextLux = (TextView) findViewById(R.id.main_lux_value);
		mTextEv = (TextView) findViewById(R.id.main_ev_value);
		mTextIso = (TextView) findViewById(R.id.main_iso_value);
		mTextAperture = (TextView) findViewById(R.id.main_aperture_value);
		mTextShutter = (TextView) findViewById(R.id.main_shutter_value);
		
		mBtnMeasure = (Button) findViewById(R.id.main_button);
		mBtnMeasure.setOnClickListener(this);
		mBtnMeasure.setOnTouchListener(mTouchListener);
		
		findViewById(R.id.main_button_up).setOnClickListener(this);
		findViewById(R.id.main_button_down).setOnClickListener(this);
		
		((RadioGroup) findViewById(R.id.main_option_mode)).setOnCheckedChangeListener(mOnCheckedChangeListener);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mMeter = new LightMeter();
		mMeter.setISO(200);
		
		mTextIso.setText(String.valueOf(mMeter.getISO()));
	}
	
	private String printShutterValue(int shutter) {
		String str;
		if (shutter == 0) {
			str = "N/A";
		} else if (shutter < 0) {
			str = String.format("1/%d s", -shutter);
		} else {
			str = String.format("%d s", shutter);
		}
		return str;
	}

	@Override
	protected void onPause() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onPause");
		//mSensorManager.unregisterListener(mSensorListener, mLightSensor);
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onResume");
		//mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onOptionsItemSelected itemId:" + item.getItemId());
		switch (item.getItemId()) {
		case MENU_SETTING:
			startActivity(new Intent(this, ActivitySettings.class));
			break;
		case MENU_ABOUT:
			startActivity(new Intent(this, ActivityAbout.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onClick(View v) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onClick id:" + v.getId());
		switch (v.getId()) {
		case R.id.main_button_up:
			break;
		case R.id.main_button_down:
			break;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onCreateDialog id:" + id);
		switch (id) {
		case DIALOG_QUIT_CONFIRM:
			return new AlertDialog.Builder(this)
					.setTitle(R.string.dlg_quit_confirm_title)
					.setMessage(R.string.dlg_quit_confirm_message)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									finish();
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
								}
							}).create();
		}
		return null;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onBackPressed");
		showDialog(DIALOG_QUIT_CONFIRM);
	}
	
	private OnTouchListener mTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (DEBUG) Log.v(TAG, "ActivityMain::OnTouchListener::onTouch id:" + v.getId());
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mSensorManager.unregisterListener(mSensorListener, mLightSensor);
				break;
			}
			return false;
		}
	};
	
	private OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			if (DEBUG) Log.v(TAG, "ActivityMain::OnCheckedChangeListener::onCheckedChanged checkedId:" + checkedId);
			switch (checkedId) {
			case R.id.main_option_mode_a:
				mTextAperture.setSelected(true);
				break;
			case R.id.main_option_mode_s:
				mTextShutter.setSelected(true);
				break;
			case R.id.main_option_mode_iso:
				mTextIso.setSelected(true);
				break;
			}
		}
	};
	
	private SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::SensorEventListener::onAccuracyChanged");
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::SensorEventListener::onSensorChanged");
			float lux = event.values[0];
			double ev = mMeter.caculateEv(lux);
			
			mTextLux.setText(String.valueOf(lux));
			mTextEv.setText(String.valueOf(ev));
			
			mTextLux.setText(String.format("%.2f", lux));
			mTextEv.setText(String.format("%.2f", ev));
			
			mTextAperture.setText(String.valueOf(5.6f));
			int shutter = mMeter.getShutterByFv(5.6f);
			if (shutter == 0) {
				shutter = mMeter.getShutterByFv(22f);
				mTextAperture.setText(String.valueOf(22f));
			}
			mTextShutter.setText(printShutterValue(shutter));
		}

	};


}
