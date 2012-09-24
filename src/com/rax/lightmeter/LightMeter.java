package com.rax.lightmeter;

import android.util.Log;


/*
 * Reference en.wikipedia.org/wiki/Exposure_value
 * 
 * EV	: Exposure Value
 * Lux	: Illuminance
 * N	: Aperture (F-number)
 * t	: Exposure time (Shutter speed)
 * 
 * Lux	= (250 / ISO) * 2^EV
 * 2^EV	= Lux / (250 / ISO)
 * EV	= log2(Lux / 2.5)	= log(Lux / 2.5) / log(2)
 * 
 * 2^EV	= N^2 / t
 * t	= N^2 / 2^EV = N^2 / (Lux / (250 / ISO))
 * N	= sqrt(2^EV * t) = sqrt((Lux / (250 / ISO)) * t)
 */
public class LightMeter {

	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;
	
	public static enum STEP { FULL, HALF, THIRD };
	
	private int mISO = 100;
	
	private double mLux;
	private double mEv;
	
	private static final double sLog2 = Math.log(2);
	
	public LightMeter() {
		if (DEBUG) Log.v(TAG, "LightMeter::constructor sLog2:" + sLog2);
	}
	
	public double setLux(double lux) {
		mLux = lux;
		mEv = Math.log((mLux * mISO / 250f)) / sLog2;
		if (DEBUG) Log.v(TAG, "LightMeter::caculateEv lux:" + lux + " mEv:" + mEv);
		return mEv;
	}
	
	public void setEv(double ev) {
		mEv = ev;
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
	
	public int getISO() {
		return mISO;
	}
	
	// Return 0 means invalid
	public double getTByFv(double N) {
		double t = 0;
		if (DEBUG) Log.v(TAG, "LightMeter::getTByFv N:" + N + " mEv:" + mEv);
		if (findFv(N)) {
			t = (N * N * 250) / (mLux * mISO);
			//t = (N * N) / Math.pow(2, mEv);
		}
		if (DEBUG) Log.v(TAG, "LightMeter::getTByFv t:" + t);
		return getMatchTv(t);
	}
	
	// Return 0 for invalid result
	public double getFvByT(double t) {
		double N = 0;
		if (DEBUG) Log.v(TAG, "LightMeter::getFvByT t:" + t);
		if (findT(t)) {
			double T = (t < 0) ? -1 / t : t;
			N = Math.sqrt(mLux * mISO * T / 250f);
		}
		if (DEBUG) Log.v(TAG, "LightMeter::getFvByT N:" + N);
		return getMatchFv(N);
	}
	
	public double getMatchTv(double value) {
		double matched = getMatchFromArray(value, sTvIndex3);
		if (matched == MAX_TV) matched = 0;
		else {
			if (DEBUG) Log.v(TAG, "LightMeter::getMatchTv matched:" + matched);
		}
		return matched;
	}
	
	public double getMatchFv(double value) {
		double matched = getMatchFromArray(value, sFvIndex3);
		if (matched == MAX_FV) matched = 0;
		else {
			if (DEBUG) Log.v(TAG, "LightMeter::getMatchFv matched:" + matched);
		}
		return matched;
	}
	
	protected double getMatchFromArray(double value, double [] arr) {
		if (DEBUG) Log.v(TAG, "LightMeter::getMatchFromArray value:" + String.format("%.6f", value) + " arr.length:" + arr.length);
		double v = 0;
		double diff = Double.MAX_VALUE;
		double matched = 0;
		for (int i = 0; i < arr.length; i++) {
			v = (arr[i] < 0) ? -1 / arr[i] : arr[i];
			//if (DEBUG) Log.v(TAG, "LightMeter::getMatchFromArray arr[i]:" + arr[i] + " v:" + String.format("%.6f", v) + " diff:" + String.format("%.6f", Math.abs(value - v)));
			if (Math.abs(value - v) < diff) {
				diff = Math.abs(value - v);
				matched = arr[i];
			}
		}
		return matched;
	}
	
	private boolean findFv(double N) {
		boolean matched = false;
		for (double f : sFvIndex3) {
			if (f == N) {
				matched = true;
				break;
			}
		}
		for (double f : sFvIndex2) {
			if (f == N) {
				matched = true;
				break;
			}
		}
		if (matched == false) {
			if (DEBUG) Log.v(TAG, "LightMeter::findFv not found N:" + N);
		}
		return matched;
	}
	
	private boolean findT(double T) {
		boolean matched = false;
		for (double t : sTvIndex3) {
			if (t == T) {
				matched = true;
				break;
			}
		}
		if (matched == false) {
			if (DEBUG) Log.v(TAG, "LightMeter::findT not found T:" + T);
		}
		return matched;
	}
	
	public int getISO(STEP step, boolean dir) {
		int idx = -1;
		int offset = 0;
		switch (step) {
		case FULL: offset = 3 * (dir ? 1 : -1); break;
		case HALF: offset = 2 * (dir ? 1 : -1); break;
		case THIRD: offset = 1 * (dir ? 1 : -1); break;
		}
		for (int i = 0; i < sISOIndex.length; i++) {
			if (sISOIndex[i] == mISO) {
				idx = i + offset;
				if (idx >= sISOIndex.length) idx = sISOIndex.length - 1;
				if (idx < 0) idx = 0;
				break;
			}
		}
		return (idx == -1) ? mISO : sISOIndex[idx];
	}
	
	public double getFv(STEP step, double cur, boolean dir) {
		int idx = -1;
		int offset = 0;
		switch (step) {
		case FULL: offset = 3 * (dir ? 1 : -1); break;
		case HALF: offset = 2 * (dir ? 1 : -1); break;
		case THIRD: offset = 1 * (dir ? 1 : -1); break;
		}
		cur = getMatchFv(cur);
		for (int i = 0; i < sFvIndex3.length; i++) {
			if (sFvIndex3[i] == cur) {
				idx = i + offset;
				if (idx >= sFvIndex3.length - 1) idx = sFvIndex3.length - 2;
				if (idx <= 0) idx = 1;
				break;
			}
		}
		return (idx == -1) ? cur : sFvIndex3[idx];
	}
	
	public double getTv(STEP step, double cur, boolean dir) {
		int idx = -1;
		int offset = 0;
		switch (step) {
		case FULL: offset = 3 * (dir ? 1 : -1); break;
		case HALF: offset = 2 * (dir ? 1 : -1); break;
		case THIRD: offset = 1 * (dir ? 1 : -1); break;
		}
		cur = (cur < 0) ? (-1 / cur) : cur;
		cur = getMatchTv(cur);
		for (int i = 0; i < sTvIndex3.length; i++) {
			if (sTvIndex3[i] == cur) {
				idx = i + offset;
				if (idx >= sTvIndex3.length) idx = sTvIndex3.length - 1;
				if (idx < 0) idx = 0;
				break;
			}
		}
		return (idx == -1) ? cur : sTvIndex3[idx];
	}
	
	private static final double MIN_FV = 0;
	private static final double MAX_FV = 64 + 64 / 3;	// Add 1/3 EV for detect overflow
	//private static final double MAX_FV = 512 + 512 / 3;
	
	// Step 1/3 EV
	private static final double [] sFvIndex3 = {
		MIN_FV,
		1d,		1.1d,	1.2d,
		1.4d,	1.6d,	1.8d,
		2d,		2.2d,	2.5d,
		2.8d,	3.2d,	3.5d,
		4d,		4.5d,	5d,
		5.6d,	6.3d,	7.1d,
		8d,		9d,		10d, 
		11d,	13d,	14d, 
		16d,	18d,	20d, 
		22d,	25d,	28d, 
		32d,	36d,	40d,
		45d,	51d,	57d, 
		64d,//	72d,	80d,
		//90d,	102d,	114d,
		//128d,	144d,	161d,
		//181d,	203d,	228d,
		//256d,	287d,	323d,
		//362d,	407d,	456d,
		//512d,
		MAX_FV
	};
	
	// Step 1/2 EV
	private static final double [] sFvIndex2 = {
		MIN_FV,
		1d,		1.2d,
		1.4d,	1.7d,
		2d,		2.4d,
		2.8d,	3.4d,
		4d,		4.8d,
		5.6d,	6.7d,
		8d,		9.5d,
		11d,	14d,
		16d,	19d,
		22d,	27d,
		32d,	38d,
		45d,	54d,
		64d,//	76d,
		//90d,	108d,
		//128d,	152d,
		//181d,	215d,
		//256d,	304d,
		//362d,	431d,
		//512d,
		MAX_FV
	};
	
	private static final double MIN_TV = -8000 * 1.5;
	private static final double MAX_TV = 60 * 4096 * 2;
	
	private static final double [] sTvIndex3 = {
		MIN_TV,
		-8000,		-6400,		-5000,
		-4000,		-3200,		-2500,
		-2000,		-1600,		-1250,
		-1000,		-800,		-640,
		-500,		-400,		-320,
		-250,		-200,		-160, 
		-125,		-100,		-80, 
		-60,		-50,		-40,
		-30,		-25,		-20,
		-15,		-13,		-10,
		-8,			-6,			-5, 
		-4,			-3,			-2.5,
		-2,			-1.6,		-1.3,
		1,			1.3,		1.6,
		2,			2.5,		3,
		4,			5,			6,
		8,			10,			13, 
		15,			20,			25,
		30,			40,			50,
		60,			80,			100,
		60 * 2,		60 * 2.5,	60 * 3,
		60 * 4,		60 * 5,		60 * 6,
		60 * 8,		60 * 10,	60 * 13,
		60 * 16,	60 * 20,	60 * 25,
		60 * 32,	60 * 40,	60 * 50,
		60 * 64,	60 * 80,	60 * 100,
		60 * 128,	60 * 160,	60 * 200,
		60 * 256,	60 * 320,	60 * 400,
		60 * 512,	60 * 640,	60 * 800,
		60 * 1024,	60 * 1280,	60 * 1600,
		60 * 2048,	60 * 2560,	60 * 3200,
		60 * 4096,
		MAX_TV
	};
	
	private static final double [] sTvIndex2 = {
		MIN_TV,
		-8000,		-6000,
		-4000,		-3000,
		-2000,		-1500,
		-1000,		-750,
		-500,		-320,
		-250,		-160, 
		-125,		-80, 
		-60,		-40,
		-30,		-20,
		-15,		-10,
		-8,			-5, 
		-4,			-2.5,
		-2,			-1.2,
		1,			-0.6,
		2,			3,
		4,			6,
		8,			12, 
		15,			25,
		30,			45,
		60,			60 * 1.5,
		60 * 2,		60 * 3,
		60 * 4,		60 * 6,
		60 * 8,		60 * 12,
		60 * 16,	60 * 24,
		60 * 32,	60 * 48,
		60 * 64,	60 * 96,
		60 * 128,	60 * 192,
		60 * 256,	60 * 320,	60 * 400,
		60 * 512,	60 * 640,	60 * 800,
		60 * 1024,	60 * 1280,	60 * 1600,
		60 * 2048,	60 * 2560,	60 * 3200,
		60 * 4096,
		MAX_TV
	};
	
	private static final int[] sISOIndex = {
		//0.8,	1,		1.2,
		//1.6,	2,		2.5,
		//3,		4,		5,
		//6,		8,		10,
		//12,		16,		20,
		//25,		32,		40, 
		50,		64,		80, 
		100,	125,	160,
		200,	250,	320,
		400,	500,	640,
		800,	1000,	1250,
		1600,	2000,	2500,
		3200,	4000,	5000,
		6400,
		12800,
		25600,
		51200,
		102400,
	};
	
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
