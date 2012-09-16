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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class ActivityFirst extends Activity {

	private static final boolean DEBUG = true;
	private static final String TAG = "RaxLog";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "ActivityFirst::onCreate");

		setContentView(R.layout.activity_first);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "ActivityFirst::onStart");

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (DEBUG) Log.v(TAG, "Runnable::run timeout");
				Intent intent = new Intent(ActivityFirst.this, ActivityMain.class);
				startActivity(intent);
				finish();
			}
		}, 1000);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (DEBUG) Log.v(TAG, "ActivityFirst::onStop");
	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "ActivityFirst::onDestroy");
		super.onDestroy();
	}
}
