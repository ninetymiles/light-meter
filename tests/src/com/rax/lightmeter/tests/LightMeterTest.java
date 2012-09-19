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

import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;

import com.rax.lightmeter.LightMeter;

public class LightMeterTest extends InstrumentationTestCase {

	private AssetManager mLocalAssets;
	private AssetManager mTargetAssets;
	
	protected void setUp() throws Exception {
		super.setUp();
		mLocalAssets = getInstrumentation().getContext().getAssets();
		mTargetAssets = getInstrumentation().getTargetContext().getAssets();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCaculateEv() throws Exception {
		LightMeter meter = new LightMeter();
		
		assertEquals(-6d, meter.setLux(Math.pow(2, -6) * 2.5));
		assertEquals(0d, meter.setLux(Math.pow(2, 0) * 2.5));
		assertEquals(10d, meter.setLux(1024 * 2.5));	// 2 ^ 10 * 2.5f
		assertEquals(3d, meter.setLux(8 * 2.5));		// 2 ^ 3 * 2.5f
	}
	
	public void testGetTByFv() throws Exception {
		
		LightMeter meter = new LightMeter();
		
		meter.setLux(1 * 2.5);
		assertEquals(1d, meter.getTByFv(1.0d));
		assertEquals(8d, meter.getTByFv(2.8d));
		assertEquals(30d, meter.getTByFv(5.6d));
		assertEquals(60d, meter.getTByFv(8d));
		assertEquals(64 * 60d, meter.getTByFv(64d));
		assertEquals(0d, meter.getTByFv(0d));		// Out of range
		assertEquals(0d, meter.getTByFv(128d));		// Out of range
		
		meter.setLux(Math.pow(2, 3) * 2.5);
		assertEquals(1d, meter.getTByFv(2.0d));
		assertEquals(4d, meter.getTByFv(5.6d));
		
		meter.setLux(1024 * 2.5);
		assertEquals(-1000d, meter.getTByFv(1.0d));
		assertEquals(-30d, meter.getTByFv(5.6d));
		assertEquals(4d, meter.getTByFv(64d));
		
		meter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(0d, meter.getTByFv(1.0d));
		assertEquals(0d, meter.getTByFv(1.4d));
		assertEquals(-8000d, meter.getTByFv(2.0d));
		assertEquals(-8d, meter.getTByFv(64d));
	}
	
	public void testGetFvByT() throws Exception {
		
		LightMeter meter = new LightMeter();
		
		meter.setLux(1 * 2.5);
		assertEquals(1.0d, meter.getFvByT(1));
		assertEquals(2.8d, meter.getFvByT(8));
		assertEquals(5.6d, meter.getFvByT(30));
		assertEquals(8d, meter.getFvByT(60));
		assertEquals(64d, meter.getFvByT(64 * 60));
		assertEquals(0d, meter.getFvByT(0));		// Invalid value
		assertEquals(0d, meter.getFvByT(128));		// Out of range
		
		meter.setLux(1024 * 2.5);
		assertEquals(1.0d, meter.getFvByT(-1000));
		assertEquals(5.6d, meter.getFvByT(-30));
		assertEquals(64d, meter.getFvByT(4));
		
		meter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(0d, meter.getFvByT(-16000));
		assertEquals(2.0d, meter.getFvByT(-8000));
		assertEquals(64d, meter.getFvByT(-8));
	}
	
	public void testISO() throws Exception {
		LightMeter meter = new LightMeter();
		
		meter.setISO(200);
		meter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(-15d, meter.getTByFv(64d));		// ISO200, can use 1/15 rather than 1/8 for shutter speed
		
		meter.setISO(50);
		meter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(-4d, meter.getTByFv(64d));		// ISO50, need slow down to 1/4
		
		meter.setISO(200);
		meter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(32d, meter.getFvByT(-60));
		assertEquals(45d, meter.getFvByT(-30));
		assertEquals(64d, meter.getFvByT(-15));
		assertEquals(0d, meter.getFvByT(-8));		// ISO100 15EV will use 1/8 F64, ISO200 will out of range
		assertEquals(11d, meter.getFvByT(-500));	// ISO100 15EV will use 1/500 F8, ISO200 can use F11 rather than F8
		
		meter.setISO(50);
		meter.setLux(Math.pow(2, 15) * 2.5);
		assertEquals(5.6d, meter.getFvByT(-500));	// Use ISO50, need set aperture to F5.6
		
	}
}
