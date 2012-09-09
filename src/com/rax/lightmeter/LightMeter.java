package com.rax.lightmeter;


public class LightMeter {

	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;
	
	private int mISO;
	private int mAperture;
	private int mShutter;
	
	private float mLux;
	private double mEv;
	
	private static final double sLog2 = Math.log(2);
	
	public double caculateEv(float lux) {
		// if (DEBUG) Log.v(TAG, "LightMeter::caculateEv lux:" + lux);
		
		// Caculate EV from Lux
		// Lux = 250 * 2^EV
		
		mLux = lux;
		mEv = Math.log((mLux / 250)) / sLog2;
		return mEv;
	}
	
	public float getLux() {
		return mLux;
	}
	
	public double getEv() {
		return mEv;
	}
}
