package com.rex.lightmeter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import android.app.Fragment;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class FragmentReflect extends Fragment {
	
	private static final String TAG = "RexLog";
	private static final boolean DEBUG = true;
	
	private SurfaceView mPreview;
	private SurfaceHolder mHolder;
	private Button mBtnMeasure;
	
	private Camera mCamera;
	private int mCameraNum;
	private int mCameraId = 0;
	private List<Size> mSupportedPreviewSizes;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "FragmentReflect::onCreateView");
		View fragView = inflater.inflate(R.layout.activity_reflect, container, false);
		
		mPreview = (SurfaceView) fragView.findViewById(R.id.reflect_surface);
		mBtnMeasure = (Button) fragView.findViewById(R.id.reflect_button);
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
			if (DEBUG) Log.v(TAG, "FragmentReflect::onCreateView cameraInfo:" + cameraInfo.facing);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				mCameraId = i;
			}
		}
		
		return fragView;
	}
	
	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "FragmentReflect::onPause");
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		if (DEBUG) Log.v(TAG, "FragmentReflect::onResume");
		try {
			mCamera = Camera.open(mCameraId); // attempt to get a Camera instance
		} catch (Exception e) {
			Log.w(TAG, "FragmentReflect::onResume", e);
		}
		//if (mCamera != null) {
		//	mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
		//}
		super.onResume();
	}
	
	private void setupPreview() {
		if (DEBUG) Log.v(TAG, "FragmentReflect::setupPreview");
		// Stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}
		
		// Rotate display orientation according to device
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(mCameraId, info);
		
		int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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
			Log.e(TAG, "FragmentReflect::surfaceChanged Error starting camera preview:" + e.toString());
		}
	}
	
	private SurfaceHolder.Callback mHolderCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "FragmentReflect::surfaceCreated");
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
			if (DEBUG) Log.v(TAG, "FragmentReflect::surfaceChanged width:" + width + " height:" + height);
			
			if (mHolder.getSurface() == null) {
				Log.w(TAG, "FragmentReflect::surfaceChanged preview surface does not exist");
				return;
			}
			
			setupPreview();
		}
		
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "FragmentReflect::surfaceDestroyed");
			// Surface will be destroyed when we return, so stop the preview.
			if (mCamera != null) {
				mCamera.stopPreview();
			}
		}
	};
	
	private OnClickListener mClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (DEBUG) Log.v(TAG, "FragmentReflect::onClick");
			if (mCamera != null) {
				mCamera.takePicture(null, null, mPictureCallback);
			}
		}
	};
	
	private PictureCallback mPictureCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			if (DEBUG) Log.v(TAG, "FragmentReflect::onPictureTaken");
			
			BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(data));
			Metadata metadata = null;
			try {
				metadata = ImageMetadataReader.readMetadata(stream, false);
			} catch (IOException ex) {
				Log.e(TAG, "FragmentReflect::onPictureTaken read meta data failed", ex);
			} catch (ImageProcessingException ex) {
				Log.w(TAG, "FragmentReflect::onPictureTaken image process failed", ex);
			}
			for (Directory directory : metadata.getDirectories()) {
				for (Tag tag : directory.getTags()) {
					Log.i(TAG, tag.toString());
				}
			}
			// obtain the Exif directory
			ExifSubIFDDirectory directory = metadata.getDirectory(ExifSubIFDDirectory.class);
			ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);
			
			// obtain exposure attributes
			String f = directory.getString(ExifSubIFDDirectory.TAG_FNUMBER);
			String t = directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
			String iso = directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
			Log.i(TAG, "Activityeflect::onPictureTaken f:" + f + " t:" + t + " iso:" + iso);
			
			// Camera HAL of some devices have a bug. 
			// Starting preview immediately after taking a picture will fail. 
			// Wait some time before starting the preview.
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					setupPreview();
				}
			}, 300);
		}
	};
}
