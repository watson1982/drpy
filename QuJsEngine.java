package com.quickjs.android.example.js;

import android.text.TextUtils;
import android.util.Base64;

import com.lzy.okgo.OkGo;
import com.quickjs.android.example.App;
import com.quickjs.android.example.LOG;
import com.quickjs.android.example.LoadingEvent;
import com.quickjs.android.example.MD5;
import com.quickjs.android.example.SourceBean;
import com.quickjs.android.example.Spider;
import com.quickjs.android.example.SpiderNull;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class QuJsEngine {
    public static volatile QuJsEngine jsQuJsEngine;

    public ConcurrentHashMap<String, Spider> spiderMap = new ConcurrentHashMap<>();
    public LinkedHashMap<String, String> jsStrMap = new LinkedHashMap<>();
    public QuickJSContext quickJS;

    public static QuJsEngine getInstance() {
        if (jsQuJsEngine == null) {
            synchronized (QuJsEngine.class) {
                if (jsQuJsEngine == null) {
                    jsQuJsEngine = new QuJsEngine();
                }
            }
        }
        return jsQuJsEngine;
    }

    public QuickJSContext getContext() {
        return quickJS;
    }

    public static String getAsOpen(String name) {
        try {
            InputStream is = App.getInstance().getAssets().open(name);
            byte[] data = new byte[is.available()];
            is.read(data);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    public boolean isAsFile(String str, String str2) {
        try {
            for (String str3 : App.getInstance().getAssets().list(str2)) {
                if (str3.equals(str.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOG.e(e);
        }
        return false;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024*4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public void init() {
        try {
            if(quickJS == null ) quickJS = QuickJSContext.create();

             quickJS.setModuleLoader(new QuickJSContext.BytecodeModuleLoader() {
                @Override
                public byte[] getModuleBytecode(String moduleName) {
                    try {
                        String ss = loadModule(moduleName);
                        if(ss.startsWith("//bb")){
                            byte[] b = Base64.decode(ss.replace("//bb",""), 0);
                            return byteFF(b);
                        } else {
                            byte[] b = quickJS.compileModule(ss, moduleName);
                            //FileUtils.setCacheByte(MD5.encode(moduleName), b);
                            return b;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return new byte[0];
                }

                 @Override
                 public String getModuleStringCode(String moduleName) {
                     try {
                         return loadModule(moduleName);
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             });

            JSObject consoleObject = quickJS.createJSObject();
            quickJS.getGlobalObject().set("console", consoleObject);
            consoleObject.set("log", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    StringBuilder value = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        value.append(args[i]);
                        if (i < args.length - 1) {
                            value.append(" ");
                        }
                    }
                    LOG.i("QuJs", value.toString());
                    return null;
                }
            });

            quickJS.getGlobalObject().bind(new QuJsGlobal());

            JSObject localObject = quickJS.createJSObject();
            quickJS.getGlobalObject().set("local", localObject);
            localObject.bind(new QuJslocal());

            quickJS.evaluate(loadModule("net.js"), "");
        } catch (Throwable th) {
            LOG.e("iii",th);
        }
    }

    public Spider getSpider(SourceBean source) {
        try {
            if (!TextUtils.isEmpty(source.getExt())) {
                if (this.quickJS == null) {
                    spiderMap.clear();
                    init();
                }

                String key = "J" + MD5.encode(source.getKey());
                if (spiderMap.containsKey(key)) {
                    return spiderMap.get(key);
                }

                String str3 = "";
                String str4 = "";
                if (source.getApi().endsWith(".js")) {
                    str3 = loadModule(source.getApi());
                    if(source.getExt().startsWith("{")){
                        str4 = source.getExt();
                    } else {
                        str4 = loadModule(source.getExt());
                    }
                }
                if (TextUtils.isEmpty(str3)) {
                    EventBus.getDefault().post(new LoadingEvent("[api]不能为空", true));
                    return new SpiderNull();
                }

                if(str3.startsWith("//bb")){
                    byte[] b = Base64.decode(str3.replace("//bb",""), 0);
                    quickJS.execute(byteFF(b),  key +  ".js");
                    quickJS.evaluateModule("import {__jsEvalReturn} from '" + key + ".js';\n\nglobalThis." + key + " = __jsEvalReturn();\n\n"
                            + "console.log(typeof(pdfl));console.log(Object.keys(globalThis." + key + "));");

                    //LOG.e("222", "B>" + Arrays.toString(quickJS.getKeys(getContext().getGlobalObject())));
                } else {
                    if (str3.contains("__jsEvalReturn")) {
                        //quickJS.evaluate("req = http");
                        str3 = str3 + "\n\nglobalThis." + key + " = __jsEvalReturn()";
                    } else if (str3.contains("export default{") || str3.contains("export default {")) {
                        str3 = str3.replaceAll("export default.*?[{]", "globalThis." + key + " = {");
                    } else {
                        str3 = str3.replace("__JS_SPIDER__", "globalThis." + key);
                    }
                    quickJS.evaluateModule(str3 + "\n\n;console.log(typeof(pdfl));console.log(Object.keys(globalThis." + key + "));", source.getApi());

                    //LOG.e("222", "B>" + Arrays.toString(quickJS.getKeys(getContext().getGlobalObject())));
                }

                QuJsSpider jsspider = new QuJsSpider(key, quickJS);
                jsspider.init(App.getInstance(), str4);
                spiderMap.put(key, jsspider);
                return jsspider;
            } else {
                EventBus.getDefault().post(new LoadingEvent("[ext]不能为空", true));
                return new SpiderNull();
            }
        } catch (Throwable th) {
            LOG.e("111",th);
            return new SpiderNull();
        }
    }

    public static byte[] byteFF(byte[] bytes) {
        byte[] newBt = new byte[bytes.length - 4];
        newBt[0] = 1;
        System.arraycopy(bytes, 5, newBt, 1, bytes.length - 5);
        return newBt;
    }

    public static String getByteS(byte[] name) {
        StringBuilder r = new StringBuilder(" ");
        for(byte b : name){
            r.append(b);
        }
        return r.toString();
    }
    public static byte[] getCacheByte(String name) {
        try {
            InputStream is = App.getInstance().getAssets().open(name);
            byte[] data = new byte[is.available()];
            is.read(data);
            return data;
        } catch (Exception e) {
            LOG.e(e);
            return new byte[0];
        }
    }
    public String loadModule(String str) throws IOException {
        if (str.endsWith("ali.js")) {
            str = "ali.js";
        } else if (str.endsWith("ali_api.js")) {
            str = "ali_api.js";
        } else if (str.contains("utils.js")) {
            str = "utils.js";
        } else if (str.contains("similarity.js")) {
            str = "similarity.js";
        } else if (str.contains("cat.js")) {
            str = "cat.js";
        } else if (str.contains("cta.js")) {
            str = "cta.js";
        } else if (str.contains("cheerio.min.js")) {
            str = "cheerio.min.js";
        } else if (str.contains("crypto-js.js")) {
            str = "crypto-js.js";
        } else if (str.contains("gbk.js")) {
            str = "gbk.js";
        } else if (str.contains("模板.js")) {
            str = "模板.js";
        }
        if (str.startsWith("http")) {
            return OkGo.<String>get(str).headers("User-Agent", "Mozilla/5.0").execute().body().string();
        }

        if (str.startsWith("assets://")) {
            return getAsOpen(str.substring(9));
        } else if (isAsFile(str, "js/lib")) {
            return getAsOpen("js/lib/" + str);
        }
        return str;
    }

}