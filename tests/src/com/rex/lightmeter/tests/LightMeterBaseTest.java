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

public class LightMeterBaseTest extends InstrumentationTestCase {
	
	private LightMeter mMeter;
	
	protected void setUp() throws Exception {
		super.setUp();
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
	
	public void testGetMatchAperture() throws Exception {
		assertEquals(8d, mMeter.getMatchAperture(7.9d));
		assertEquals(8d, mMeter.getMatchAperture(8.1d));
		
		assertEquals(1d, mMeter.getMatchAperture(0.9d));
		assertEquals(LightMeter.MIN_APERTURE_VALUE, mMeter.getMatchAperture(0.5d));	// Overflow
	}
	
	public void testGetMatchShutter() throws Exception {
		assertEquals(-60d, mMeter.getMatchShutter(-60d));
		assertEquals(-60d, mMeter.getMatchShutter(-65d));
		assertEquals(-80d, mMeter.getMatchShutter(-75d));
		
		assertEquals(-8000d, mMeter.getMatchShutter(-9000d));
		assertEquals(LightMeter.MAX_SHUTTER_VALUE, mMeter.getMatchShutter(60 * 4096 * 3 / 2));
	}
}
