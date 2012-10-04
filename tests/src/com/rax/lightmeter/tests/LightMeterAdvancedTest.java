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

package com.rax.lightmeter.tests;

import android.test.InstrumentationTestCase;

import com.rax.lightmeter.LightMeter;

public class LightMeterAdvancedTest extends InstrumentationTestCase {

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
	
	public void testGetTByFv() throws Exception {
		mMeter.setLux(1 * 2.5);
		assertEquals(1d, mMeter.getTByFv(1.0d));
		assertEquals(8d, mMeter.getTByFv(2.8d));
		assertEquals(30d, mMeter.getTByFv(5.6d));
		assertEquals(60d, mMeter.getTByFv(8d));
		assertEquals(64 * 60d, mMeter.getTByFv(64d));
		assertEquals(0d, mMeter.getTByFv(0d));			// Out of range
		assertEquals(0d, mMeter.getTByFv(128d));		// Out of range
		
		mMeter.setLux(Math.pow(2, 3) * 2.5);
		assertEquals(1d, mMeter.getTByFv(2.0d));
		assertEquals(4d, mMeter.getTByFv(5.6d));
		
		mMeter.setLux(1024 * 2.5);
		assertEquals(-1000d, mMeter.getTByFv(1.0d));
		assertEquals(-30d, mMeter.getTByFv(5.6d));
		assertEquals(4d, mMeter.getTByFv(64d));
		
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(0d, mMeter.getTByFv(1.0d));
		assertEquals(0d, mMeter.getTByFv(1.4d));
		assertEquals(-8000d, mMeter.getTByFv(2.0d));
		assertEquals(-8d, mMeter.getTByFv(64d));
	}
	
	public void testGetFvByT() throws Exception {
		mMeter.setLux(1 * 2.5);
		assertEquals(1.0d, mMeter.getFvByT(1));
		assertEquals(2.8d, mMeter.getFvByT(8));
		assertEquals(5.6d, mMeter.getFvByT(30));
		assertEquals(8d, mMeter.getFvByT(60));
		assertEquals(64d, mMeter.getFvByT(64 * 60));
		assertEquals(0d, mMeter.getFvByT(0));		// Invalid value
		assertEquals(0d, mMeter.getFvByT(128));		// Out of range
		
		mMeter.setLux(1024 * 2.5);
		assertEquals(1.0d, mMeter.getFvByT(-1000));
		assertEquals(5.6d, mMeter.getFvByT(-30));
		assertEquals(64d, mMeter.getFvByT(4));
		
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(0d, mMeter.getFvByT(-16000));
		assertEquals(2.0d, mMeter.getFvByT(-8000));
		assertEquals(64d, mMeter.getFvByT(-8));
	}
	
	public void testISO() throws Exception {
		mMeter.setISO(200);
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(-15d, mMeter.getTByFv(64d));	// ISO200, can use 1/15 rather than 1/8 for shutter speed
		
		mMeter.setISO(50);
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(-4d, mMeter.getTByFv(64d));	// ISO50, need slow down to 1/4
		
		mMeter.setISO(200);
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(32d, mMeter.getFvByT(-60));
		assertEquals(45d, mMeter.getFvByT(-30));
		assertEquals(64d, mMeter.getFvByT(-15));
		assertEquals(0d, mMeter.getFvByT(-8));		// ISO100 15EV will use 1/8 F64, ISO200 will out of range
		assertEquals(11d, mMeter.getFvByT(-500));	// ISO100 15EV will use 1/500 F8, ISO200 can use F11 rather than F8
		
		mMeter.setISO(50);
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(5.6d, mMeter.getFvByT(-500));	// Use ISO50, need set aperture to F5.6
		
	}
}