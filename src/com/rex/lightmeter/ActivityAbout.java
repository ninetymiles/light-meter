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

package com.rex.lightmeter;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.rex.flurry.FlurryAgentWrapper;
import com.rex.lightmeter.R;

public class ActivityAbout extends PreferenceActivity {

	private static final String TAG = "RexLog";
	private static final boolean DEBUG = true;

	private static final String KEY_ABOUT_VERSION = "preference_about_version";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.about);
		
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			Preference prefsVersion = getPreferenceScreen().findPreference(KEY_ABOUT_VERSION);
			String strTemplate = getResources().getString(R.string.about_version_summary);
			String strVersion = String.format(strTemplate, packageInfo.versionName);
			prefsVersion.setSummary(strVersion);
		} catch (NameNotFoundException ex) {
			Log.e(TAG, "ActivityAbout::onCreate " + ex.getMessage());
		}
	}
	
	@Override
	protected void onStart() {
		if (DEBUG) Log.v(TAG, "ActivityAbout::onStart");
		FlurryAgentWrapper.onStartSession(this);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "ActivityAbout::onStop");
		FlurryAgentWrapper.onEndSession(this);
		super.onStop();
	}
}
