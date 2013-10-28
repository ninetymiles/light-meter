package com.rex.lightmeter;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import com.rex.flurry.FlurryAgentWrapper;

public class ActivityReflect extends Activity {
	
	private static final String TAG = "RexLog";
	private static final boolean DEBUG = true;
	
	private SurfaceView mPreview;
	private SurfaceHolder mHolder;
	private Button mBtnMeasure;
	
	private Camera mCamera;
	private int mCameraNum;
	private int mCameraId = -1;
	private List<Size> mSupportedPreviewSizes;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onCreate");
		super.onCreate(savedInstanceState);
		
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("CONF_ENABLE_KEEP_SCREEN_ON", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		
		setContentView(R.layout.activity_reflect);
		mPreview = (SurfaceView) findViewById(R.id.reflect_surface);
		mBtnMeasure = (Button) findViewById(R.id.reflect_button);
		mBtnMeasure.setOnClickListener(mClickListener);
		
		// Find the total number of cameras available
		mCameraNum = Camera.getNumberOfCameras();
		
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mPreview.getHolder();
		mHolder.addCallback(mHolderCallback);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// Find the ID of the default camera
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < mCameraNum; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				mCameraId = i;
			}
		}
	}
	
	@Override
	protected void onStart() {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onStart");
		FlurryAgentWrapper.onStartSession(this);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onStop");
		FlurryAgentWrapper.onEndSession(this);
		super.onStop();
	}
	
	@Override
	protected void onPause() {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onPause");
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onResume");
		try {
			mCamera = Camera.open(mCameraId); // attempt to get a Camera instance
		} catch (Exception e) {
			Log.e(TAG, "ActivityReflect::onResume", e);
		}
		if (mCamera != null) {
			mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_reflect_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onOptionsItemSelected itemId:" + item.getItemId());
		switch (item.getItemId()) {
		case R.id.menu_switch:
			startActivity(new Intent(this, ActivitySettings.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onSaveInstanceState");
		//outState.putDouble("LUX", mMeter.getLux());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onRestoreInstanceState");
		//mMeter.setLux(savedInstanceState.getDouble("LUX"));
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public void onBackPressed() {
		if (DEBUG) Log.v(TAG, "ActivityReflect::onBackPressed");
		super.onBackPressed();
	}
	
	private SurfaceHolder.Callback mHolderCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "ActivityReflect::surfaceCreated");
			// The Surface has been created, acquire the camera and tell it where to draw.
			try {
				if (mCamera != null) {
					mCamera.setPreviewDisplay(holder);
					mCamera.startPreview();
				}
			} catch (IOException exception) {
				Log.e(TAG, "IOException caused by setPreviewDisplay", exception);
			}
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if (DEBUG) Log.v(TAG, "ActivityReflect::surfaceChanged width:" + width + " height:" + height);
			
			if (mHolder.getSurface() == null) {
				Log.w(TAG, "ActivityReflect::surfaceChanged preview surface does not exist");
				return;
			}
			
			// Stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}
			
			// Rotate display orientation according to device
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(mCameraId, info);
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			int degrees = 0;
			switch (rotation) {
			case Surface.ROTATION_0:	degrees = 0;	break;
			case Surface.ROTATION_90:	degrees = 90;	break;
			case Surface.ROTATION_180:	degrees = 180;	break;
			case Surface.ROTATION_270:	degrees = 270;	break;
			}
			int result;
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				result = (info.orientation + degrees) % 360;
				result = (360 - result) % 360; // compensate the mirror
			} else { // back-facing
				result = (info.orientation - degrees + 360) % 360;
			}
			mCamera.setDisplayOrientation(result);
			
			// Set preview size
			//Camera.Parameters parameters = mCamera.getParameters();
			//parameters.setPreviewSize(width, height);
			//mCamera.setParameters(parameters);
			
			// Start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			} catch (Exception e) {
				Log.e(TAG, "ActivityReflect::surfaceChanged Error starting camera preview:" + e.toString());
			}
		}
		
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "ActivityReflect::surfaceDestroyed");
			// Surface will be destroyed when we return, so stop the preview.
			if (mCamera != null) {
				mCamera.stopPreview();
			}
		}
	};
	
	private OnClickListener mClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (DEBUG) Log.v(TAG, "ActivityReflect::OnClickListener::onClick");
		}
	};
}
