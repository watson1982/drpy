package xyz.doikki.videoplayer.render;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import xyz.doikki.videoplayer.render.listener.OnLayoutChanged;

public class VideoSurfaceView extends SurfaceView {

    private MeasureHelper mMeasureHelper;

    /**
     * view的宽高固定时的回调
     */
    private OnLayoutChanged mOnLayoutChanged;

    public VideoSurfaceView(Context context) {
        this(context, null);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        //getHolder().addCallback(this);
        mMeasureHelper = new MeasureHelper();

    }

    public MeasureHelper getMeasureHelper() {
        return mMeasureHelper;
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int[] measuredSize = mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(measuredSize[0], measuredSize[1]);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && mOnLayoutChanged != null) {
            mOnLayoutChanged.onLayout(right - left, bottom - top);
        }
    }


    public void setOnLayoutChanged(OnLayoutChanged changed) {
        mOnLayoutChanged = changed;
    }

}
