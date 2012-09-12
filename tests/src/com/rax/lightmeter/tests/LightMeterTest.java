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
		
		meter.setISO(200);
		assertEquals(-15, meter.getShutterByFv(64f));	// ISO200, can use 1/15 rather than 1/8 for shutter speed
		
		meter.setISO(50);
		assertEquals(-4, meter.getShutterByFv(64f));	// ISO50, need slow down to 1/4
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
		
		meter.setISO(200);
		assertEquals(0f, meter.getFvByShutter(-8));		// ISO100 15EV will use 1/8 F64, ISO200 will out of range
		assertEquals(11f, meter.getFvByShutter(-500));	// ISO100 15EV will use 1/500 F8, ISO200 can use F11 rather than F8
		
		meter.setISO(50);
		assertEquals(5.6f, meter.getFvByShutter(-500));	// Use ISO50, need set aperture to F5.6
	}
	
	public void testISO() throws Exception {
		
		class MockLightMeter extends LightMeter {
			public int testGetEvIndex(int ev) {
				return getEvIndex(ev);
			}
		};
		
		MockLightMeter meter = new MockLightMeter();
		
		meter.setISO(100);
		assertEquals(0, meter.testGetEvIndex(-6));		// Default -6EV will be map to data line 0
		assertEquals(6, meter.testGetEvIndex(0));		// 0EV will be map to data line 6
		assertEquals(-1, meter.testGetEvIndex(-7));		// Out of range
		assertEquals(27, meter.testGetEvIndex(21));		// Last line of sExposureValue
		assertEquals(-1, meter.testGetEvIndex(22));		// Out of range
		
		meter.setISO(200);
		assertEquals(0, meter.testGetEvIndex(-7));		// ISO200, -7EV will be map to data line 0
		assertEquals(7, meter.testGetEvIndex(0));		// ISO200, 0EV will be map to data line 7
		assertEquals(-1, meter.testGetEvIndex(21));		// 16EV will out of range
		
		meter.setISO(50);
		assertEquals(-1, meter.testGetEvIndex(-6));		// ISO50, -6EV will out of range
		assertEquals(5, meter.testGetEvIndex(0));		// 0EV will be map to data line 5
		assertEquals(26, meter.testGetEvIndex(21));		// 21EV will be map to data line 26
	}
}
