package com.example.mikko.sensorproject.utils;

import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.mikko.sensorproject.interfaces.DragInterface;


public class DragUtils {
    //saves where last point is when user lets off of screen
    public float lastY, lastX;

    public boolean down = false;
    public void setupViewDrag(View v, final DragInterface intterfeis) {
        v.setOnTouchListener(new View.OnTouchListener() {


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                float posY = 0;
                float posX = 0;

                switch (action) {
                    //when finger is down on the screen
                    case (MotionEvent.ACTION_DOWN): {
                        down = true;
                        int pointer = MotionEventCompat.getActionIndex(event);
                        float y = event.getRawY();
                        float x = event.getRawX();

                        //activePointer = MotionEventCompat.getPointerId(event, 0);
                        Log.d("asdadad", "on action up: " + event.toString());
                        System.out.println("asdasdadadf "+event.getRawX()+", "+event.getRawY());
                        
                        //save the point where finger touched the screen
                        lastY = y;
                        lastX = x;
                        break;
                    }

                    //starts sending data via interface when user drags in any direction
                    case (MotionEvent.ACTION_MOVE): {
                       // int pointer = MotionEventCompat.findPointerIndex(event, activePointer);
                        //getRaw is more accurate
                        float y = event.getRawY();
                        float x = event.getRawX();
                        float distanceMoved = y - lastY;
                        float distanceMovedX = x - lastX;

                        posY += distanceMoved;
                        posX += distanceMovedX;
                        //System.out.println("distane moved: " + posY);
                        
                        //send data to main activity
                        intterfeis.dragData(posX,posY);

                        lastY = y;
                        lastX = x;
                        //texture.setAspectRatio(1,1);
                        //texture.measure(500,500);
                        break;

                    }
                    case (MotionEvent.ACTION_UP): {
                        down = false;
                        //activePointer = MotionEvent.INVALID_POINTER_ID;
                    }


                    default: {
                        return false;
                    }
                }
                return true;
            }

        });
    }
}
