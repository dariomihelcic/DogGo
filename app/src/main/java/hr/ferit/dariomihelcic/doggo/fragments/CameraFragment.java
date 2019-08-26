package hr.ferit.dariomihelcic.doggo.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import hr.ferit.dariomihelcic.doggo.R;
import hr.ferit.dariomihelcic.doggo.camera.AutoFitTextureView;
import hr.ferit.dariomihelcic.doggo.data.DogBreed;
import hr.ferit.dariomihelcic.doggo.data.DogListHolder;
import hr.ferit.dariomihelcic.doggo.tflite.Classifier;
import hr.ferit.dariomihelcic.doggo.tflite.Recognition;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private CameraCaptureSession captureSession;
    private Object lock = new Object();
    private boolean runClassifier = false;
    private boolean checkPermission = false;
    private Classifier imageClassifier;
    private DogListHolder holder = new DogListHolder();
    private String cameraId;
    private CameraDevice cameraDevice;
    private Size previewSize;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ImageReader imageReader;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;


    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private AutoFitTextureView textureView;
    private TextView tvResultOne;
    private TextView tvResultTwo;
    private TextView tvResultThree;
    private ImageButton btnDogOne;
    private ImageButton btnDogTwo;
    private ImageButton btnDogThree;
    private int[] result_id = new int[3];

    public CameraFragment() {
        // Required empty public constructor
    }

    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice currentDevice) {
            cameraOpenCloseLock.release();
            cameraDevice = currentDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice currentDevice) {
            cameraOpenCloseLock.release();
            currentDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice currentDevice, int i) {
            cameraOpenCloseLock.release();
            currentDevice.close();
            cameraDevice = null;
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(height, width);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        tvResultOne = view.findViewById(R.id.tvResultOne);
        tvResultTwo = view.findViewById(R.id.tvResultTwo);
        tvResultThree = view.findViewById(R.id.tvResultThree);
        btnDogOne = view.findViewById(R.id.btnDogOne);
        btnDogTwo= view.findViewById(R.id.btnDogTwo);
        btnDogThree= view.findViewById(R.id.btnDogThree);
        loadDogList();
        setOnClickListeners(view);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.list_menu, menu);
        MenuItem item = menu.findItem(R.id.dog_list);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW|MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DogListFragment listFragment = DogListFragment.newInstance();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, listFragment);
        transaction.commit();
        return  super.onOptionsItemSelected(item);
    }


    private void setOnClickListeners(final View view) {
    btnDogOne.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = result_id[0];
            createDescFragment(view, id);
        }
    });
    btnDogTwo.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = result_id[1];
            createDescFragment(view, id);
        }
    });
    btnDogThree.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = result_id[2];
            createDescFragment(view, id);
        }
    });
    }

    private void createDescFragment(View v, int id) {
        DogBreed dog = holder.getDogBreed(holder.getMappedId(id));
        Bundle bundle = new Bundle();

        bundle.putString("description", dog.getDescription());
        bundle.putString("picture", dog.getPicture());

        DogDescriptionFragment newFragment = DogDescriptionFragment.newInstance();
        newFragment.setArguments(bundle);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, newFragment);
        transaction.commit();
    }

    private String[] getRequiredPermissions() {
        Activity activity = getActivity();
        try {
            PackageInfo info =
                    activity
                            .getPackageManager()
                            .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {
                            classifyFrame();
                        }
                    }
                    backgroundHandler.post(periodicClassify);
                }
            };

    private static Size chooseOptimalSize(
            Size[] choices,
            int textureViewWidth,
            int textureViewHeight,
            int maxWidth,
            int maxHeight,
            Size aspectRatio) {

        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth
                    && option.getHeight() <= maxHeight
                    && option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Fragment", "Couldn't find any suitable fragment_camera size");
            return choices[0];
        }
    }

    public static Fragment newInstance() {
        return new CameraFragment();
    }

    private void loadDogList() {
        try {
            holder.loadData(getContext());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CameraFragment", "Failed to load data.");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            imageClassifier = new Classifier(getActivity(), 4);
        } catch (IOException e) {
            Log.e("CameraFragment", "Failed to initialize image classifier");
        }

        startBackgroundThread();
    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    @Override
    public void onDestroy() {
        imageClassifier.close();
        super.onDestroy();
    }

    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size largest =
                        Collections.max(
                                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader =
                        ImageReader.newInstance(
                                largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/ 2);

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                previewSize =
                        chooseOptimalSize(
                                map.getOutputSizes(SurfaceTexture.class),
                                rotatedPreviewWidth,
                                rotatedPreviewHeight,
                                maxPreviewWidth,
                                maxPreviewHeight,
                                largest);

                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                this.cameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        if (!checkPermission && !allPermissionsGranted()) {
            super.requestPermissions( getRequiredPermissions(), PERMISSION_REQUEST_CODE);
            return;
        } else {
            checkPermission = true;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }
        private boolean allPermissionsGranted() {
            for (String permission : getRequiredPermissions()) {
                if (ContextCompat.checkSelfPermission(getActivity(), permission)!= PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraFregment");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        backgroundHandler.post(periodicClassify);
    }


    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            Surface surface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == cameraDevice) {
                                return;
                            }

                            captureSession = cameraCaptureSession;
                            try {
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void classifyFrame() {
        if (imageClassifier == null || getActivity() == null || cameraDevice == null) {
            return;
        }
        Bitmap bitmap =
                textureView.getBitmap(Classifier.getImageSizeX(), Classifier.getImageSizeY());
        List<Recognition> results = imageClassifier.recognizeImage(bitmap);
        bitmap.recycle();
        showResults(results);
        writeResultId(results);
    }

    private void writeResultId(List<Recognition> results) {
        result_id[0] = Integer.parseInt(results.get(0).getId());
        result_id[1] = Integer.parseInt(results.get(1).getId());
        result_id[2] = Integer.parseInt(results.get(2).getId());
    }


    private void showResults(final List<Recognition> results) {
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (activity != null) {
                    if (results != null && results.size() >= 3) {
                        Recognition recognition = results.get(0);
                        if (recognition != null) {
                            if (recognition.getTitle() != null && recognition.getConfidence() != null)
                                tvResultOne.setText(String.format("%.2f", (100 * recognition.getConfidence())) + "%" + recognition.getTitle());
                        }
                        Recognition recognition1 = results.get(1);
                        if (recognition != null) {
                            if (recognition.getTitle() != null && recognition1.getConfidence() != null)
                                tvResultTwo.setText(String.format("%.2f", (100 * recognition1.getConfidence())) + "%" + recognition1.getTitle());
                        }

                        Recognition recognition2 = results.get(2);
                        if (recognition != null) {
                            if (recognition.getTitle() != null && recognition2.getConfidence() != null)
                                tvResultThree.setText(String.format("%.2f", (100 * recognition2.getConfidence())) + "%" + recognition2.getTitle());
                        }
                    }
                }
            }
        });
    }

    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }

    }

}
