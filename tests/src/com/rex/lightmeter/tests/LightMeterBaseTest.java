/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rex.lightmeter.tests;

import android.test.InstrumentationTestCase;

import com.rex.lightmeter.LightMeter;
import com.rex.lightmeter.LightMeter.STEP;

public class LightMeterBaseTest extends InstrumentationTestCase {

	//private AssetManager mLocalAssets;
	//private AssetManager mTargetAssets;
	
	private LightMeter mMeter;
	
	protected void setUp() throws Exception {
		super.setUp();
		//mLocalAssets = getInstrumentation().getContext().getAssets();
		//mTargetAssets = getInstrumentation().getTargetContext().getAssets();
		
		mMeter = new LightMeter();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCaculateEv() throws Exception {
		assertEquals(-6d, mMeter.setLux(Math.pow(2, -6) * 2.5));
		assertEquals(0d, mMeter.setLux(Math.pow(2, 0) * 2.5));
		assertEquals(10d, mMeter.setLux(Math.pow(2, 10) * 2.5));
		assertEquals(3d, mMeter.setLux(Math.pow(2, 3) * 2.5));
	}
	
	public void testResetISO() throws Exception {
		mMeter.setISO(200);
		assertEquals(200, mMeter.resetISO());
		
		mMeter.setISO(250);
		assertEquals(200, mMeter.resetISO());
		
		mMeter.setISO(300);
		assertEquals(200, mMeter.resetISO());
		
		mMeter.setISO(320);
		assertEquals(200, mMeter.resetISO());
	}
	
	public void testGetNextISO() throws Exception {
		mMeter.setISO(200);
		mMeter.setStep(STEP.THIRD);
		
		assertEquals(250, mMeter.getNextISO());
		assertEquals(320, mMeter.getNextISO());
		assertEquals(400, mMeter.getNextISO());
	}
	
	public void testGetPriviousISO() throws Exception {
		mMeter.setISO(200);
		mMeter.setStep(STEP.FULL);
		assertEquals(100, mMeter.getPreviousISO());
		
		mMeter.setStep(STEP.HALF);
		assertEquals(75, mMeter.getPreviousISO());
		
		mMeter.setStep(STEP.FULL);
		assertEquals(50, mMeter.getPreviousISO());
	}
	
	public void testResetAperture() throws Exception {
		assertEquals(5.6d, mMeter.resetAperture(5.6d));
		assertEquals(5.6d, mMeter.resetAperture(6.3d));
		assertEquals(5.6d, mMeter.resetAperture(6.7d));
		assertEquals(5.6d, mMeter.resetAperture(7.1d));
	}
	
	public void testGetNextAperture() throws Exception {
		mMeter.setStep(STEP.THIRD);
		assertEquals(9d, mMeter.getNextAperture(8d));
		assertEquals(10d, mMeter.getNextAperture(9d));
		assertEquals(11d, mMeter.getNextAperture(10d));
		
		// F1 - 1/2 EV and F1 - 2/3 EV are both F1.2
		assertEquals(1.1d, mMeter.getNextAperture(1.0d));
		assertEquals(1.2d, mMeter.getNextAperture(1.1d));
		assertEquals(1.4d, mMeter.getNextAperture(1.2d));
	}
	
	public void testGetPriviousAperture() throws Exception {
		mMeter.setStep(STEP.FULL);
		assertEquals(4d, mMeter.getPreviousAperture(5.6d));
		assertEquals(1d, mMeter.getPreviousAperture(1d));
		
		mMeter.setStep(STEP.HALF);
		assertEquals(4.8d, mMeter.getPreviousAperture(5.6d));
	}
	
	public void testResetShutter() throws Exception {
		assertEquals(60d, mMeter.resetShutter(60));
		assertEquals(60d, mMeter.resetShutter(80));
		assertEquals(60d, mMeter.resetShutter(100));
		assertEquals(-2000d, mMeter.resetShutter(-1500));
	}
	
	public void testGetNextShutter() throws Exception {
		mMeter.setStep(STEP.THIRD);
		assertEquals(80d, mMeter.getNextShutter(60));
		assertEquals(100d, mMeter.getNextShutter(80));
		assertEquals(120d, mMeter.getNextShutter(100));
	}
	
	public void testGetPriviousShutter() throws Exception {
		mMeter.setStep(STEP.FULL);
		assertEquals(30d, mMeter.getPreviousShutter(60));
		assertEquals(15d, mMeter.getPreviousShutter(30));
		
		mMeter.setStep(STEP.HALF);
		assertEquals(-8000d, mMeter.getPreviousShutter(-6000));
	}
	
	public void testGetMatchAperture() throws Exception {
		assertEquals(8d, mMeter.getMatchAperture(7.9d));
		assertEquals(8d, mMeter.getMatchAperture(8.1d));
		
		assertEquals(1d, mMeter.getMatchAperture(0.9d));
		assertEquals(0d, mMeter.getMatchAperture(0.5d));	// Overflow
	}
	
	public void testGetMatchShutter() throws Exception {
		assertEquals(-60d, mMeter.getMatchShutter(-60d));
		assertEquals(-60d, mMeter.getMatchShutter(-65d));
		assertEquals(-80d, mMeter.getMatchShutter(-75d));
		
		assertEquals(-8000d, mMeter.getMatchShutter(-9000d));
		assertEquals(0d, mMeter.getMatchShutter(60 * 4096 * 3 / 2));
	}
}
