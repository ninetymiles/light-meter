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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityFirst extends Activity {

    private final Logger mLogger = LoggerFactory.getLogger("RexLog");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogger.trace("");
        setContentView(R.layout.activity_first);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLogger.trace("");
        startMain();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLogger.trace("");
    }

    private void startMain() {
        mLogger.trace("");
        Intent intent = new Intent(ActivityFirst.this, ActivityMain.class);
        startActivity(intent);
        finish();
    }
}
