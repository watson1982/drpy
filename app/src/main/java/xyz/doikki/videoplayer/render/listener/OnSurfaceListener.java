package xyz.doikki.videoplayer.render.listener;

import android.view.Surface;

public interface OnSurfaceListener {

    void onSurfaceCreated(Surface surface);

    void onSurfaceChanged(int width, int height);

    void onSurfaceDestroy();
}
