/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.rax.lightmeter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.rax.flurry.FlurryAgentWrapper;

public class ActivitySettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "RaxLog";
	private static final boolean DEBUG = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	@Override
	protected void onStart() {
		if (DEBUG) Log.v(TAG, "ActivitySettings::onStart");
		FlurryAgentWrapper.onStartSession(this);
		super.onStart();
	}

	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "ActivitySettings::onStop");
		FlurryAgentWrapper.onEndSession(this);
		super.onStop();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (DEBUG) Log.d(TAG, "ActivitySettings::onSharedPreferenceChanged key:" + key);
		if ("CONF_ENABLE_TRACKING".equals(key)) {
			//FlurryAgentWrapper.setTrackingEnabled(this, sharedPreferences.getBoolean(key, true));
		}
	}
}
