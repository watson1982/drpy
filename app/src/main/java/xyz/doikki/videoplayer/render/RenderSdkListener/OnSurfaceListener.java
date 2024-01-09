package xyz.doikki.videoplayer.render.RenderSdkListener;

import android.view.Surface;

public interface OnSurfaceListener {

    void onSurfaceCreated(Surface surface);

    void onSurfaceChanged(int width, int height);

    void onSurfaceDestroy();
}
