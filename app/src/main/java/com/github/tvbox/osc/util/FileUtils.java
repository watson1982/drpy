package com.github.tvbox.osc.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.server.ControlManager;

import com.github.tvbox.osc.util.urlhttp.OkHttpUtil;
import com.quickjs.android.JSUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.UUID;

public class FileUtils {

    public static File open(String str) {
        return new File(App.getInstance().getExternalCacheDir().getAbsolutePath() + "/qjscache_" + str + ".js");
    }
    public static String genUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
    public static String getCache(String name) {
        try {
            String code = "";
            File file = open(name);
            if (file.exists()) {
                code = new String(readSimple(file));
            }
            if (TextUtils.isEmpty(code)) {
                return "";
            }
            JsonObject asJsonObject = (new Gson().fromJson(code, JsonObject.class)).getAsJsonObject();
            if (((long) asJsonObject.get("expires").getAsInt()) > System.currentTimeMillis() / 1000) {
                return asJsonObject.get("data").getAsString();
            }
            recursiveDelete(open(name));
            return "";
        } catch (Exception e4) {
            return "";
        }
    }

    public static void setCache(int time, String name, String data) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("expires", (int) (time + (System.currentTimeMillis() / 1000)));
            jSONObject.put("data", data);
            writeSimple(jSONObject.toString().getBytes(), open(name));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadJs(String name) {
        try {
            if (name.startsWith("http://") || name.startsWith("https://")) {
                String cache = getCache(MD5.encode(name));
                if (JSUtils.isEmpty(cache)) {
                    String netStr = OkHttpUtil.get(name);
                    if (!TextUtils.isEmpty(netStr)) {
                        setCache(604800, MD5.encode(name), netStr);
                    }
                    return netStr;
                }
                return ControlManager.get().getAddress(true) + "/proxy?do=ext&txt=" + Base64.encodeToString(cache.getBytes(), Base64.URL_SAFE);
                //return cache;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return name;
        }
        return name;
    }

    public static String loadModule(String name) {
        try {
            if(name.contains("cheerio.min.js")){
                name = "cheerio.min.js";
            } else if(name.contains("crypto-js.js")){
                name = "crypto-js.js";
            } else if(name.contains("dayjs.min.js")){
                name = "dayjs.min.js";
            } else if(name.contains("uri.min.js")){
                name = "uri.min.js";
            } else if(name.contains("underscore-esm-min.js")){
                name = "underscore-esm-min.js";
            }
            if (name.startsWith("http://") || name.startsWith("https://")) {
                String cache = getCache(MD5.encode(name));
                if (JSUtils.isEmpty(cache)) {
                    String netStr = OkHttpUtil.get(name);
                    if (!TextUtils.isEmpty(netStr)) {
                        setCache(604800, MD5.encode(name), netStr);
                    }
                    return netStr;
                }
                return cache;
            } else if (name.startsWith("assets://")) {
                return getAsOpen(name.substring(9));
            } else if (isAsFile(name, "js/lib")) {
                return getAsOpen("js/lib/" + name);
            } else if (name.startsWith("file://")) {
                return OkHttpUtil.get(ControlManager.get().getAddress(true) + "file/" + name.replace("file:///", "").replace("file://", ""));
            } else if (name.startsWith("clan://localhost/")) {
                return OkHttpUtil.get(ControlManager.get().getAddress(true) + "file/" + name.replace("clan://localhost/", ""));
            } else if (name.startsWith("clan://")) {
                String substring = name.substring(7);
                int indexOf = substring.indexOf(47);
                return OkHttpUtil.get("http://" + substring.substring(0, indexOf) + "/file/" + substring.substring(indexOf + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return name;
        }
        return name;
    }

    public static boolean isAsFile(String name, String path) {
        try {
            for (String fname : App.getInstance().getAssets().list(path)) {
                if (fname.equals(name.trim())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getAsOpen(String name) {
        try {
            InputStream is = App.getInstance().getAssets().open(name);
            byte[] data = new byte[is.available()];
            is.read(data);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    public static boolean writeSimple(byte[] data, File dst) {
        try {
            if (dst.exists())
                dst.delete();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dst));
            bos.write(data);
            bos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] readSimple(File src) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
            int len = bis.available();
            byte[] data = new byte[len];
            bis.read(data);
            bis.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
    public static String getRootPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static File getLocal(String path) {
        return new File(path.replace("file:/", getRootPath()));
    }

    public static File getCacheDir() {
        return App.getInstance().getCacheDir();
    }
    public static File getExternalCacheDir() {
        return App.getInstance().getExternalCacheDir();
    }
    public static String getExternalCachePath() {
        return getExternalCacheDir().getAbsolutePath();
    }
    public static String getCachePath() {
        return getCacheDir().getAbsolutePath();
    }
    public static void recursiveDelete(File file) {
        if (!file.exists())
            return;
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                recursiveDelete(f);
            }
        }
        file.delete();
    }

    /**
     * 获取缓存大小
     * @param context
     * @return
     * @throws Exception
     */
    public static String getTotalCacheSize(Context context) {
        long cacheSize = getFolderSize(context.getCacheDir());
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cacheSize += getFolderSize(context.getExternalCacheDir());
        }
        return getFormatSize(cacheSize);
    }

    // 获取文件
    //Context.getExternalFilesDir() --> SDCard/Android/data/你的应用的包名/files/ 目录，一般放一些长时间保存的数据
    //Context.getExternalCacheDir() --> SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
    public static long getFolderSize(File file) {
        long size = 0;
        try {
            File[] fileList = file.listFiles();
            for (File value : fileList) {
                // 如果下面还有文件
                if (value.isDirectory()) {
                    size = size + getFolderSize(value);
                } else {
                    size = size + value.length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 格式化单位
     *
     * @param size
     * @return
     */
    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
//            return size + "Byte";
            return "0K";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "TB";
    }
    /***
     * 清理所有缓存
     */
    public static void clearAllCache() {
        deleteDir(getCacheDir());
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            deleteDir(getExternalCacheDir());
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
