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

public class LightMeterAdvancedTest extends InstrumentationTestCase {

	private LightMeter mMeter;
	
	protected void setUp() throws Exception {
		super.setUp();
		mMeter = new LightMeter();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGetShutterByAperture() throws Exception {
		mMeter.setLux(1 * 2.5);					// 0 EV
		assertEquals(1d, mMeter.getShutterByAperture(1.0d));
		assertEquals(8d, mMeter.getShutterByAperture(2.8d));
		
		mMeter.setLux(Math.pow(2, 3) * 2.5);	// 3 EV
		assertEquals(1d, mMeter.getShutterByAperture(2.8d));
		
		mMeter.setLux(1024 * 2.5);				// 10 EV
		assertEquals(-1000d, mMeter.getShutterByAperture(1.0d));
		
		mMeter.setLux(Math.pow(2, 15) * 2.5);	// 15 EV
		assertEquals(LightMeter.MIN_SHUTTER_VALUE, mMeter.getShutterByAperture(1.0d));
		assertEquals(LightMeter.MIN_SHUTTER_VALUE, mMeter.getShutterByAperture(1.4d));
		assertEquals(-8000d, mMeter.getShutterByAperture(2.0d));
		assertEquals(-8d, mMeter.getShutterByAperture(64d));
	}
	
	public void testGetApertureByShutter() throws Exception {
		mMeter.setLux(1 * 2.5);					// 0 EV
		assertEquals(1.0d, mMeter.getApertureByShutter(1));
		assertEquals(2.8d, mMeter.getApertureByShutter(8));
		
		mMeter.setLux(Math.pow(2, 15) * 2.5);	// 15 EV
		assertEquals(LightMeter.MIN_APERTURE_VALUE, mMeter.getApertureByShutter(-64000));	// Out of range
		assertEquals(2.0d, mMeter.getApertureByShutter(-8000));
		assertEquals(64d, mMeter.getApertureByShutter(-8));
	}
	
	public void testISO() throws Exception {
		mMeter.setISO(200);
		mMeter.setLux(Math.pow(2, 15) * 2.5);	// 15 EV
		assertEquals(-15d, mMeter.getShutterByAperture(64d));	// ISO200, can use 1/15 rather than 1/8 for shutter speed
		
		mMeter.setISO(50);
		mMeter.setLux(Math.pow(2, 15) * 2.5);	// 15 EV
		assertEquals(-4d, mMeter.getShutterByAperture(64d));	// ISO50, need slow down to 1/4
		
		mMeter.setISO(200);
		mMeter.setLux(Math.pow(2, 15) * 2.5);	// 15 EV
		assertEquals(32d, mMeter.getApertureByShutter(-60));
		assertEquals(LightMeter.MAX_APERTURE_VALUE, mMeter.getApertureByShutter(-8));		// ISO100 15EV will use 1/8 F64, ISO200 will out of range
		assertEquals(11d, mMeter.getApertureByShutter(-500));	// ISO100 15EV will use 1/500 F8, ISO200 can use F11 rather than F8
		
		mMeter.setISO(50);
		mMeter.setLux(Math.pow(2, 15) * 2.5);	// 15 EV
		assertEquals(5.6d, mMeter.getApertureByShutter(-500));	// Use ISO50, need set aperture to F5.6
	}
	
	public void testCompensation() throws Exception {
		mMeter.setLux(Math.pow(2, 15) * 2.5);
		
		// 15 EV ISO100 F64 should use 1/8
		mMeter.setCompensation(1); // +1 EV
		assertEquals(-4d, mMeter.getShutterByAperture(64d));	// 15+1 EV F64 should use 1/4
		
		mMeter.setCompensation(-2); // -2 EV
		assertEquals(-30d, mMeter.getShutterByAperture(64d));	// 15-2 EV F64 should use 1/30
		
		mMeter.setCompensation(-1/3f); // -1/3 EV
		assertEquals(-10d, mMeter.getShutterByAperture(64d));	// 15-1/3 EV F64 should use 1/10
		
		// 15 EV ISO100 1/8000 should use F2
		mMeter.setCompensation(1); // +1 EV
		assertEquals(1.4d, mMeter.getApertureByShutter(-8000));	// 15+1 EV 1/8000 should use F1.4
		
		mMeter.setCompensation(4/3f); // +1.3 EV
		assertEquals(1.2d, mMeter.getApertureByShutter(-8000));	// 15+1.3 EV 1/8000 should use F1.2
		
		mMeter.setCompensation(-1); // -1 EV
		assertEquals(2.8d, mMeter.getApertureByShutter(-8000));	// 15-1 EV 1/8000 should use F2.8
	}
}
