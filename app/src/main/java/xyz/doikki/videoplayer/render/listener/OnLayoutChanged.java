package xyz.doikki.videoplayer.render.listener;

public interface OnLayoutChanged {

    /**
     * view 大小改变时回调
     * @param width 新的宽
     * @param height 新的高
     */
    void onLayout(int width, int height);
}
