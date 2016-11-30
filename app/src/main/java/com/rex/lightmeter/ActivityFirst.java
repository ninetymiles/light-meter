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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.rex.lightmeter.R;

public class ActivityFirst extends Activity {

    private static final boolean DEBUG = false;
    private static final String TAG = "RexLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "ActivityFirst::onCreate");
        setContentView(R.layout.activity_first);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        if (DEBUG) Log.v(TAG, "ActivityFirst::onStart");
        startMain();
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.v(TAG, "ActivityFirst::onStop");
        super.onStop();
    }

    private void startMain() {
        Intent intent = new Intent(ActivityFirst.this, ActivityMain.class);
        startActivity(intent);
        finish();
    }
}
