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
}
