package hr.ferit.dariomihelcic.doggo.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> supportedPreviewSizes;
    private Camera.Size previewSize;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // supported fragment_camera sizes
        supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the fragment_camera.
        try {
            Camera.Parameters params = mCamera.getParameters();
            if (params.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            mCamera.setParameters(params);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera fragment_camera: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your fragment_camera can change or rotate, take care of those events here.
        // Make sure to stop the fragment_camera before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // fragment_camera surface does not exist
            return;
        }

        // stop fragment_camera before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent fragment_camera
        }

        // set fragment_camera size and make any resize, rotate or
        // reformatting changes here

        // start fragment_camera with new settings
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera fragment_camera: " + e.getMessage());
        }
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
        }

        float ratio;
        if(previewSize.height >= previewSize.width)
            ratio = (float) previewSize.height / (float) previewSize.width;
        else
            ratio = (float) previewSize.width / (float) previewSize.height;

        setMeasuredDimension(width, (int) (width * ratio));
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}
