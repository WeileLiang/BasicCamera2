package com.example.sensetimeliangweile.basiccamera2.fragments;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.example.sensetimeliangweile.basiccamera2.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Camera2Fragment extends Fragment {

    private CameraManager mCameraManager;
    private String mCameraId;
    private CameraCharacteristics mCharacteristics;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private Size mPreviewSize;

    private TextureView mTexture;

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    public static Camera2Fragment newInstance() {
        Camera2Fragment fragment = new Camera2Fragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera2, container, false);

        mTexture = view.findViewById(R.id.texture);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();
        mTexture.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private void openCamera(int width, int height) {
        setupCameraOutputs(width, height);
        try {
            mCameraManager.openCamera(mCameraId,mStateCallback,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("camera");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void setupCameraOutputs(int width, int height) {
        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);


        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristic = mCameraManager.getCameraCharacteristics(cameraId);
                if (CameraCharacteristics.LENS_FACING_BACK == characteristic.get(CameraCharacteristics.LENS_FACING)) {
                    mCameraId = cameraId;
                    mCharacteristics = characteristic;
                    break;
                }
            }

            StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largestJpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new ComparatorByArea());

            mPreviewSize = getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), largestJpeg);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreviewSession() {
        SurfaceTexture texture = mTexture.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        Surface surface = new Surface(texture);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d("lwl_success", "success");

                    mCameraCaptureSession = session;
                    try {
                        // Auto focus should be continuous for camera preview.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // Flash is automatically enabled when necessary.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                            }
                        }, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.d("lwl_message", e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d("lwl_failed", "failed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalPreviewSize(Size[] options, Size largestJpeg) {
        int textureViewWidth = mTexture.getWidth();
        int textureViewHeight = mTexture.getHeight();

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swapSize = false;
        switch (rotation) {
            case Surface
                    .ROTATION_0:
            case Surface.ROTATION_180: {
                if (sensorOrientation == 90 || sensorOrientation == 270) swapSize = true;
                break;
            }
            case Surface
                    .ROTATION_90:
            case Surface.ROTATION_270: {
                if (sensorOrientation == 0 || sensorOrientation == 180) swapSize = true;
                break;
            }

        }

        if (swapSize) {
            textureViewWidth = mTexture.getHeight();
            textureViewHeight = mTexture.getWidth();
        }

        List<Size> bigEnough = new ArrayList<>();
        for (Size option : options) {
            if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight
                    && option.getWidth() == option.getHeight() * largestJpeg.getWidth() / largestJpeg.getHeight()) {
                bigEnough.add(option);
            }
        }

        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new ComparatorByArea());
        } else {
            return options[0];
        }
    }

    public class ComparatorByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}
