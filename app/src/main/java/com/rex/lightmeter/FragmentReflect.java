package com.rex.lightmeter;

import android.app.Fragment;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class FragmentReflect extends Fragment {

    private final Logger mLogger = LoggerFactory.getLogger("RexLog");

    private SurfaceView mPreview;
    private SurfaceHolder mHolder;
    private ImageButton mBtnMeasure;

    private Camera mCamera;
    private int mCameraNum;
    private int mCameraId = 0;
    private List<Size> mSupportedPreviewSizes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLogger.trace("");
        View fragView = inflater.inflate(R.layout.fragment_reflect, container, false);

        mPreview = (SurfaceView) fragView.findViewById(R.id.reflect_surface);
        mBtnMeasure = (ImageButton) fragView.findViewById(R.id.reflect_button);
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
            mLogger.trace("cameraInfo:{}", cameraInfo.facing);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                mCameraId = i;
            }
        }

        return fragView;
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogger.trace("");
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mLogger.trace("");
        try {
            mCamera = Camera.open(mCameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            mLogger.warn("Failed to open camera\n", e);
        }
        //if (mCamera != null) {
        //	mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        //}
    }

    private void setupPreview() {
        mLogger.trace("");
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
            mLogger.error("Failed to start camera preview\n", e);
        }
    }

    private SurfaceHolder.Callback mHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mLogger.trace("");
            // The Surface has been created, acquire the camera and tell it where to draw.
            try {
                if (mCamera != null) {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                }
            } catch (IOException ex) {
                mLogger.error("Failed to set preview display\n", ex);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mLogger.trace("format:{} width:{} height:{}", format, width, height);
            if (mHolder.getSurface() == null) {
                mLogger.warn("Preview surface does not exist");
                return;
            }
            setupPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mLogger.trace("");
            // Surface will be destroyed when we return, so stop the preview.
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }
    };

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mLogger.trace("");
            if (mCamera != null) {
                mCamera.takePicture(null, null, mPictureCallback);
            }
        }
    };

    private PictureCallback mPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mLogger.trace("");

            BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(data));
            Metadata metadata = null;
            try {
                metadata = ImageMetadataReader.readMetadata(stream, false);
            } catch (IOException ex) {
                mLogger.error("Failed to read meta data\n", ex);
            } catch (ImageProcessingException ex) {
                mLogger.warn("Failed to process image\n", ex);
            }
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    mLogger.info("  {}", tag.toString());
                }
            }
            // obtain the Exif directory
            ExifSubIFDDirectory directory = metadata.getDirectory(ExifSubIFDDirectory.class);
            ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(directory);

            // obtain exposure attributes
            String f = directory.getString(ExifSubIFDDirectory.TAG_FNUMBER);
            String t = directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
            String iso = directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
            mLogger.info("f:{} t:{} iso:{}", f, t, iso);

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
