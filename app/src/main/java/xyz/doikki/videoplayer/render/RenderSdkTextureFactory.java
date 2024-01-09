package xyz.doikki.videoplayer.render;

import android.content.Context;

import xyz.doikki.videoplayer.render.RenderSdkEffect.ColorAdjustUtils;

public class RenderSdkTextureFactory extends RenderViewFactory {

    public static RenderSdkTextureFactory create() {
        return new RenderSdkTextureFactory();
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
        mRenderProcess.addEffect(highStr);

        mRenderProcess.setTextureView(new RenderSdkTextureView(context));
        return mRenderProcess;
    }
}
