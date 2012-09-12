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
import android.util.Log;

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
		
		assertEquals(10d, meter.caculateEv(1024 * 250));
		assertEquals(3d, meter.caculateEv(8 * 250));
	}
	
	public void testGetShutterByFv() throws Exception {
		
		LightMeter meter = new LightMeter();
		
		meter.caculateEv(0);
		assertEquals(1, meter.getShutterByFv(1.0f));
		assertEquals(0, meter.getShutterByFv(0f));		// Out of range
		assertEquals(0, meter.getShutterByFv(128f));	// Out of range
		
		meter.caculateEv(1024 * 250);
		assertEquals(-1000, meter.getShutterByFv(1.0f));
		assertEquals(-30, meter.getShutterByFv(5.6f));
		assertEquals(4, meter.getShutterByFv(64f));
		
		meter.caculateEv(Math.pow(2, 15) * 250);
		assertEquals(0, meter.getShutterByFv(1.0f));
		assertEquals(0, meter.getShutterByFv(1.4f));
		assertEquals(-8000, meter.getShutterByFv(2.0f));
		assertEquals(-8, meter.getShutterByFv(64f));
	}
	
	public void testGetFvByShutter() throws Exception {
		
		LightMeter meter = new LightMeter();
		
		meter.caculateEv(0);
		assertEquals(1.0f, meter.getFvByShutter(1));
		assertEquals(8.0f, meter.getFvByShutter(60));
		assertEquals(0f, meter.getFvByShutter(0));		// Invalid value
		assertEquals(0f, meter.getFvByShutter(128));	// Out of range
		
		meter.caculateEv(1024 * 250);
		assertEquals(1.0f, meter.getFvByShutter(-1000));
		assertEquals(5.6f, meter.getFvByShutter(-30));
		assertEquals(64f, meter.getFvByShutter(4));
		
		meter.caculateEv(Math.pow(2, 15) * 250);
		assertEquals(0f, meter.getFvByShutter(-16000));
		assertEquals(2.0f, meter.getFvByShutter(-8000));
		assertEquals(64f, meter.getFvByShutter(-8));
	}
}
