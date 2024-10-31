package com.example.myreader;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
//import android.widget.ImageView;

public class ZoomImageView extends androidx.appcompat.widget.AppCompatImageView{
    private Matrix matrix = new Matrix();

    //States
    private enum State { NONE, DRAG, ZOOM }
    private State state = State.NONE;

    //Remember some things for zooming
    private float minScale = 1f ;
    private float maxScale = 5f;
    private float[] m;

    //For touch events
    private float lastX = -1f;
    private float lastY = -1f;
    private int pointerCount = 0;

    //For pinch zooming
    private float startDistance = 0f;

    public ZoomImageView(Context context){
        super(context);
        init();
    }

    public ZoomImageView(Context context, AttributeSet attribute){
        super(context, attribute);
        init();
    }

    public ZoomImageView(Context context, AttributeSet attribute, int defStyle){
        super(context, attribute, defStyle);
        init();
    }

    public void init(){
        super.setClickable(true);
        m = new float[9];
        matrix.setTranslate(1f, 1f);
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motion){
        switch(motion.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                state = State.DRAG;
                lastX = motion.getX();
                lastY = motion.getY();
                pointerCount = 1;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                state = State.ZOOM;
                startDistance = spacing(motion);
                break;

            case MotionEvent.ACTION_MOVE:
                if(state == State.DRAG && pointerCount == 1){
                    float dx = motion.getX() - lastX;
                    float dy = motion.getY() - lastY;
                    matrix.postTranslate(dx, dy);
                    lastX = motion.getX();
                    lastY = motion.getY();
                }else if(state == State.ZOOM && pointerCount >= 2){
                    float newDistance = spacing(motion);
                    if(newDistance > 10f){
                        float scale = newDistance / startDistance;
                        startDistance = newDistance;
                        float[] values = new float[9];
                        matrix.getValues(values);
                        float currentScale = values[Matrix.MSCALE_X];
                        if((scale > 1f && currentScale < maxScale) || (scale < 1f && currentScale > minScale)){
                            matrix.postScale(scale, scale, getWidth()/2f, getHeight()/2f);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                state = State.NONE;
                pointerCount = motion.getPointerCount();
                break;
        }
        setImageMatrix(matrix);
        return true;
    }

    private float spacing(MotionEvent motion){
        if(motion.getPointerCount() < 2){
            return 0;
        }
        float x = motion.getX(0) - motion.getX(1);
        float y = motion.getY(0) - motion.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
}
