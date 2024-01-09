package xyz.doikki.videoplayer.render;

import android.content.Context;

import xyz.doikki.videoplayer.render.RenderSdkEffect.ColorAdjustUtils;

public class RenderSdkSurfaceFactory extends RenderViewFactory {

    public static RenderSdkSurfaceFactory create() {
        return new RenderSdkSurfaceFactory();
    }

    @Override
    public IRenderView createRenderView(Context context) {
        IRenderSdkProcess mRenderProcess = RenderSdk.createRenderProcess();

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

        mRenderProcess.setSurfaceView(new RenderSdkSurfaceView(context));
        return mRenderProcess;
    }
}