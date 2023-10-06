package com.github.tvbox.osc.base;

import android.app.Activity;
import androidx.multidex.MultiDexApplication;
import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.js.jianpian;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.kingja.loadsir.core.LoadSir;
import com.jieba_android.JiebaSegmenter;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;

import java.util.ArrayList;
import java.util.HashMap;

import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;
    private Activity homeActivity;
    private static P2PClass p;
    public static String burl;
    private static String dashData;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initParams();
        // OKGo
        OkGoHelper.init();

        // 初始化Web服务器
        ControlManager.init(this);
        //初始化数据库
        AppDataManager.init();
        LoadSir.beginBuilder()
            .addCallback(new EmptyCallback())
            .addCallback(new LoadingCallback())
            .commit();
        AutoSizeConfig.getInstance()
            .setCustomFragment(true)
            .getUnitsManager()
            .setSupportDP(false)
            .setSupportSP(false)
            .setSupportSubunits(Subunits.MM);
        PlayerHelper.init();
        QuickJSLoader.init();
        FileUtils.cleanPlayerCache();
        JiebaSegmenter.init(instance.getApplicationContext());
    }

    private void initParams() {
        // Hawk
        Hawk.init(this)
            .build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);
        
        String defaultApiName = "默认-自备份线路";
        String defaultApi = "";
        
        HashMap<String, String> defaultApiMap = Hawk.get(HawkConfig.API_MAP, new HashMap<>());
        defaultApiMap.put(defaultApiName, defaultApi);

        ArrayList<String> defaultApiHistory = Hawk.get(HawkConfig.API_NAME_HISTORY, new ArrayList<>());
        defaultApiHistory.add(defaultApiName);
        
        putDefault(HawkConfig.API_URL, defaultApi);
        putDefault(HawkConfig.API_NAME, defaultApiName);
        putDefault(HawkConfig.API_NAME_HISTORY, defaultApiHistory);
        putDefault(HawkConfig.API_MAP, defaultApiMap);

        //        putDefault(HawkConfig.HOME_REC, 1);       // Home Rec 0=豆瓣, 1=推荐, 2=历史
        putDefault(HawkConfig.PLAY_TYPE, 1); // Player   0=系统, 1=IJK, 2=Exo
        putDefault(HawkConfig.IJK_CODEC, "硬解码"); // IJK Render 软解码, 硬解码
        putDefault(HawkConfig.PLAY_RENDER, 1); // 渲染 0=text, 1=surface
        //        putDefault(HawkConfig.DOH_URL, 2);        // DNS
        //        putDefault(HawkConfig.SEARCH_VIEW, 1);    // Text or Picture
    }

    public Activity getHomeActivity() {
        return homeActivity;
    }

    public void setHomeActivity(Activity homeActivity) {
        this.homeActivity = homeActivity;
    }

    private void putDefault(String key, Object value) {
        if (!Hawk.contains(key)) {
            Hawk.put(key, value);
        }
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsLoader.stopAll();
        jianpian.finish();
    }

    public static P2PClass getp2p() {
        try {
            if (p == null) {
                p = new P2PClass(instance.getCacheDir()
                    .getAbsolutePath());
            }
            return p;
        } catch (Exception unused) {
            return null;
        }
    }

    public void setDashData(String data) {
        dashData = data;
    }
    public String getDashData() {
        return dashData;
    }
}