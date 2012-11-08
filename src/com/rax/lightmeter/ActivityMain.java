package com.rax.lightmeter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
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
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rax.flurry.FlurryAgentWrapper;
import com.rax.lightmeter.billing.google.BillingService;
import com.rax.lightmeter.billing.google.BillingService.RequestPurchase;
import com.rax.lightmeter.billing.google.BillingService.RestoreTransactions;
import com.rax.lightmeter.billing.google.Consts;
import com.rax.lightmeter.billing.google.Consts.PurchaseState;
import com.rax.lightmeter.billing.google.Consts.ResponseCode;
import com.rax.lightmeter.billing.google.PurchaseDatabase;
import com.rax.lightmeter.billing.google.PurchaseObserver;

public class ActivityMain extends Activity implements OnClickListener, OnFocusChangeListener {
	
	private static final String TAG = "RaxLog";
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

	// Google IAP
//	private Handler mHandler;
//	private BillingService mBillingService;
//	private PurchaseDatabase mPurchaseDatabase;
//	private RaxPurchaseObserver mPurchaseObserver;
	
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
		
//		mHandler = new Handler();
		
//		mPurchaseObserver = new RaxPurchaseObserver(mHandler);
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
	
	private String printShutterValue(double shutter) {
		if (DEBUG) Log.v(TAG, "ActivityMain::printShutterValue shutter:" + shutter);
		String str = "";
		if (shutter == 0) {
			str = "N/A";
		} else if (shutter < 0) {
			str = "1/" + String.valueOf(Math.abs(shutter));
		} else if (shutter > 60) {
			int sec = (int) shutter % 60;
			int min = (int) shutter / 60 % 60;
			int hour = (int) shutter / 3600;
			if (hour > 0) {
				str = hour + "h " + min + "\" " + sec;
			} else if (min > 0) {
				str = min + "\" " + sec;
			}
		} else {
			str = String.valueOf(shutter);
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
		mMeter.setStep(mEvStep);
		mFv = mMeter.getMatchAperture(prefs.getFloat(PREFS_FV, 8f));
		mTv = prefs.getFloat(PREFS_TV, -60);
		mTv = mMeter.getMatchShutter((mTv < 0) ? (-1 / mTv) : mTv);
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
		mTextAperture.setText(String.valueOf(aperture));
		mTextShutter.setText(printShutterValue(shutter));
		mTv = shutter;
		mFv = aperture;
	}
	
	@Override
	public void onClick(View v) {
		//if (DEBUG) Log.v(TAG, "ActivityMain::onClick id:" + v.getId());
		switch (v.getId()) {
		case R.id.main_button_up:
			if (DEBUG) Log.v(TAG, "ActivityMain::onClick BUTTON_UP");
			if (mTextIso.isFocused()) {
				mISO = mMeter.getNextISO();
				mTextIso.setText(String.valueOf(mISO));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mISO:" + mISO);
			}
			if (mTextShutter.isFocused()) {
				mTv = mMeter.getNextShutter(mTv);
				mTextShutter.setText(printShutterValue(mTv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mTv:" + mTv);
			}
			if (mTextAperture.isFocused()) {
				mFv = mMeter.getNextAperture(mFv);
				mTextAperture.setText(String.valueOf(mFv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mFv:" + mFv);
			}
			break;
		case R.id.main_button_down:
			if (DEBUG) Log.v(TAG, "ActivityMain::onClick BUTTON_DOWN");
			if (mTextIso.isFocused()) {
				mISO = mMeter.getPreviousISO();
				mTextIso.setText(String.valueOf(mISO));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mISO:" + mISO);
			}
			if (mTextShutter.isFocused()) {
				mTv = mMeter.getPreviousShutter(mTv);
				mTextShutter.setText(printShutterValue(mTv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mTv:" + mTv);
			}
			if (mTextAperture.isFocused()) {
				mFv = mMeter.getPreviousAperture(mFv);
				mTextAperture.setText(String.valueOf(mFv));
				if (DEBUG) Log.v(TAG, "ActivityMain::onClick mFv:" + mFv);
			}
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
	protected void onSaveInstanceState(Bundle outState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onSaveInstanceState");
		outState.putDouble("LUX", mMeter.getLux());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityMain::onRestoreInstanceState");
		mMeter.setLux(savedInstanceState.getDouble("LUX"));
		mTextLux.setText(String.format("%.2f", mMeter.getLux()));
		mTextEv.setText(String.format("%.2f", mMeter.getEv()));
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
	
	private void clearFocus() {
		if (mTextIso.isFocused()) mTextIso.clearFocus();
		if (mTextAperture.isFocused()) mTextAperture.clearFocus();
		if (mTextShutter.isFocused()) mTextShutter.clearFocus();
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
	
	private class RaxPurchaseObserver extends PurchaseObserver {
		
		public RaxPurchaseObserver(Handler handler) {
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
