package com.rex.lightmeter;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class FragmentIncident extends Fragment implements OnFocusChangeListener {

    private final Logger mLogger = LoggerFactory.getLogger("RexLog");

    private final String PREFS_FV = "PREFS_FV";
    private final String PREFS_TV = "PREFS_TV";
    private final String PREFS_ISO = "PREFS_ISO";
    private final String PREFS_MODE = "PREFS_MODE";

    private static enum Mode { UNDEFINED, TV_FIRST, FV_FIRST };

    private TextView mTextLux;
    private TextView mTextEv;
    private Spinner mSpinnerIso;
    private Spinner mSpinnerAperture;
    private Spinner mSpinnerShutter;
    private TextView mTextCompensationLabel;
    private TextView mTextCompensationValue;

    private Button mBtnMeasure;

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
    private boolean mIsEnableRecordMaxValue = false;
    private LightMeter.STOP mEvStop = LightMeter.STOP.THIRD;
    private double mFv = 8f;
    private double mTv = -60;
    private int mISO = 200;
    private double mCompensation = 0f;
    private Mode mMode = Mode.UNDEFINED;

    private float mMaxLux;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLogger.trace("");
        View fragView = inflater.inflate(R.layout.fragment_incident, container, false);

        mOrientation = new OrientationHelper(getActivity());

        mTextLux = (TextView) fragView.findViewById(R.id.main_lux_value);
        mTextEv = (TextView) fragView.findViewById(R.id.main_ev_value);

        mSpinnerIso = (Spinner) fragView.findViewById(R.id.main_iso_value);
        mSpinnerIso.setOnItemSelectedListener(mSpinnerSelectedListener);
        mSpinnerIso.setFocusableInTouchMode(true);

        mSpinnerAperture = (Spinner) fragView.findViewById(R.id.main_aperture_value);
        mSpinnerAperture.setOnItemSelectedListener(mSpinnerSelectedListener);
        mSpinnerAperture.setOnFocusChangeListener(this);
        mSpinnerAperture.setFocusableInTouchMode(true);
        mSpinnerShutter = (Spinner) fragView.findViewById(R.id.main_shutter_value);
        mSpinnerShutter.setOnItemSelectedListener(mSpinnerSelectedListener);
        mSpinnerShutter.setOnFocusChangeListener(this);
        mSpinnerShutter.setFocusableInTouchMode(true);

        mTextCompensationLabel = (TextView) fragView.findViewById(R.id.main_compensation_label);
        mTextCompensationValue = (TextView) fragView.findViewById(R.id.main_compensation_value);

        mBtnMeasure = (Button) fragView.findViewById(R.id.main_button);
        mBtnMeasure.setOnTouchListener(mTouchListener);
        mBtnMeasure.setOnClickListener(mClickListener);
        mBtnMeasure.setOnLongClickListener(mLongClickListener);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mMeter = new LightMeter();
        mMeter.setISO(200);

        return fragView;
    }

    private String printApertureValue(double aperture) {
        //mLogger.trace("aperture:{}", aperture);
        String str = "";
        DecimalFormat df = new DecimalFormat("0.#");
        if (aperture == LightMeter.MIN_APERTURE_VALUE) {
            str = "MIN";
        } else if (aperture == LightMeter.MAX_APERTURE_VALUE) {
            str = "MAX";
        } else {
            str = df.format(aperture);
        }
        return str;
    }

    private String printShutterValue(double shutter) {
        //mLogger.trace("shutter:{}", shutter);
        String str = "";
        DecimalFormat df = new DecimalFormat("0.#");
        if (shutter == LightMeter.MIN_SHUTTER_VALUE) {
            str = "MIN";
        } else if (shutter == LightMeter.MAX_SHUTTER_VALUE) {
            str = "MAX";
        } else if (shutter < 0) {
            str = "1/" + df.format(Math.abs(shutter));
        } else if (shutter > 60) {
            int sec = (int) shutter % 60;
            int min = (int) shutter / 60 % 60;
            int hour = (int) shutter / 3600;
            if (hour > 0) {
                str += hour + "h ";
            }
            if (min > 0) {
                str += min + "m ";
            }
            if (sec > 0) {
                str += sec + "\"";
            }
        } else {
            str = df.format(shutter) + "\"";
        }
        return str.trim();
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogger.trace("");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit()
                .putFloat(PREFS_FV, (float) mFv)
                .putFloat(PREFS_TV, (float) mTv)
                .putInt(PREFS_ISO, mISO)
                .putInt(PREFS_MODE, mMode.ordinal())
                .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        mLogger.trace("");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIsEnableVolumeKey = prefs.getBoolean("CONF_ENABLE_VOLUME_KEY", true);
        mIsEnableRecordMaxValue = prefs.getBoolean("CONF_ENABLE_RECORD_MAX_VALUE", false);
        mEvStop = LightMeter.STOP.values()[Integer.valueOf(prefs.getString("CONF_EV_STEP", "2"))];
        mMeter.setStop(mEvStop);
        mFv = mMeter.getMatchAperture(prefs.getFloat(PREFS_FV, 8f));
        mTv = prefs.getFloat(PREFS_TV, -60);
        mTv = mMeter.getMatchShutter((mTv < 0) ? (-1 / mTv) : mTv);
        mISO = prefs.getInt(PREFS_ISO, 200);
        mMode = Mode.values()[prefs.getInt(PREFS_MODE, 2)];
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
        adapterISO = new ArrayAdapter<MainSpinnerItem>(getActivity(), android.R.layout.simple_spinner_item, mISOValue);
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
        adapterAperture = new ArrayAdapter<MainSpinnerItem>(getActivity(), android.R.layout.simple_spinner_item, mApertureValue);
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
        adapterShutter = new ArrayAdapter<MainSpinnerItem>(getActivity(), android.R.layout.simple_spinner_item, mShutterValue);
        adapterShutter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinnerShutter.setAdapter(adapterShutter);
        mSpinnerShutter.setSelection(position);

        // TODO: Will use image icon instead
        String compensation = prefs.getString("CONF_EXPOSURE_COMPENSATION", "0");
        int comp = 0;
        try {
            comp = Integer.parseInt(compensation);
        } catch (Exception ex) {}
        mCompensation = comp / 3.0f;
        mMeter.setCompensation(mCompensation);

        if ("0".equals(compensation)) {
            //mTextCompensationLabel.setVisibility(View.INVISIBLE);
            mTextCompensationValue.setVisibility(View.INVISIBLE);
        } else {
            String compValue = String.format(Locale.US, "%s %.1f EV", mCompensation > 0 ? "+" : "-", Math.abs(mCompensation));
            //mTextCompensationLabel.setVisibility(View.VISIBLE);
            mTextCompensationValue.setVisibility(View.VISIBLE);
            mTextCompensationValue.setText(compValue);
        }

        mLogger.trace("mIsEnableVolumeKey:{} mEvStop:{} mMode:{} mISO:{} mFv:{} mTv:{}", mIsEnableVolumeKey, mEvStop, mMode, mISO, mFv, mTv);
        if (prefs.getBoolean("CONF_ENABLE_KEEP_SCREEN_ORIENTATION", true)) {
            mOrientation.lock();
        } else {
            mOrientation.unlock();
        }
    }

    private void setMode(Mode mode) {
        mLogger.trace("mode:{}", mode);
        mMode = mode;
        getView().findViewById(R.id.main_aperture_mode).setSelected(mMode == Mode.FV_FIRST);
        getView().findViewById(R.id.main_shutter_mode).setSelected(mMode == Mode.TV_FIRST);
    }

    private void updateEv() {
        mLogger.trace("mMode:{} mFv:{} mTv:{}", mMode, mFv, mTv);
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
        mLogger.trace("shutter:{} aperture:{}", shutter, aperture);
        mSpinnerAperture.setSelection(mArrAperture.indexOf(aperture));
        mSpinnerShutter.setSelection(mArrShutter.indexOf(shutter));
        mTv = shutter;
        mFv = aperture;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        mLogger.trace("id:{} hasFocus:{}", v.getId(), hasFocus);
        int position = 0;
        if (hasFocus) {
            switch (v.getId()) {
            case R.id.main_aperture_value:
                setMode(Mode.FV_FIRST);
                position = mSpinnerAperture.getSelectedItemPosition();
                if (position != AdapterView.INVALID_POSITION) {
                    if (position < 1) position = 1;
                    else if (position >= mArrAperture.size() - 1) position = mArrAperture.size() - 2;
                    mSpinnerAperture.setSelection(position);
                }
                break;
            case R.id.main_shutter_value:
                setMode(Mode.TV_FIRST);
                position = mSpinnerShutter.getSelectedItemPosition();
                if (position != AdapterView.INVALID_POSITION) {
                    if (position < 1) position = 1;
                    else if (position >= mArrShutter.size() - 1) position = mArrShutter.size() - 2;
                    mSpinnerShutter.setSelection(position);
                }
                break;
            }
        }
    }

//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		mLogger.trace("keyCode:{}", keyCode);
//		boolean handled = false;
//		if (mIsEnableVolumeKey) {
//			switch (keyCode) {
//			case KeyEvent.KEYCODE_VOLUME_DOWN:
//				if (mSpinnerIso.isFocused()) {
//					int position = mSpinnerIso.getSelectedItemPosition();
//					if (position != AdapterView.INVALID_POSITION) {
//						position--;
//						if (position < 0) position = 0;
//						mSpinnerIso.setSelection(position);
//					}
//				} else if (mSpinnerAperture.isFocused() || mMode == Mode.FV_FIRST) {
//					int position = mSpinnerAperture.getSelectedItemPosition();
//					if (position != AdapterView.INVALID_POSITION) {
//						position--;
//						if (position < 1) position = 1;
//						mSpinnerAperture.setSelection(position);
//					}
//				} else if (mSpinnerShutter.isFocused() || mMode == Mode.TV_FIRST) {
//					int position = mSpinnerShutter.getSelectedItemPosition();
//					if (position != AdapterView.INVALID_POSITION) {
//						position--;
//						if (position < 1) position = 1;
//						mSpinnerShutter.setSelection(position);
//					}
//				}
//				handled = true;
//				break;
//			case KeyEvent.KEYCODE_VOLUME_UP:
//				if (mSpinnerIso.isFocused()) {
//					int position = mSpinnerIso.getSelectedItemPosition();
//					if (position != AdapterView.INVALID_POSITION) {
//						position++;
//						if (position >= mArrISO.size()) position = mArrISO.size() - 1;
//						mSpinnerIso.setSelection(position);
//					}
//				} else if (mSpinnerAperture.isFocused() || mMode == Mode.FV_FIRST) {
//					int position = mSpinnerAperture.getSelectedItemPosition();
//					if (position != AdapterView.INVALID_POSITION) {
//						position++;
//						if (position >= mArrAperture.size() - 1) position = mArrAperture.size() - 2;
//						mSpinnerAperture.setSelection(position);
//					}
//				} else if (mSpinnerShutter.isFocused() || mMode == Mode.TV_FIRST) {
//					int position = mSpinnerShutter.getSelectedItemPosition();
//					if (position != AdapterView.INVALID_POSITION) {
//						position++;
//						if (position >= mArrShutter.size() - 1) position = mArrShutter.size() - 2;
//						mSpinnerShutter.setSelection(position);
//					}
//				}
//				handled = true;
//				break;
//			}
//		}
//		return handled || super.onKeyDown(keyCode, event);
//	}

//	@Override
//	public void onSaveInstanceState(Bundle outState) {
//		mLogger.trace("");
//		outState.putDouble("LUX", mMeter.getLux());
//		super.onSaveInstanceState(outState);
//	}

//	@Override
//	public void onRestoreInstanceState(Bundle savedInstanceState) {
//		mLogger.trace("");
//		mMeter.setLux(savedInstanceState.getDouble("LUX"));
//		mTextLux.setText(String.format("%.2f", mMeter.getLux()));
//		mTextEv.setText(String.format("%.2f", mMeter.getEv()));
//		super.onRestoreInstanceState(savedInstanceState);
//	}

    // FIXME: Should remove this
    private void clearFocus() {
        if (mSpinnerIso.isFocused()) mSpinnerIso.clearFocus();
        if (mSpinnerAperture.isFocused()) mSpinnerAperture.clearFocus();
        if (mSpinnerShutter.isFocused()) mSpinnerShutter.clearFocus();
    }

    private void doStartMeasure() {
        mLogger.trace("");
        mSensorManager.registerListener(mSensorListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
        if (false == PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("CONF_ENABLE_KEEP_SCREEN_ORIENTATION", true)) {
            mOrientation.lock();
        }
    }

    private void doStopMeasure() {
        mLogger.trace("");
        mSensorManager.unregisterListener(mSensorListener, mLightSensor);
        if (false == PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("CONF_ENABLE_KEEP_SCREEN_ORIENTATION", true)) {
            mOrientation.unlock();
        }
    }

    // TODO: Enable the reflect mode if user do have a camera
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private OnItemSelectedListener mSpinnerSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
            mLogger.trace("parentView:{} position:{} id:{}", parentView.getId(), position, id);
            switch (parentView.getId()) {
            case R.id.main_iso_value:
                mLogger.trace("ISO:", mISOValue[position]);
                mISO = (Integer) mISOValue[position].getValue();
                mMeter.setISO(mISO);
                break;
            case R.id.main_aperture_value:
                mLogger.trace("Aperture:{}", mApertureValue[position]);
                mFv = (Double) mApertureValue[position].getValue();
                break;
            case R.id.main_shutter_value:
                mLogger.trace("Shutter:{}", mShutterValue[position]);
                mTv = (Double) mShutterValue[position].getValue();
                break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
            //mLogger.trace("");
        }
    };

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //mLogger.trace("");
        }
    };

    private OnLongClickListener mLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            //mLogger.trace("");
            return true;
        }
    };

    private OnTouchListener mTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //mLogger.trace("id:{} action:{}", v.getId(), event.getAction());
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMaxLux = 0;
                doStartMeasure();
                clearFocus();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                doStopMeasure();
                break;
            }
            return false;
        }
    };

    private SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            mLogger.trace("accuracy:{}", accuracy);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //mLogger.trace("");
            float lux = event.values[0];
            if (mIsEnableRecordMaxValue && lux <= mMaxLux) return;
            mMaxLux = lux;
            double ev = mMeter.setLux(lux);
            mTextLux.setText(String.format(Locale.US, "%.2f", lux));
            mTextEv.setText(String.format(Locale.US, "%.2f", ev));
            updateEv();
        }
    };
}
