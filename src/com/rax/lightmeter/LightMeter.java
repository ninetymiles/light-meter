package com.rax.lightmeter;

import android.util.Log;



public class LightMeter {

	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;
	
	private int mISO = 100;
	private int mShutter = -60;
	private float mAperture = 5.6f;
	
	private double mLux;
	private double mEv;
	
	private static final double sLog2 = Math.log(2);
	
	public double caculateEv(double lux) {
		// if (DEBUG) Log.v(TAG, "LightMeter::caculateEv lux:" + lux);
		
		// Caculate EV from Lux
		// Lux = 250 * 2^EV
		
		mLux = lux;
		mEv = Math.log((mLux / 250)) / sLog2;
		return mEv;
	}
	
	public void setISO(int iso) {
		mISO = iso;
	}
	
	public double getLux() {
		return mLux;
	}
	
	public double getEv() {
		return mEv;
	}
	
	public int getShutterByFv(float f) {
		if (DEBUG) Log.v(TAG, "LightMeter::getShutterByFv f:" + f);
		int s = 0;
		try {
			int [] e = sExposureValue[getEvIndex((int) Math.round(mEv))];
			s = e[getFvIndex(f)];
		} catch(ArrayIndexOutOfBoundsException ex) {
			if (DEBUG) Log.d(TAG, "LightMeter::getShutterByFv f:" + f + " ex:" + ex.toString());
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		if (DEBUG) Log.v(TAG, "LightMeter::getShutterByFv s:" + s);
		return s;
	}
	
	public float getFvByShutter(int shutter) {
		if (DEBUG) Log.v(TAG, "LightMeter::getFvByShutter s:" + shutter);
		float f = 0;
		try {
			int [] e = sExposureValue[getEvIndex((int) Math.round(mEv))];
			for (int i = 0; i < e.length; i++) {
				if (shutter == e[i]) {
					f = sFvIndex[i];
					break;
				}
			}
		} catch(ArrayIndexOutOfBoundsException ex) {
			if (DEBUG) Log.d(TAG, "LightMeter::getFvByShutter s:" + shutter + " ex:" + ex.toString());
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		if (DEBUG) Log.v(TAG, "LightMeter::getFvByShutter f:" + f);
		return f;
	}
	
	// Protected scope is for unit test
	protected int getFvIndex(float f) {
		int idx = -1;
		for (int i = 0; i < sFvIndex.length; i++) {
			if (f == sFvIndex[i]) {
				idx = i;
				break;
			}
		}
		return idx;
	}
	
	// Protected scope is for unit test
	protected int getEvIndex(int ev) {
		int offset = 6; // -6EV default map to data line 0
		
		// ISO 100 will set offset 0, ISO 200 will set offset 1 
		offset += Math.log(mISO / 100f) / sLog2;
		
		if (DEBUG) Log.v(TAG, "LightMeter::getEvIndex ev:" + ev + " offset:" + offset + " l:" + sExposureValue.length);
		
		int idx = ev + offset;
		if (idx < 0 || idx >= sExposureValue.length) {
			idx = -1;
		}
		return idx;
	}
	
	private static final float [] sFvIndex = { 1, 1.4f, 2, 2.8f, 4, 5.6f, 8, 11, 16, 22, 32, 45, 64};
	
	// Shutter values in second, positive value means seconds, nagitive value means 1/ seconds
	// Reference http://en.wikipedia.org/wiki/Exposure_value
	private static final int [][] sExposureValue = {
		/* -6 EV */ { 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60, 128 * 60, 256 * 60, 512 * 60, 1024 * 60, 2048 * 60, 4096 * 60},
		/* -5 EV */ { 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60, 128 * 60, 256 * 60, 512 * 60, 1024 * 60, 2048 * 60},
		/* -4 EV */ { 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60, 128 * 60, 256 * 60, 512 * 60, 1024 * 60},
		/* -3 EV */ { 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60, 128 * 60, 256 * 60, 512 * 60},
		/* -2 EV */ { 4, 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60, 128 * 60, 256 * 60},
		/* -1 EV */ { 2, 4, 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60, 128 * 60},
		/*  0 EV */ { 1, 2, 4, 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60, 64 * 60},
		/*  1 EV */ { -2, 1, 2, 4, 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60, 32 * 60},
		/*  2 EV */ { -4, -2, 1, 2, 4, 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60, 16 * 60},
		/*  3 EV */ { -8, -4, -2, 1, 2, 4, 8, 15, 30, 60, 2 * 60, 4 * 60, 8 * 60},
		/*  4 EV */ { -15, -8, -4, -2, 1, 2, 4, 8, 15, 30, 60, 2 * 60, 4 * 60},
		/*  5 EV */ { -30, -15, -8, -4, -2, 1, 2, 4, 8, 15, 30, 60, 2 * 60},
		/*  6 EV */ { -60, -30, -15, -8, -4, -2, 1, 2, 4, 8, 15, 30, 60},
		/*  7 EV */ { -125, -60, -30, -15, -8, -4, -2, 1, 2, 4, 8, 15, 30},
		/*  8 EV */ { -250, -125, -60, -30, -15, -8, -4, -2, 1, 2, 4, 8, 15},
		/*  9 EV */ { -500, -250, -125, -60, -30, -15, -8, -4, -2, 1, 2, 4, 8},
		/* 10 EV */ { -1000, -500, -250, -125, -60, -30, -15, -8, -4, -2, 1, 2, 4},
		/* 11 EV */ { -2000, -1000, -500, -250, -125, -60, -30, -15, -8, -4, -2, 1, 2},
		/* 12 EV */ { -4000, -2000, -1000, -500, -250, -125, -60, -30, -15, -8, -4, -2, 1},
		/* 13 EV */ { -8000, -4000, -2000, -1000, -500, -250, -125, -60, -30, -15, -8, -4, -2},
		/* 14 EV */ { 0, -8000, -4000, -2000, -1000, -500, -250, -125, -60, -30, -15, -8, -4},
		/* 15 EV */ { 0, 0, -8000, -4000, -2000, -1000, -500, -250, -125, -60, -30, -15, -8},
		/* 16 EV */ { 0, 0, 0, -8000, -4000, -2000, -1000, -500, -250, -125, -60, -30, -15},
		/* 17 EV */ { 0, 0, 0, 0, -8000, -4000, -2000, -1000, -500, -250, -125, -60, -30},
		/* 18 EV */ { 0, 0, 0, 0, 0, -8000, -4000, -2000, -1000, -500, -250, -125, -60},
		/* 19 EV */ { 0, 0, 0, 0, 0, 0, -8000, -4000, -2000, -1000, -500, -250, -125},
		/* 20 EV */ { 0, 0, 0, 0, 0, 0, 0, -8000, -4000, -2000, -1000, -500, -250},
		/* 21 EV */ { 0, 0, 0, 0, 0, 0, 0, 0, -8000, -4000, -2000, -1000, -500},
	};
}
