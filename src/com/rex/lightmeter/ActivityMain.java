package com.rex.lightmeter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rex.flurry.FlurryAgentWrapper;
import com.rex.lightmeter.billing.google.BillingService.RequestPurchase;
import com.rex.lightmeter.billing.google.BillingService.RestoreTransactions;
import com.rex.lightmeter.billing.google.Consts;
import com.rex.lightmeter.billing.google.Consts.PurchaseState;
import com.rex.lightmeter.billing.google.Consts.ResponseCode;
import com.rex.lightmeter.billing.google.PurchaseObserver;

public class ActivityMain extends Activity implements OnClickListener, OnFocusChangeListener {
	
	private static final String TAG = "RexLog";
	private static final boolean DEBUG = true;
	
	private final String PREFS_FV = "PREFS_FV";
	private final String PREFS_TV = "PREFS_TV";
	private final String PREFS_ISO = "PREFS_ISO";
	private final String PREFS_MODE = "PREFS_MODE";
	private final String PREFS_INITIALIZED = "PREFS_INITIALIZED";
	
	private static final int DIALOG_CANNOT_CONNECT_ID = 1;
	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
	
	private static enum Mode { UNDEFINED, TV_FIRST, FV_FIRST };
	
	private long mExitTime;
	
	private TextView mTextLux;
	private TextView mTextEv;
	private Spinner mSpinnerIso;
	private Spinner mSpinnerAperture;
	private Spinner mSpinnerShutter;
	
	private Button mBtnMeasure;
	private ImageView mBtnDot;

	private SensorManager mSensorManager;
	private Sensor mLightSensor;
	private LightMeter mMeter;
	private OrientationHelper mOrientation;
	
	private MainSpinnerItem[] mISOValue;
	private MainSpinnerItem[] mApertureValue;
	private MainSpinnerItem[] mShutterValue;
	
	private List<Integer> mArrISO;
	private List<Double> mArrAperture;
	private List<Double> mArrShutter;
	
	private boolean mIsEnableVolumeKey = true;
	private LightMeter.STEP mEvStep = LightMeter.STEP.THIRD;
	private double mFv = 8f;
	private double mTv = -60;
	private int mISO = 200;
	private Mode mMode = Mode.UNDEFINED;

	// Google IAP
//	private Handler mHandler;
//	private BillingService mBillingService;
//	private PurchaseDatabase mPurchaseDatabase;
//	private RexPurchaseObserver mPurchaseObserver;
	
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
		
		mSpinnerIso = (Spinner) findViewById(R.id.main_iso_value);
		mSpinnerIso.setOnItemSelectedListener(mSpinnerSelectedListener);
		mSpinnerIso.setFocusableInTouchMode(true);
		
		mSpinnerAperture = (Spinner) findViewById(R.id.main_aperture_value);
		mSpinnerAperture.setOnItemSelectedListener(mSpinnerSelectedListener);
		mSpinnerAperture.setOnFocusChangeListener(this);
		mSpinnerAperture.setFocusableInTouchMode(true);
		mSpinnerShutter = (Spinner) findViewById(R.id.main_shutter_value);
		mSpinnerShutter.setOnItemSelectedListener(mSpinnerSelectedListener);
		mSpinnerShutter.setOnFocusChangeListener(this);
		mSpinnerShutter.setFocusableInTouchMode(true);
		
		mBtnMeasure = (Button) findViewById(R.id.main_button);
		mBtnMeasure.setOnTouchListener(mTouchListener);
		mBtnMeasure.setOnClickListener(this);
		mBtnMeasure.setOnLongClickListener(mLongClickListener);
		mBtnDot = (ImageView) findViewById(R.id.main_button_dot);
		mBtnDot.setVisibility(View.GONE);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mMeter = new LightMeter();
		mMeter.setISO(200);
		
//		mHandler = new Handler();
		
//		mPurchaseObserver = new RexPurchaseObserver(mHandler);
//		mBillingService = new BillingService();
//		mBillingService.setContext(this);
//
//		mPurchaseDatabase = new PurchaseDatabase(this);
//		
//		// Check if billing is supported.
//		ResponseHandler.register(mPurchaseObserver);
//		if (!mBillingService.checkBillingSupported()) {
//			showDialog(DIALOG_CANNOT_CONNECT_ID);
//		}
	}
	
	private String printApertureValue(double aperture) {
		if (DEBUG) Log.v(TAG, "ActivityMain::printApertureValue aperture:" + aperture);
		String str = "";
		if (aperture == LightMeter.MIN_APERTURE_VALUE) {
			str = "MIN";
		} else if (aperture == LightMeter.MAX_APERTURE_VALUE) {
			str = "MAX";
		} else {
			str = String.valueOf(aperture);
		}
		return str;
	}
	
	private String printShutterValue(double shutter) {
		if (DEBUG) Log.v(TAG, "ActivityMain::printShutterValue shutter:" + shutter);
		String str = "";
		if (shutter == LightMeter.MIN_SHUTTER_VALUE) {
			str = "MIN";
		} else if (shutter == LightMeter.MAX_SHUTTER_VALUE) {
			str = "MAX";
		} else if (shutter < 0) {
			str = "1/" + String.valueOf(Math.abs(shutter));
		} else if (shutter > 60) {
			int sec = (int) shutter % 60;
			int min = (int) shutter / 60 % 60;
			int hour = (int) shutter / 3600;
			if (hour > 0) {
				str = hour + "h " + min + "m " + sec + "\"";
			} else if (min > 0) {
				str = min + "m " + sec + "\"";
			}
		} else {
			str = String.valueOf(shutter) + "\"";
		}
		return str;
	}
	
	@Override
	protected void onStart() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onStart");
		FlurryAgentWrapper.onStartSession(this);

//		ResponseHandler.register(mPurchaseObserver);
//		initializeOwnedItems();
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onStop");
		FlurryAgentWrapper.onEndSession(this);
//		ResponseHandler.unregister(mPurchaseObserver);
		super.onStop();
	}
	
	@Override
	protected void onPause() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onPause");
		//mSensorManager.unregisterListener(mSensorListener, mLightSensor);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit()
				.putFloat(PREFS_FV, (float) mFv)
				.putFloat(PREFS_TV, (float) mTv)
				.putInt(PREFS_ISO, mISO)
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
		mMeter.setStep(mEvStep);
		mFv = mMeter.getMatchAperture(prefs.getFloat(PREFS_FV, 8f));
		mTv = prefs.getFloat(PREFS_TV, -60);
		mTv = mMeter.getMatchShutter((mTv < 0) ? (-1 / mTv) : mTv);
		mISO = prefs.getInt(PREFS_ISO, 200);
		mMode = Mode.values()[prefs.getInt(PREFS_MODE, 0)];
		setMode(mMode);
		
		Integer value;
		int position = 0;
		
		mArrISO = mMeter.getISOArray();
		mISOValue = new MainSpinnerItem[mArrISO.size()];
		for (int i = 0; i < mArrISO.size(); i++) {
			value = mArrISO.get(i);
			mISOValue[i] = new MainSpinnerItem(value, String.valueOf(value));
			if (value == mISO) {
				position = i;
			}
		}
		
		ArrayAdapter<MainSpinnerItem> adapterISO;
		adapterISO = new ArrayAdapter<MainSpinnerItem>(this, android.R.layout.simple_spinner_item, mISOValue);
		adapterISO.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		mSpinnerIso.setAdapter(adapterISO);
		mSpinnerIso.setSelection(position);
		
		Double aperture;
		mArrAperture = mMeter.getApertureArray();
		mApertureValue = new MainSpinnerItem[mArrAperture.size()];
		for (int i = 0; i < mArrAperture.size(); i++) {
			aperture = mArrAperture.get(i);
			mApertureValue[i] = new MainSpinnerItem(aperture, printApertureValue(aperture));
			if (aperture == mFv) {
				position = i;
			}
		}
		ArrayAdapter<MainSpinnerItem> adapterAperture;
		adapterAperture = new ArrayAdapter<MainSpinnerItem>(this, android.R.layout.simple_spinner_item, mApertureValue);
		adapterAperture.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		mSpinnerAperture.setAdapter(adapterAperture);
		mSpinnerAperture.setSelection(position);
		
		Double shutter;
		mArrShutter = mMeter.getShutterArray();
		mShutterValue = new MainSpinnerItem[mArrShutter.size()];
		for (int i = 0; i < mArrShutter.size(); i++) {
			shutter = mArrShutter.get(i);
			mShutterValue[i] = new MainSpinnerItem(shutter, printShutterValue(shutter));
			if (shutter == mTv) {
				position = i;
			}
		}
		ArrayAdapter<MainSpinnerItem> adapterShutter;
		adapterShutter = new ArrayAdapter<MainSpinnerItem>(this, android.R.layout.simple_spinner_item, mShutterValue);
		adapterShutter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		mSpinnerShutter.setAdapter(adapterShutter);
		mSpinnerShutter.setSelection(position);
		
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
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onDestroy");
//		mPurchaseDatabase.close();
//		mBillingService.unbind();
		super.onDestroy();
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
		case R.id.menu_feedback:
			UtilHelper.sendEmail(this);
			break;
		case R.id.menu_share:
			UtilHelper.shareThisApp(this);
			break;
		case R.id.menu_about:
			startActivity(new Intent(this, ActivityAbout.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void setMode(Mode mode) {
		if (DEBUG) Log.v(TAG, "ActivityMain::setMode mode:" + mode);
		mMode = mode;
		findViewById(R.id.main_aperture_mode).setSelected(mMode == Mode.FV_FIRST);
		findViewById(R.id.main_shutter_mode).setSelected(mMode == Mode.TV_FIRST);
	}
	
	private void updateEv() {
		if (DEBUG) Log.v(TAG, "ActivityMain::updateEv mMode:" + mMode + " mFv:" + mFv + " mTv:" + mTv);
		double shutter = 0;
		double aperture = 0;
		switch (mMode) {
		case UNDEFINED:
			aperture = 2.8d;
			shutter = mMeter.getShutterByAperture(aperture);
			if (shutter == 0) {
				aperture = 5.6d;
				shutter = mMeter.getShutterByAperture(aperture);
				if (shutter == 0) {
					aperture = 11d;
					shutter = mMeter.getShutterByAperture(aperture);
					if (shutter == 0) {
						aperture = 22d;
						shutter = mMeter.getShutterByAperture(aperture);
					}
				}
			}
			break;
		case TV_FIRST:
			shutter = mTv;
			aperture = mMeter.getApertureByShutter(shutter);
			break;
		case FV_FIRST:
			aperture = mFv;
			shutter = mMeter.getShutterByAperture(aperture);
			break;
		}
		if (DEBUG) Log.v(TAG, "ActivityMain::updateEv shutter:" + shutter + " aperture:" + aperture);
		mSpinnerAperture.setSelection(mArrAperture.indexOf(aperture));
		mSpinnerShutter.setSelection(mArrShutter.indexOf(shutter));
		mTv = shutter;
		mFv = aperture;
	}
	
	@Override
	public void onClick(View v) {
		//if (DEBUG) Log.v(TAG, "ActivityMain::onClick id:" + v.getId());
		switch (v.getId()) {
		case R.id.main_button:
			//if (DEBUG) Log.v(TAG, "ActivityMain::onClick BUTTON_MEASURE");
//			if (mBtnDot.isSelected()) {
//				mBtnDot.setSelected(false);
//				doStopMeasure();
//			}
			break;
		}
	}
	
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onFocusChange id:" + v.getId() + " hasFocus:" + hasFocus);
		if (hasFocus) {
			switch (v.getId()) {
			case R.id.main_aperture_value: setMode(Mode.FV_FIRST); break;
			case R.id.main_shutter_value: setMode(Mode.TV_FIRST); break;
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
				if (mSpinnerIso.isFocused()) {
					int position = mSpinnerIso.getSelectedItemPosition();
					if (position != AdapterView.INVALID_POSITION) {
						position--;
						if (position < 0) position = 0;
						mSpinnerIso.setSelection(position);
					}
				}
				if (mSpinnerAperture.isFocused()) {
					int position = mSpinnerAperture.getSelectedItemPosition();
					if (position != AdapterView.INVALID_POSITION) {
						position--;
						if (position < 0) position = 0;
						mSpinnerAperture.setSelection(position);
					}
				}
				if (mSpinnerShutter.isFocused()) {
					int position = mSpinnerShutter.getSelectedItemPosition();
					if (position != AdapterView.INVALID_POSITION) {
						position--;
						if (position < 0) position = 0;
						mSpinnerShutter.setSelection(position);
					}
				}
				handled = true;
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (mSpinnerIso.isFocused()) {
					int position = mSpinnerIso.getSelectedItemPosition();
					if (position != AdapterView.INVALID_POSITION) {
						position++;
						if (position >= mArrISO.size()) position = mArrISO.size() - 1;
						mSpinnerIso.setSelection(position);
					}
				}
				if (mSpinnerAperture.isFocused()) {
					int position = mSpinnerAperture.getSelectedItemPosition();
					if (position != AdapterView.INVALID_POSITION) {
						position++;
						if (position >= mArrAperture.size()) position = mArrAperture.size() - 1;
						mSpinnerAperture.setSelection(position);
					}
				}
				if (mSpinnerShutter.isFocused()) {
					int position = mSpinnerShutter.getSelectedItemPosition();
					if (position != AdapterView.INVALID_POSITION) {
						position++;
						if (position >= mArrShutter.size()) position = mArrShutter.size() - 1;
						mSpinnerShutter.setSelection(position);
					}
				}
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
	protected void onSaveInstanceState(Bundle outState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onSaveInstanceState");
		outState.putDouble("LUX", mMeter.getLux());
//		outState.putBoolean("DOT", mBtnDot.isSelected());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onRestoreInstanceState");
		mMeter.setLux(savedInstanceState.getDouble("LUX"));
		mTextLux.setText(String.format("%.2f", mMeter.getLux()));
		mTextEv.setText(String.format("%.2f", mMeter.getEv()));
//		if (savedInstanceState.getBoolean("DOT")) {
//			mBtnDot.setSelected(true);
//			doStartMeasure();
//		}
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public void onBackPressed() {
		if (DEBUG) Log.v(TAG, "ActivityMain::onBackPressed");
		
		if (System.currentTimeMillis() - mExitTime > 2000) {
			Toast.makeText(getApplicationContext(), R.string.toast_quit, Toast.LENGTH_SHORT).show();
			mExitTime = System.currentTimeMillis();
		} else {
			finish();
		}
	}
	
	// FIXME: Should remove this
	private void clearFocus() {
		if (mSpinnerIso.isFocused()) mSpinnerIso.clearFocus();
		if (mSpinnerAperture.isFocused()) mSpinnerAperture.clearFocus();
		if (mSpinnerShutter.isFocused()) mSpinnerShutter.clearFocus();
	}
	
	private void doStartMeasure() {
		if (DEBUG) Log.v(TAG, "ActivityMain::doStartMeasure");
		mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
		FlurryAgentWrapper.logEvent("MEASURE", true);
		mOrientation.lock();
	}
	
	private void doStopMeasure() {
		if (DEBUG) Log.v(TAG, "ActivityMain::doStopMeasure");
		mSensorManager.unregisterListener(mSensorListener, mLightSensor);
		FlurryAgentWrapper.endTimedEvent("MEASURE");
		mOrientation.unlock();
	}
	
	private OnItemSelectedListener mSpinnerSelectedListener = new OnItemSelectedListener() {
		
		@Override
		public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
			if (DEBUG && false) {
				Log.v(TAG, "ActivityMain::OnItemSelectedListener::onItemSelected" +
					" parentView:" + parentView.getId() + 
					" position:" + position + 
					" id:" + id);
			}
			switch (parentView.getId()) {
			case R.id.main_iso_value:
				if (DEBUG) Log.v(TAG, "ActivityMain::OnItemSelectedListener::onItemSelected ISO:" + mISOValue[position]);
				mISO = (Integer) mISOValue[position].getValue();
				break;
			case R.id.main_aperture_value:
				if (DEBUG) Log.v(TAG, "ActivityMain::OnItemSelectedListener::onItemSelected Aperture:" + mApertureValue[position]);
				mFv = (Double) mApertureValue[position].getValue();
				break;
			case R.id.main_shutter_value:
				if (DEBUG) Log.v(TAG, "ActivityMain::OnItemSelectedListener::onItemSelected Shutter:" + mShutterValue[position]);
				mTv = (Double) mShutterValue[position].getValue();
				break;
			}
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
			if (DEBUG) Log.v(TAG, "ActivityMain::OnItemSelectedListener::onNothingSelected");
		}
	};
	
	private OnLongClickListener mLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::OnLongClickListener::onLongClick");
			//mBtnDot.setSelected(true);
			return true;
		}
	};
	
	private OnTouchListener mTouchListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			//if (DEBUG) Log.v(TAG, "ActivityMain::OnTouchListener::onTouch id:" + v.getId() + " action:" + event.getAction());
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				doStartMeasure();
//				if (mBtnDot.isSelected() == false) {
//					doStartMeasure();
//				}
				clearFocus();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				doStopMeasure();
//				if (mBtnDot.isSelected() == false) {
//					doStopMeasure();
//				}
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
	
	private class RexPurchaseObserver extends PurchaseObserver {
		
		public RexPurchaseObserver(Handler handler) {
			super(ActivityMain.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
			if (Consts.DEBUG) {
				Log.i(TAG, "supported: " + supported);
			}
			if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
				if (supported) {
					//restoreDatabase();
					// Update UI, enable buy button
				} else {
					//showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
				}
			}
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {
			if (Consts.DEBUG) {
				Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " "
						+ purchaseState);
			}

			if (purchaseState == PurchaseState.PURCHASED) {
				//mOwnedItems.add(itemId);
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			if (Consts.DEBUG) {
				Log.d(TAG, request.mProductId + ": " + responseCode);
			}
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) {
					Log.i(TAG, "purchase was successfully sent to server");
				}
			} else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
				if (Consts.DEBUG) {
					Log.i(TAG, "user canceled purchase");
				}
			} else {
				if (Consts.DEBUG) {
					Log.i(TAG, "purchase failed");
				}
			}
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) {
					Log.d(TAG, "completed RestoreTransactions request");
				}
				// Update the shared preferences so that we don't perform
				// a RestoreTransactions again.
				SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putBoolean(PREFS_INITIALIZED, true);
				edit.commit();
			} else {
				if (Consts.DEBUG) {
					Log.d(TAG, "RestoreTransactions error: " + responseCode);
				}
			}
		}
	}
	
	/**
	 * If the database has not been initialized, we send a RESTORE_TRANSACTIONS
	 * request to Android Market to get the list of purchased items for this
	 * user. This happens if the application has just been installed or the user
	 * wiped data. We do not want to do this on every startup, rather, we want
	 * to do only when the database needs to be initialized.
	 */
	private void restoreDatabase() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		boolean initialized = prefs.getBoolean(PREFS_INITIALIZED, false);
		if (!initialized) {
//			mBillingService.restoreTransactions();
			//Toast.makeText(this, R.string.restoring_transactions, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Creates a background thread that reads the database and initializes the
	 * set of owned items.
	 */
	private void initializeOwnedItems() {
		new Thread(new Runnable() {
			@Override
			public void run() {
//				Cursor cursor = mPurchaseDatabase.queryAllPurchasedItems();
//				if (cursor == null) {
//					return;
//				}

//				final Set<String> ownedItems = new HashSet<String>();
//				try {
//					int productIdCol = cursor
//							.getColumnIndexOrThrow(PurchaseDatabase.PURCHASED_PRODUCT_ID_COL);
//					while (cursor.moveToNext()) {
//						String productId = cursor.getString(productIdCol);
//						ownedItems.add(productId);
//					}
//				} finally {
//					cursor.close();
//				}

				// We will add the set of owned items in a new Runnable that runs on
				// the UI thread so that we don't need to synchronize access to
				// mOwnedItems.
//				mHandler.post(new Runnable() {
//					@Override
//					public void run() {
//						mOwnedItems.addAll(ownedItems);
//					}
//				});
			}
		}).start();
	}
	
}
