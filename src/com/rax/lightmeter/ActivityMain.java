package com.rax.lightmeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActivityMain extends Activity implements OnClickListener, OnFocusChangeListener {
	
	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;
	
	private final String PREFS_FV = "PREFS_FV";
	private final String PREFS_TV = "PREFS_TV";
	private final String PREFS_ISO = "PREFS_ISO";
	private final String PREFS_MODE = "PREFS_MODE";
	
	private static final int DIALOG_QUIT_CONFIRM = 1;
	private static enum Mode { UNDEFINED, TV_FIRST, FV_FIRST };
	
	private TextView mTextLux;
	private TextView mTextEv;
	private TextView mTextIso;
	private TextView mTextAperture;
	private TextView mTextShutter;
	
	private LinearLayout mLcdLayout;
	private LinearLayout mBtnLayout;
	
	private Button mBtnMeasure;
	private Button mBtnUp;
	private Button mBtnDown;

	private SensorManager mSensorManager;
	private Sensor mLightSensor;
	private LightMeter mMeter;
	private OrientationHelper mOrientation;
	
	private boolean mIsEnableVolumeKey = true;
	private LightMeter.STEP mEvStep = LightMeter.STEP.THIRD;
	private double mFv = 8f;
	private double mTv = -60;
	private int mISO = 200;
	private Mode mMode = Mode.UNDEFINED;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onCreate");
		super.onCreate(savedInstanceState);
		
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("CONF_ENABLE_KEEP_SCREEN_ON", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		
		setContentView(R.layout.activity_main);
		mOrientation = new OrientationHelper(this);
		
		mTextLux = (TextView) findViewById(R.id.main_lux_value);
		mTextEv = (TextView) findViewById(R.id.main_ev_value);
		mTextIso = (TextView) findViewById(R.id.main_iso_value);
		mTextIso.setOnClickListener(this);
		mTextIso.setOnFocusChangeListener(this);
		mTextAperture = (TextView) findViewById(R.id.main_aperture_value);
		mTextAperture.setOnClickListener(this);
		mTextAperture.setOnFocusChangeListener(this);
		mTextShutter = (TextView) findViewById(R.id.main_shutter_value);
		mTextShutter.setOnClickListener(this);
		mTextShutter.setOnFocusChangeListener(this);
		
		mLcdLayout = (LinearLayout) findViewById(R.id.main_lcd_layout);
		mLcdLayout.setOnClickListener(this);
		
		mBtnLayout = (LinearLayout) findViewById(R.id.main_button_layout);
		mBtnMeasure = (Button) findViewById(R.id.main_button);
		mBtnMeasure.setOnClickListener(this);
		mBtnMeasure.setOnTouchListener(mTouchListener);
		mBtnUp = (Button) findViewById(R.id.main_button_up);
		mBtnUp.setOnClickListener(this);
		mBtnDown = (Button) findViewById(R.id.main_button_down);
		mBtnDown.setOnClickListener(this);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mMeter = new LightMeter();
		mMeter.setISO(200);
		
		mTextIso.setText(String.valueOf(mMeter.getISO()));
	}
	
	private String printShutterValue(double shutter) {
		if (DEBUG) Log.v(TAG, "ActivityMain::printShutterValue shutter:" + shutter);
		String str;
		if (shutter == 0) {
			str = "N/A";
		} else if (shutter < 0) {
			str = "1/" + String.valueOf(Math.abs(shutter));
		} else {
			str = String.valueOf(shutter);
		}
		return str;
	}

	@Override
	protected void onPause() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onPause");
		//mSensorManager.unregisterListener(mSensorListener, mLightSensor);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit()
				.putFloat(PREFS_FV, (float) mFv)
				.putFloat(PREFS_TV, (float) mTv).putInt(PREFS_ISO, mISO)
				.putInt(PREFS_MODE, mMode.ordinal())
				.commit();
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onResume");
		//mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mIsEnableVolumeKey = prefs.getBoolean("CONF_ENABLE_VOLUME_KEY", true);
		mEvStep = LightMeter.STEP.values()[Integer.valueOf(prefs.getString("CONF_EV_STEP", "2"))];
		mFv = mMeter.getMatchFv(prefs.getFloat(PREFS_FV, 8f));
		mTv = prefs.getFloat(PREFS_TV, -60);
		mTv = mMeter.getMatchTv((mTv < 0) ? (-1 / mTv) : mTv);
		mISO = prefs.getInt(PREFS_ISO, 200);
		mMode = Mode.values()[prefs.getInt(PREFS_MODE, 0)];
		setMode(mMode);
		mTextAperture.setText(String.valueOf(mFv));
		mTextShutter.setText(printShutterValue(mTv));
		if (DEBUG) Log.v(TAG, "ActivityMain::onResume" +
				" mIsEnableVolumeKey:" + mIsEnableVolumeKey + 
				" mEvStep:" + mEvStep +
				" mMode:" + mMode +
				" mISO:" + mISO +
				" mFv:" + mFv + 
				" mTv:" + mTv);
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onOptionsItemSelected itemId:" + item.getItemId());
		switch (item.getItemId()) {
		case R.id.menu_setting:
			startActivity(new Intent(this, ActivitySettings.class));
			break;
		case R.id.menu_about:
			startActivity(new Intent(this, ActivityAbout.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void setMode(Mode mode) {
		if (DEBUG) Log.v(TAG, "ActivityMain::setMode mode:" + mode);
		mMode = mode;
		findViewById(R.id.main_aperture_mode).setPressed(mMode == Mode.FV_FIRST);
		findViewById(R.id.main_shutter_mode).setPressed(mMode == Mode.TV_FIRST);
	}
	
	private void updateEv() {
		if (DEBUG) Log.v(TAG, "ActivityMain::updateEv mMode:" + mMode + " mFv:" + mFv + " mTv:" + mTv);
		double shutter = 0;
		double aperture = 0;
		switch (mMode) {
		case UNDEFINED:
			aperture = 2.8d;
			shutter = mMeter.getTByFv(aperture);
			if (shutter == 0) {
				aperture = 5.6d;
				shutter = mMeter.getTByFv(aperture);
				if (shutter == 0) {
					aperture = 11d;
					shutter = mMeter.getTByFv(aperture);
					if (shutter == 0) {
						aperture = 22d;
						shutter = mMeter.getTByFv(aperture);
					}
				}
			}
			break;
		case TV_FIRST:
			shutter = mTv;
			aperture = mMeter.getFvByT(shutter);
			break;
		case FV_FIRST:
			aperture = mFv;
			shutter = mMeter.getTByFv(aperture);
			break;
		}
		mTextAperture.setText(String.valueOf(aperture));
		mTextShutter.setText(printShutterValue(shutter));
	}
	
	@Override
	public void onClick(View v) {
		//if (DEBUG) Log.v(TAG, "ActivityMain::onClick id:" + v.getId());
		switch (v.getId()) {
		case R.id.main_button_up:
			if (DEBUG) Log.v(TAG, "ActivityMain::onClick BUTTON_UP");
			if (mTextIso.isFocused()) {
				mISO = mMeter.getISO(mEvStep, true);
				mTextIso.setText(String.valueOf(mISO));
				mMeter.setISO(mISO);
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mISO:" + mISO);
			}
			if (mTextShutter.isFocused()) {
				mTv = mMeter.getTv(mEvStep, mTv, true);
				mTextShutter.setText(printShutterValue(mTv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mTv:" + mTv);
			}
			if (mTextAperture.isFocused()) {
				mFv = mMeter.getFv(mEvStep, mFv, true);
				mTextAperture.setText(String.valueOf(mFv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mFv:" + mFv);
			}
			break;
		case R.id.main_button_down:
			if (DEBUG) Log.v(TAG, "ActivityMain::onClick BUTTON_DOWN");
			if (mTextIso.isFocused()) {
				mISO = mMeter.getISO(mEvStep, false);
				mTextIso.setText(String.valueOf(mISO));
				mMeter.setISO(mISO);
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mISO:" + mISO);
			}
			if (mTextShutter.isFocused()) {
				mTv = mMeter.getTv(mEvStep, mTv, false);
				mTextShutter.setText(printShutterValue(mTv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mTv:" + mTv);
			}
			if (mTextAperture.isFocused()) {
				mFv = mMeter.getFv(mEvStep, mFv, false);
				mTextAperture.setText(String.valueOf(mFv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mFv:" + mFv);
			}
			break;
		}
	}
	
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onFocusChange id:" + v.getId() + " hasFocus:" + hasFocus);
		
		switch (v.getId()) {
		case R.id.main_aperture_value: setMode(Mode.FV_FIRST); break;
		case R.id.main_shutter_value: setMode(Mode.TV_FIRST); break;
		}
		
		if (mTextIso.isFocused() || mTextAperture.isFocused() || mTextShutter.isFocused()) {
			if (mBtnLayout.isShown() == false) {
				mBtnLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
				mBtnLayout.setVisibility(View.VISIBLE);
			}
		} else {
			if (mBtnLayout.isShown()) {
				mBtnLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
				mBtnLayout.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onKeyDown keyCode:" + keyCode);
		boolean handled = false;
		if (mIsEnableVolumeKey) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mBtnDown.performClick();
				handled = true;
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				mBtnUp.performClick();
				handled = true;
				break;
			}
		}
		return handled || super.onKeyDown(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onConfigurationChanged");
		
		super.onConfigurationChanged(newConfig);
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
	public void onBackPressed() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onBackPressed");
		showDialog(DIALOG_QUIT_CONFIRM);
	}
	
	private OnTouchListener mTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::OnTouchListener::onTouch id:" + v.getId() + " action:" + event.getAction());
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
				mOrientation.lock();
				clearFocus();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mSensorManager.unregisterListener(mSensorListener, mLightSensor);
				mOrientation.unlock();
				break;
			}
			return false;
		}
	};
	
	private SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			if (DEBUG) Log.v(TAG, "ActivityMain::SensorEventListener::onAccuracyChanged accuracy:" + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::SensorEventListener::onSensorChanged");
			float lux = event.values[0];
			double ev = mMeter.setLux(lux);
			mTextLux.setText(String.format("%.2f", lux));
			mTextEv.setText(String.format("%.2f", ev));
			updateEv();
		}
	};
	
	private void clearFocus() {
		if (mTextIso.isFocused()) mTextIso.clearFocus();
		if (mTextAperture.isFocused()) mTextAperture.clearFocus();
		if (mTextShutter.isFocused()) mTextShutter.clearFocus();
	}
}
