package com.rax.flurry;

import java.util.Map;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.rax.lightmeter.BuildConfig;


public final class FlurryAgentWrapper {
	
	private static final String TAG = "FlurryAgent";
	private static final boolean DEBUG = false;
	
	private static final String PREFS_ID = "FLURRY_TRACKING_ENABLED";
	private static final String FLURRY_KEY_DEV = "6MKC6G3R9BXBY3N84WZ4";
	private static final String FLURRY_KEY_PUB = "X5BB66QDR39R2PGFYSM9";
	
	private static boolean isEnableTracking = true;
	private static boolean isInitialized = false;
	
	public static final void onStartSession(Context context) {
		String flurryKey = BuildConfig.DEBUG ? FLURRY_KEY_DEV : FLURRY_KEY_PUB;
		if (isInitialized == false) {
			FlurryAgent.setCaptureUncaughtExceptions(false);
			FlurryAgent.setContinueSessionMillis(15000);
			FlurryAgent.setLogEnabled(DEBUG);
			isInitialized = true;
			isEnableTracking = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_ID, true);
			
			if (DEBUG) Log.d(TAG, "FlurryAgentWrapper::onStartSession isEnableTracking:" + isEnableTracking);

			Log.i(TAG, "Starting new session with key: "
							+ flurryKey.substring(0, 4)
							+ "..."
							+ flurryKey.substring(flurryKey.length() - 4,
									flurryKey.length()));
		}
		if (isEnableTracking) FlurryAgent.onStartSession(context, flurryKey);
	}
	
	public static final void onEndSession(Context context) {
		if (isEnableTracking) FlurryAgent.onEndSession(context);
	}
	
	public static final void logEvent(String eventId) {
		if (isEnableTracking) FlurryAgent.logEvent(eventId);
	}
	
	public static final void logEvent(String eventId, boolean timed) {
		if (isEnableTracking) FlurryAgent.logEvent(eventId, timed);
	}
	
	public static final void logEvent(String eventId, Map parameters) {
		if (isEnableTracking) FlurryAgent.logEvent(eventId, parameters);
	}
	
	public static final void logEvent(String eventId, Map parameters, boolean timed) {
		if (isEnableTracking) FlurryAgent.logEvent(eventId, parameters, timed);
	}
	
	public static final void endTimedEvent(String eventId) {
		if (isEnableTracking) FlurryAgent.endTimedEvent(eventId);
	}
	
	public static final void setTrackingEnabled(Context context, boolean isEnableTracking) {
		if (DEBUG) Log.d(TAG, "FlurryAgentWrapper::setTrackingEnabled isEnableTracking:" + isEnableTracking);
		FlurryAgentWrapper.isEnableTracking = isEnableTracking;
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREFS_ID, isEnableTracking).commit();
	}
}
