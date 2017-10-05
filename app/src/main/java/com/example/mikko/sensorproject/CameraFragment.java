package com.example.mikko.sensorproject;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.mikko.R;
import com.example.mikko.sensorproject.CompassActivity.Compass;

import java.util.Arrays;
import java.util.List;

/**
 * Created by buckfast on 21.9.2017.
 */
public class CameraFragment extends Fragment implements Compass.OnAngleChangedListener {


    private BetterTextureView texture;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;

    private Compass compass;

    private Size imageDimension;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;

    int devicewidth;
    int deviceheight;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    //private int activePointer = MotionEvent.INVALID_POINTER_ID;
    //private float lastY = 0f;
    //private float lastX = 0f;
    Canvas canvas;

    SurfaceView drawSurface;
    private SurfaceHolder sfhTrackHolder;
    //List<Integer> bestPreview;

    DragInterface dragCallback;
    ChangeFragmentListener changeFragmentListener;
    DragUtils dragUtils;
    private DestinationInterface destinationInterface;
    boolean built = false;

    private ImageView cornerIcon;


    public CameraFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        dragCallback = (DragInterface) context;
        dragUtils = new DragUtils();
        changeFragmentListener = (ChangeFragmentListener) context;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);

        compass = new Compass(getActivity(), this) ;

        drawSurface = (SurfaceView) v.findViewById(R.id.surface);
        drawSurface.setZOrderOnTop(true);    // necessary?
        sfhTrackHolder = drawSurface.getHolder();
        sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);

     //   surface.setAlpha(0.5f);
      //  surface.addView(new AugmentedView(this));

        Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        dragUtils.setupViewDrag(v, dragCallback);

        cornerIcon = (ImageView)v.findViewById(R.id.cornericon);
        cornerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFragmentListener.changeEvent("compass");
            }
        });
        return v;
    }




    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
            getScreenResolution(getContext());
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

    private void openCamera() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission


            if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                //showPhoneStatePermission();
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

            System.out.println(imageDimension.getWidth()+"  "+imageDimension.getHeight());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

/*
    private void showPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.CAMERA)) {
                showExplanation("Permission Needed", "Rationale", android.Manifest.permission.CAMERA, REQUEST_CAMERA_PERMISSION);
            } else {
                requestPermission(android.Manifest.permission.CAMERA, REQUEST_CAMERA_PERMISSION);
            }
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }
    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{permissionName}, permissionRequestCode);
    }*/
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open

            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.d("map: " , "start compass");
        compass.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        compass.start();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (texture.isAvailable()) {
            openCamera();
        } else {
            texture.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    public void onPause() {
        cameraDevice.close();
        compass.stop();
        cameraDevice = null;
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        ((ViewGroup)getView()).removeAllViewsInLayout();
        View newview = inflater.inflate(R.layout.fragment_camera,  ((ViewGroup)getView()));


    }*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int screenRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();

    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = this.texture.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            System.out.println("asdad");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        texture = (BetterTextureView) view.findViewById(R.id.texture);
        assert texture != null;
        texture.setSurfaceTextureListener(textureListener);
        built = true;

    }

    public void drawing (float azimuth) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas = sfhTrackHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float devicePercent = (float) (devicewidth/100.0);
        float azimuthPercent = azimuth/45;


        if (azimuth < 22.5 && azimuth > -22.5) {
            canvas.drawCircle(devicewidth/2 - devicePercent * (azimuthPercent*100), deviceheight/2 ,100 , paint );
            Log.i("Drew circle", String.valueOf(azimuth));
        }
        else if (azimuth > 22.5 && azimuth < 180) {
            canvas.drawLine(0,deviceheight/2, 200, deviceheight/2, paint );
        }
        else if (azimuth < -22.5 && azimuth > -180) {
            canvas.drawLine(devicewidth, deviceheight/2, devicewidth-200, deviceheight/2, paint);
        }




        sfhTrackHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void onAngleChanged(Float azimuth) {
        if (built) {
            Log.i("piirtaaaaaa", String.valueOf(azimuth));
            drawing(azimuth);
        }
    }

    private void getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        devicewidth = metrics.widthPixels;
        deviceheight = metrics.heightPixels;
    }
}
