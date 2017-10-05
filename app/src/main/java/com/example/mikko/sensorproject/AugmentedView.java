package com.example.mikko.sensorproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public class AugmentedView extends SurfaceView implements SurfaceHolder.Callback {

    private Paint paint;
    private SurfaceHolder mHolder;
    private Context context;
    private Bitmap bmp;

    private int mWidth;
    private int mLength;

    public AugmentedView(CameraFragment context) {
        super(context.getActivity().getBaseContext());
        //mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context.getActivity().getBaseContext();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
    }

    public AugmentedView(Context context, AttributeSet attr) {
        super(context, attr);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        getHolder().addCallback(this);
    }

    public AugmentedView(Context context) {
        super(context);
        getHolder().addCallback(this);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
 //       Canvas canvas = mHolder.lockCanvas();
 /*       if (canvas != null) {
            canvas.drawCircle(100, 100, 100, paint );
            mHolder.unlockCanvasAndPost(canvas);
        } */
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
       /* Canvas canvas = mHolder.lockCanvas();
        mLength = height;
        mWidth = width;
        if (canvas != null) {
            canvas.drawCircle(width/2, height/2, 100, paint );
            mHolder.unlockCanvasAndPost(canvas);
        } */

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


    public void drawStuff() {
        mHolder = getHolder();
        Canvas canvas = mHolder.lockCanvas();
        if (canvas != null) {
            canvas.drawCircle(mWidth/4, mLength/4, 100, paint );
            mHolder.unlockCanvasAndPost(canvas);
        }
    }
}
