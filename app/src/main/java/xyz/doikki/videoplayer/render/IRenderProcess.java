package xyz.doikki.videoplayer.render;

import xyz.doikki.videoplayer.render.listener.OnCaptureListener;
import xyz.doikki.videoplayer.render.listener.OnRenderListener;
import xyz.doikki.videoplayer.render.listener.OnSurfaceListener;


public interface IRenderProcess extends IRenderView {
    void setMeasureHelper(MeasureHelper measureHelper);

    void setOnSurfaceListener(OnSurfaceListener listener);

    void setOnRenderListener(OnRenderListener listener);

    void setTextureView(VideoTextureView view);

    void setSurfaceView(VideoSurfaceView view);

    void setVideoWH(int width, int height);

    int addEffect(String config);

    void updateEffect(int id, String config);

    void deleteEffect(int id);

    int addFilter(String config);

    void updateFilter(int id, String config);

    void updateFilterIntensity(int id, int intensity);

    void deleteFilter(int id);

    void updateFrame();

    void captureFrame(OnCaptureListener listener);

    void setMirror(MirrorType type);

    MirrorType getMirrorType();

    void destroy();
}
