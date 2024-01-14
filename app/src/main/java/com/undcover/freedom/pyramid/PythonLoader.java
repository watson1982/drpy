package com.undcover.freedom.pyramid;

import android.app.Application;
import android.util.Base64;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.TxtSubscribe;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PythonLoader {
    private static volatile PythonLoader sInstance;
    private Python pyInstance;
    private PyObject pyApp;
    private String cache;
    private int port = -1;
    private static ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
    private Application app;

    public PythonLoader() {}

    public void load() {
        spiders.clear();
    }

    public static PythonLoader getInstance() {
        if (sInstance == null) {
            synchronized (PythonLoader.class) {
                if (sInstance == null) {
                    sInstance = new PythonLoader();
                }
            }
        }
        return sInstance;
    }

    public PythonLoader setApplication(Application app) {
        this.app = app;
        cache = app.getExternalCacheDir().getAbsolutePath() + "/pycache/";
        PyLog.getInstance().setLogLevel(5).setFilter(PyLog.FILTER_NW | PyLog.FILTER_LC);
        PyLog.TagConstant.TAG_APP = "PythonLoader";
        if (pyInstance == null) {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(app));
            }
            pyInstance = Python.getInstance();
            pyApp = pyInstance.getModule("app");
        }
        return this;
    }

    private volatile String recentJarKey = "";

    public Spider getSpider(String key, String url, String ext) throws Exception {
        if (app == null) throw new Exception("set application first");
        if (spiders.containsKey(key)) {
            PyLog.d(key + " :缓存加载成功！");
            return spiders.get(key);
        }
        recentJarKey = key;
        try {
            Spider sp = new PythonSpider(pyApp, key, cache, ext);
            sp.init(app, url);
            spiders.put(key, sp);
            return sp;
        } catch (Throwable th) {
            PyLog.e(th.toString());
        }
        return new Spider() {
        };
    }

    public Object[] proxyLocal(Map<String, String> map) {
        try {
            Spider proxyFun = spiders.get(recentJarKey);
            if (proxyFun != null) {
                return proxyFun.proxyLocal(map);
            }
        } catch (Throwable th) {
            LOG.e("proxyInvoke", th);
        }

        return new Object[]{};
    }

    public void getPort() {
        if (port <= 0) {
            for (int i = 9978; i < 10000; i++) {
                if (OkHttp.string("http://127.0.0.1:" + i + "/proxy?do=ck&api=python").equals("ok")) {
                    port = i;
                    return;
                }
            }
        }
    }

    public String localProxyUrl() {
        getPort();
        return "http://127.0.0.1:" + port + "/proxy";
    }

}
