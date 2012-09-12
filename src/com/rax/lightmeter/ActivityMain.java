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
import android.widget.TextView;

public class ActivityMain extends Activity {
	
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

	private SensorManager mSensorManager;
	private Sensor mLightSensor;
	private LightMeter mMeter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mTextLux = (TextView) findViewById(R.id.main_lux);
		mTextEv = (TextView) findViewById(R.id.main_ev);
		mTextIso = (TextView) findViewById(R.id.main_iso);
		mTextAperture = (TextView) findViewById(R.id.main_aperture);
		mTextShutter = (TextView) findViewById(R.id.main_shutter);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mMeter = new LightMeter();
		mMeter.setISO(100);
		
		mTextIso.setText(String.valueOf(mMeter.getISO()));
	}
	
	private String printShutterValue(int shutter) {
		String str;
		if (shutter == 0) {
			str = "N/A";
		} else if (shutter < 0) {
			str = String.format("1 / %d s", -shutter);
		} else {
			str = String.format("%d s", shutter);
		}
		return str;
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
			
			float f = mMeter.getFvByShutter(-60);
			if (f != 0) {
				mTextAperture.setText(String.valueOf(f));
				mTextShutter.setText(printShutterValue(-60));
			} else {
				int shutter = mMeter.getShutterByFv(5.6f);
				if (shutter == 0) {
					shutter = mMeter.getShutterByFv(22f);
					mTextAperture.setText(String.valueOf(22f));
				} else {
					mTextAperture.setText(String.valueOf(5.6f));
				}
				mTextShutter.setText(printShutterValue(shutter));
			}
		}

	};
}
