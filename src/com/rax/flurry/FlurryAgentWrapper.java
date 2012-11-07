package com.rax.flurry;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.flurry.android.FlurryAgent;


public final class FlurryAgentWrapper {
	
	private static final String TAG = "FlurryAgent";
	private static final boolean DEBUG = false;
	
	private static final String PREFS_ID = "FLURRY_TRACKING_ENABLED";
	private static final String FLURRY_KEY = "6MKC6G3R9BXBY3N84WZ4";
	
	private static boolean isEnableTracking = true;
	private static boolean isInitialized = false;
	
	public static final void onStartSession(Context context) {
		if (isInitialized == false) {
			FlurryAgent.setCaptureUncaughtExceptions(false);
			FlurryAgent.setContinueSessionMillis(15000);
			FlurryAgent.setLogEnabled(DEBUG);
			isInitialized = true;
			isEnableTracking = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_ID, true);
			
			if (DEBUG) Log.d(TAG, "FlurryAgentWrapper::onStartSession isEnableTracking:" + isEnableTracking);

			Log.i(TAG, "Starting new session with key: "
							+ FLURRY_KEY.substring(0, 4)
							+ "..."
							+ FLURRY_KEY.substring(FLURRY_KEY.length() - 4,
									FLURRY_KEY.length()));
		}
		if (isEnableTracking) FlurryAgent.onStartSession(context, FLURRY_KEY);
	}
	
	public static final void onEndSession(Context context) {
		if (isEnableTracking) FlurryAgent.onEndSession(context);
	}
	
	public static final void logEvent(String eventId) {
		if (isEnableTracking) FlurryAgent.logEvent(eventId);
	}
	
	public static final void logUserChoose(String eventId, String eventDes) {
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put("USER_CHOOSE", eventDes);
		if (isEnableTracking) FlurryAgent.logEvent(eventId, parameters);
	}
	
	public static final void logEventResult(String eventId, String eventDes) {
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put("RESULT", eventDes);
		if (isEnableTracking) FlurryAgent.logEvent(eventId, parameters);
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
