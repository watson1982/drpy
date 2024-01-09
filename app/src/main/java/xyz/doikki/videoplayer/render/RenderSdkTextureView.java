package xyz.doikki.videoplayer.render;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.TextureView;

@SuppressLint("ViewConstructor")
public class RenderSdkTextureView extends TextureView {
    private MeasureHelper mMeasureHelper;

    public RenderSdkTextureView(Context context) {
        super(context);
    }

    {
        mMeasureHelper = new MeasureHelper();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int[] measuredSize = mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(measuredSize[0], measuredSize[1]);
    }

    public MeasureHelper getMeasureHelper() {
        return mMeasureHelper;
    }
}