package xyz.doikki.videoplayer.render;

import android.content.Context;

import xyz.doikki.videoplayer.render.effect.ColorAdjustUtils;

public class SurfaceRenderViewFactory extends RenderViewFactory {

    public static SurfaceRenderViewFactory create() {
        return new SurfaceRenderViewFactory();
    }

    @Override
    public IRenderView createRenderView(Context context) {
        IRenderProcess mRenderProcess = RenderSdk.createRenderProcess();

        mRenderProcess.addEffect("{\n" +
                "    \"effect\":[\n" +
                "        {\n" +
                "            \"type\":\"background\",\n" +
                "            \"backgroundType\":1,\n" +
                "            \"blur\":10,\n" +
                "            \"renderFrameType\":0,\n" +
                "            \"z_order\":1\n" +
                "        }\n" +
                "    ]\n" +
                "}");
        String highStr = ColorAdjustUtils.getColorEffect(10, 0, 5, 20, 0, 10);
        mRenderProcess.addEffect(highStr);//一键高清

        mRenderProcess.setSurfaceView(new VideoSurfaceView(context));
        return mRenderProcess;
        //return new SurfaceRenderView(context);
    }
}