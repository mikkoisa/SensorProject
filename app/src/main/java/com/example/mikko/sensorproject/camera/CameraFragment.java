package com.example.mikko.sensorproject.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.mikko.R;
import com.example.mikko.sensorproject.compass.Compass;
import com.example.mikko.sensorproject.interfaces.ChangeFragmentListener;
import com.example.mikko.sensorproject.interfaces.DestinationInterface;
import com.example.mikko.sensorproject.interfaces.DragInterface;
import com.example.mikko.sensorproject.utils.DragUtils;

import java.util.Arrays;

public class CameraFragment extends Fragment implements Compass.OnAngleChangedListener {

    //textureview for camera preview
    private BetterTextureView texture;


    private Compass compass;
    
    //size of camera preview
    private Size imageDimension;
    
    //opened camera device
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;

    int devicewidth;
    int deviceheight;

    
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    Canvas canvas;

    SurfaceView drawSurface;
    private SurfaceHolder sfhTrackHolder;


    //let user change the size of camera view
    DragInterface dragCallback;
    
    
    ChangeFragmentListener changeFragmentListener;
   
    //let user change the size of camera view
    DragUtils dragUtils;
    private DestinationInterface destinationInterface;
    boolean built = false;


    public CameraFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        dragCallback = (DragInterface) context;
        dragUtils = new DragUtils();
        //to change fragment to compass
        changeFragmentListener = (ChangeFragmentListener) context;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @TargetApi(Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);

        compass = new Compass(getActivity(), this) ;
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            //get destination location even when camera is swapped to compass and back
            double lat = bundle.getDouble("latitude", 60);
            double lon = bundle.getDouble("longitude", 24);
            Log.i("inio", "CAMERAN BUNDLE: "+lat+", "+lon);

            setDest(lat,lon);
        } else {
            Log.i("inio", "bundle on nll");

        }

        drawSurface = (SurfaceView) v.findViewById(R.id.surface);
        drawSurface.setZOrderOnTop(true);    // necessary? -yes
        sfhTrackHolder = drawSurface.getHolder();
        sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);


        //devicewidth = drawSurface.getMeasuredWidth();
        //deviceheight = drawSurface.getMeasuredHeight();

     //   surface.setAlpha(0.5f);
      //  surface.addView(new AugmentedView(this));

        Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        dragUtils.setupViewDrag(v, dragCallback);

        ImageView cornerIcon = (ImageView) v.findViewById(R.id.cornericon);
        cornerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFragmentListener.changeEvent("compass");
            }
        });
        return v;
    }


    public void setDest(Double lat, Double lon){
        compass.setCoord(lat, lon);
    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();


            deviceheight = height;
            devicewidth = width;
        }

        @Override
        //when view is dragged, texture for camera preview gets stretched
        //so configureTransform is called everytime texture size changes.
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                deviceheight = height;
                devicewidth = width;

                //could optimize to make it smoother... (maybe freeze the camera)
                configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    public void locationChanged(Double newLat, Double newLon, Double speed) {

        compass.setMyLocation(newLat, newLon, speed);
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

Log.i("inio", String.valueOf(imageDimension));

            configureTransform(texture.getWidth(), texture.getHeight());

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
        
        //start a background thread so when resuming app, camera will still be available
        startBackgroundThread();
        compass.start();

        //if screen is turned off and on, check if texture is available to open camera
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



    private void createCameraPreview() {
        try {
            SurfaceTexture texture = this.texture.getSurfaceTexture();
            assert texture != null;
            //set size of image buffers (could be used for freezing maybe)
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
                    // When the session is ready, we start displaying the preview
                    //update preview
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

            if (null == cameraDevice) {
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

        Paint paint2 = new Paint();
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(10);
        paint2.setColor(Color.rgb(57,63,174));


        canvas = sfhTrackHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


        float devicePercent = (float) (devicewidth / 100.0);
        float azimuthPercent = azimuth/45;


        //The constantly changing target

        if (azimuth < 22.5 && azimuth > -22.5) {
            if (azimuth <10 && azimuth > -10){
                canvas.drawLine(devicewidth/2 - 200, deviceheight/2+10, devicewidth/2 , deviceheight/4+10 , paint2 );
                canvas.drawLine(devicewidth/2 , deviceheight/4+10 , devicewidth/2 +200, deviceheight/2+10 , paint2 );
            } else {
                if(azimuth <-10) {
                    //canvas.drawCircle(devicewidth / 2 - devicePercent * (azimuthPercent * 100), deviceheight / 2, 100, paint2);
                    canvas.drawLine(devicewidth / 2 - devicePercent * (azimuthPercent * 100), deviceheight / 2, devicewidth / 2 - devicePercent * (azimuthPercent * 100) + 150, deviceheight / 2 - 50, paint2);
                    canvas.drawLine(devicewidth / 2 - devicePercent * (azimuthPercent * 100), deviceheight / 2, devicewidth / 2 - devicePercent * (azimuthPercent * 100) + 150, deviceheight / 2 + 50, paint2);
                    Log.i("Drew circle", String.valueOf(azimuth));
                }
                else if (azimuth >10 ){
                    canvas.drawLine(devicewidth / 2 - devicePercent * (azimuthPercent * 100), deviceheight / 2, devicewidth / 2 - devicePercent * (azimuthPercent * 100) - 150, deviceheight / 2 - 50, paint2);
                    canvas.drawLine(devicewidth / 2 - devicePercent * (azimuthPercent * 100), deviceheight / 2, devicewidth / 2 - devicePercent * (azimuthPercent * 100) - 150, deviceheight / 2 + 50, paint2);
                }
            }
        }
        else if (azimuth > 22.5 && azimuth < 180) {
            canvas.drawLine(0,deviceheight/2, 150, deviceheight / 2 - 60, paint2 );
            canvas.drawLine(0,deviceheight/2, 150, deviceheight / 2 + 60, paint2 );
        }
        else if (azimuth < -22.5 && azimuth > -180) {
            canvas.drawLine(devicewidth, deviceheight/2, devicewidth-150, deviceheight / 2 - 60, paint2);
            canvas.drawLine(devicewidth, deviceheight/2, devicewidth-150, deviceheight / 2 + 60, paint2);
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

    //when camera preview size changes, texture needs transformation so aspect ratio stays constant and
    //preview fills the screen
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == texture || null == imageDimension || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, imageDimension.getHeight(), imageDimension.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        float scale = Math.max((float) viewHeight / imageDimension.getHeight(), (float) viewWidth / imageDimension.getWidth());
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        } else if (Surface.ROTATION_0 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            scale = Math.min((float) viewHeight / imageDimension.getHeight(), (float) viewWidth / imageDimension.getWidth());
            if (scale <= 1.5) {
                scale =1.5f;
            }

            Log.i("scale", String.valueOf(scale));
            matrix.postScale(scale*1.5f, scale*1.5f, centerX, centerY);

        }

            texture.setTransform(matrix);

    }

}
