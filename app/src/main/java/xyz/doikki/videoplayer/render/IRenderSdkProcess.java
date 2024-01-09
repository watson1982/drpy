package xyz.doikki.videoplayer.render;

import xyz.doikki.videoplayer.render.RenderSdkListener.OnCaptureListener;
import xyz.doikki.videoplayer.render.RenderSdkListener.OnRenderListener;
import xyz.doikki.videoplayer.render.RenderSdkListener.OnSurfaceListener;


public interface IRenderSdkProcess extends IRenderView {
    void setMeasureHelper(MeasureHelper measureHelper);

    void setOnSurfaceListener(OnSurfaceListener listener);

    void setOnRenderListener(OnRenderListener listener);

    void setTextureView(RenderSdkTextureView view);

    void setSurfaceView(RenderSdkSurfaceView view);

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

    void setMirror(RenderSdkMirrorType type);

    RenderSdkMirrorType getMirrorType();

    void destroy();
}
