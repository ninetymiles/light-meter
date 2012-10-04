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
	private int mStepValue = 2;	// Default use 1/3 EV for step
	
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
	
	public void setStep(STEP step) {
		switch (step) {
		case FULL: mStepValue = 6; break;
		case HALF: mStepValue = 3; break;
		case THIRD: mStepValue = 2; break;
		}
	}
	
	// Return 0 means invalid
	public double getShutterByAperture(double N) {
		if (DEBUG) Log.v(TAG, "LightMeter::getShutterByAperture N:" + N + " mEv:" + mEv);
		double t = (N * N * 250) / (mLux * mISO);
		//double t = (N * N) / Math.pow(2, mEv);
		if (DEBUG) Log.v(TAG, "LightMeter::getShutterByAperture t:" + t);
		return getMatchShutter(t);
	}
	
	// Return 0 for invalid result
	public double getApertureByShutter(double t) {
		if (DEBUG) Log.v(TAG, "LightMeter::getApertureByShutter t:" + t);
		double T = (t < 0) ? -1 / t : t;
		double N = Math.sqrt(mLux * mISO * T / 250f);
		if (DEBUG) Log.v(TAG, "LightMeter::getApertureByShutter N:" + N);
		return getMatchAperture(N);
	}
	
	public double getMatchAperture(double value) {
		double matched = 0;
		if (MIN_APERTURE_VALUE <= value && value <= MAX_APERTURE_VALUE) {
			matched = getMatchFromArray(value, sApertureIndex);
			if (DEBUG) Log.v(TAG, "LightMeter::getMatchAperture matched:" + matched);
		}
		return matched;
	}
	
	public double getMatchShutter(double value) {
		double matched = 0;
		if (MIN_SHUTTER_VALUE <= value && value <= MAX_SHUTTER_VALUE) {
			matched = getMatchFromArray(value, sShutterIndex);
			if (DEBUG) Log.v(TAG, "LightMeter::getMatchShutter matched:" + matched);
		}
		return matched;
	}
	
	protected double getMatchFromArray(double value, double [] arr) {
		if (DEBUG) Log.v(TAG, "LightMeter::getMatchFromArray value:" + String.format("%.6f", value));
		double v = 0;
		double diff = Double.MAX_VALUE;
		double matched = 0;
		value = (value < 0) ? -1 / value : value;
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
	
	public int getNextISO() {
		int value = mISO;
		int index = findIndexByValue(mISO, sISOIndex);
		if (index != -1) {
			try {
				value = sISOIndex[index + mStepValue];
				if (value == 0) value = mISO;
			} catch(ArrayIndexOutOfBoundsException ex) {
				value = resetISO();
			}
			mISO = value;
		}
		return value;
	}
	
	public int getPreviousISO() {
		int value = mISO;
		int index = findIndexByValue(mISO, sISOIndex);
		if (index != -1) {
			try {
				value = sISOIndex[index - mStepValue];
				if (value == 0) value = mISO;
			} catch(ArrayIndexOutOfBoundsException ex) {
				value = resetISO();
			}
			mISO = value;
		}
		return value;
	}
	
	public double getNextAperture(double currentValue) {
		double value = currentValue;
		int index = findIndexByValue(currentValue, sApertureIndex, mStepValue);
		if (index != -1) {
			try {
				value = sApertureIndex[index + mStepValue];
				if (value == 0) value = currentValue;
			} catch(ArrayIndexOutOfBoundsException ex) {
				value = resetAperture(currentValue);
			}
		}
		return value;
	}
	
	public double getPreviousAperture(double currentValue) {
		double value = currentValue;
		int index = findIndexByValue(currentValue, sApertureIndex, mStepValue);
		if (index != -1) {
			try {
				value = sApertureIndex[index - mStepValue];
				if (value == 0) value = currentValue;
			} catch(ArrayIndexOutOfBoundsException ex) {
				value = resetAperture(currentValue);
			}
		}
		return value;
	}
	
	public double getNextShutter(double currentValue) {
		double value = currentValue;
		int index = findIndexByValue(currentValue, sShutterIndex, mStepValue);
		if (index != -1) {
			try {
				value = sShutterIndex[index + mStepValue];
				if (value == 0) value = currentValue;
			} catch(ArrayIndexOutOfBoundsException ex) {
				value = resetAperture(currentValue);
			}
		}
		return value;
	}
	
	public double getPreviousShutter(double currentValue) {
		double value = currentValue;
		int index = findIndexByValue(currentValue, sShutterIndex, mStepValue);
		if (index != -1) {
			try {
				value = sShutterIndex[index - mStepValue];
				if (value == 0) value = currentValue;
			} catch(ArrayIndexOutOfBoundsException ex) {
				value = resetAperture(currentValue);
			}
		}
		return value;
	}
	
	public int resetISO() {
		int value = mISO;
		int index = findIndexByValue(mISO, sISOIndex);
		
		if (index != -1) {
			index = index / 6 * 6;
			value = sISOIndex[index];
			mISO = value;
		}
		
		return value;
	}
	
	public double resetAperture(double currentValue) {
		double value = currentValue;
		int index = findIndexByValue(currentValue, sApertureIndex, 1);
		
		if (index != -1) {
			index = index / 6 * 6;
			value = sApertureIndex[index];
		}
		return value;
	}
	
	public double resetShutter(double currentValue) {
		double value = currentValue;
		int index = findIndexByValue(currentValue, sShutterIndex, 1);
		
		if (index != -1) {
			index = index / 6 * 6;
			value = sShutterIndex[index];
		}
		return value;
	}
	
	private int findIndexByValue(int value, int[] array) {
		int index = -1;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == value) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	private int findIndexByValue(double value, double[] array, int step) {
		int index = -1;
		for (int i = 0; i < array.length; i += step) {
			if (array[i] == value) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	private static final double MIN_APERTURE_VALUE = 0.9d;
	private static final double MAX_APERTURE_VALUE = 64 + 64 / 3;	// Add 1/3 EV for detect overflow

	//	0		1/6	2/6		3/6		4/6		5/6
	//	EV		N/A	+1/3EV	+1/2EV	+2/3EV	N/A
	private static final double[] sApertureIndex = {
		1d,		0,	1.1d,	1.2d,	1.2d,	0,
		1.4d,	0,	1.6d,	1.7d,	1.8d,	0,
		2d,		0,	2.2d,	2.4d,	2.5d,	0,
		2.8d,	0,	3.2d,	3.4d,	3.5d,	0,
		4d,		0,	4.5d,	4.8d,	5d,		0,
		5.6d,	0,	6.3d,	6.7d,	7.1d,	0,
		8d,		0,	9d,		9.5d,	10d,	0,
		11d,	0,	13d,	13d,	14d,	0,
		16d,	0,	18d,	19d,	20d,	0,
		22d,	0,	25d,	27d,	28d,	0,
		32d,	0,	36d,	38d,	40d,	0,
		45d,	0,	51d,	54d,	57d,	0,
		64d,
	};

	private static final double MIN_SHUTTER_VALUE = -8000 * 4 / 3;
	private static final double MAX_SHUTTER_VALUE = 60 * 4096 * 4 / 3;
	
	// Shutter values in second, positive value means seconds, negative value means 1/ seconds
	private static final double[] sShutterIndex = {
		-8000,		0,	-6400,		-6000,		-5000,		0,
		-4000,		0,	-3200,		-3000,		-2500,		0,
		-2000,		0,	-1600,		-1500,		-1250,		0,
		-1000,		0,	-800,		-750,		-640,		0,
		-500,		0,	-400,		-350,		-320,		0,
		-250,		0,	-200,		-180,		-160, 		0,
		-125,		0,	-100,		-90,		-80, 		0,
		-60,		0,	-50,		-45,		-40,		0,
		-30,		0,	-25,		-20,		-20,		0,
		-15,		0,	-13,		-10,		-10,		0,
		-8,			0,	-6,			-6,			-5,			0,
		-4,			0,	-3,			-3,			-2.5,		0,
		-2,			0,	-1.6,		-1.5,		-1.3,		0,
		1,			0,	1.3,		1.5,		1.6,		0,
		2,			0,	2.5,		3,			3,			0,
		4,			0,	5,			6,			6,			0,
		8,			0,	10,			10,			13,			0,
		15,			0,	20,			20,			25,			0,
		30,			0,	40,			0,			50,			0,
		60,			0,	80,			0,			100,		0,
		60 * 2,		0,	60 * 2.5,	0,			60 * 3,		0,
		60 * 4,		0,	60 * 5,		0,			60 * 6,		0,
		60 * 8,		0,	60 * 10,	0,			60 * 13,	0,
		60 * 16,	0,	60 * 20,	0,			60 * 25,	0,
		60 * 32,	0,	60 * 40,	0,			60 * 50,	0,
		60 * 64,	0,	60 * 80,	0,			60 * 100,	0,
		60 * 128,	0,	60 * 160,	0,			60 * 200,	0,
		60 * 256,	0,	60 * 320,	0,			60 * 400,	0,
		60 * 512,	0,	60 * 640,	0,			60 * 800,	0,
		60 * 1024,	0,	60 * 1280,	0,			60 * 1600,	0,
		60 * 2048,	0,	60 * 2560,	0,			60 * 3200,	0,
		60 * 4096,
	};
	
	//	0		1/6		2/6		3/6		4/6		5/6
	//	EV		N/A		+1/3EV	+1/2EV	+2/3EV	N/A
	private static final int[] sISOIndex = {
		50,		0,		64,		75,		80,		0,
		100,	0,		125,	150,	160,	0,
		200,	0,		250,	300,	320,	0,
		400,	0,		500,	600,	640,	0,
		800,	0,		1000,	1200,	1250,	0,
		1600,	0,		2000,	2400,	2500,	0,
		3200,	0,		4000,	4800,	5000,	0,
		6400,	0,		8000,	9600,	10000,	0,
		12800,	0,		16000,	19200,	20000,	0,
		25600,	0,		32000,	38400,	40000,	0,
		51200,	0,		64000,	76800,	80000,	0,
		102400,
	};
}
