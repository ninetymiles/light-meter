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

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivitySettings extends PreferenceActivity {

    private final Logger mLogger = LoggerFactory.getLogger("RexLog");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogger.trace("");

        addPreferencesFromResource(R.xml.settings);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (! prefs.getBoolean("CONF_ENABLE_EXPERIMENTAL", false)) {
            PreferenceScreen screen = getPreferenceScreen();
            screen.removePreference(screen.findPreference("CATEGORY_EXPERIMENTAL"));
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLogger.trace("");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLogger.trace("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // TODO: Should use android:parentActivityName in AndroidManifest.xml instead
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
