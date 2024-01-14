package com.github.tvbox.osc.util;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.reflect.Reflect;
import com.github.tvbox.osc.server.ControlManager;

import java.io.File;

import dalvik.system.DexClassLoader;

public class PyUtil {
    static DexClassLoader classLoader;

    public static String getProxy(boolean local) {
        return ControlManager.get().getAddress(local) + "proxy?do=py";
    }

    public static void load(String jar) {
        File cacheDir = new File(App.getInstance().getCacheDir().getAbsolutePath() + "/pycache");
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        classLoader = new DexClassLoader(jar, cacheDir.getAbsolutePath(), null, App.getInstance().getClassLoader());
        // make force wait here, some device async dex load
    }

    public static String call(String className, String name, Object... obj) {
        try {
            if(obj.length > 0){
                return Reflect.onClass(classLoader.loadClass(className)).create().call(name, obj).get();
            }
            return Reflect.onClass(classLoader.loadClass(className)).create().call(name).get();
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

}
