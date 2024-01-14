package com.undcover.freedom.pyramid;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PythonSpider extends Spider {
    private PyObject pyApp;
    private PyObject pySpider;
    private String name;
    private String cachePath;
    private String extInfo;


    public PythonSpider(PyObject pyApp, String name, String cache, String ext) {
        this.pyApp = pyApp;
        this.cachePath = cache;
        this.name = name;
        this.extInfo = ext;
    }

    public void init(Context context, String url) {
        PyObject retValue = pyApp.callAttr("downloadPlugin", cachePath, url);

        if (null == extInfo) extInfo = "";
        String path = retValue.toString();
        File file = new File(path);
        if (file.exists()) {
            pySpider = pyApp.callAttr("loadFromDisk", path);

            List<PyObject> poList = pyApp.callAttr("getDependence", pySpider).asList();
            for (PyObject po : poList) {
                String api = po.toString();
                String depUrl = url.substring(0, url.lastIndexOf(47) + 1) + api + ".py";
                String tmpPath = pyApp.callAttr("downloadPlugin", cachePath, depUrl).toString();
                if (!new File(tmpPath).exists()) {
                    PyLog.d(api + "加载插件依赖失败!");
                    //return;
                } else {
                    PyLog.d(api + ": 加载插件依赖成功！");
                }
            }
            pyApp.callAttr("init", pySpider, extInfo);
            PyLog.d(name + ": 下載插件成功！");
        } else {
            PyLog.d(name + "下载插件失败");
        }
    }

    public JSONObject map2json(HashMap<String, String> extend) {
        JSONObject jo = new JSONObject();
        try {
            if (extend != null) {
                for (String key : extend.keySet()) {
                    jo.put(key, extend.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    public JSONObject map2json(Map extend) {
        JSONObject jo = new JSONObject();
        try {
            if (extend != null) {
                for (Object key : extend.keySet()) {
                    jo.put(key.toString(), extend.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    public JSONArray list2json(List<String> array) {
        JSONArray ja = new JSONArray();
        if (array != null) {
            for (String str : array) {
                ja.put(str);
            }
        }
        return ja;
    }

    public String paramLog(Object... obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("request params:[");
        for (Object o : obj) {
            sb.append(o).append("-");
        }
        sb.append("]");
        return sb.toString();
    }

    public Object[] proxyLocal(Map params) {
        PyLog.nw("localProxy", map2json(params).toString());
        List<PyObject> poList = pyApp.callAttr("localProxy", pySpider, map2json(params).toString()).asList();

        Map<PyObject, PyObject> headers = null;
        if (poList.size() > 3) {
            headers = poList.get(3).asMap();
        }
        int code = poList.get(0).toInt();
        String type = poList.get(1).toString();

        PyObject r2 = poList.get(2);
        if (r2 == null){
            return new Object[]{code, type, null, headers};
        }
        LOG.e("PyType", poList.get(2).type().toString());
        if (r2.type().toString().contains("str")) {
            return new Object[]{code, type, new ByteArrayInputStream(replaceLocalUrl(r2.toString()).getBytes()), headers};
        } else if(r2.type().toString().contains("bytes")){
            return new Object[]{code, type, new ByteArrayInputStream(r2.toJava(byte[].class)), headers};
        }
        LOG.e("不支持此类型:" + poList.get(2).type());
        return new Object[]{code, type, null, headers};
    }

    public String replaceLocalUrl(String content) {
        return content.replace("http://127.0.0.1:UndCover/proxy", PythonLoader.getInstance().localProxyUrl());
    }

    /**
     * 首页数据内容
     *
     * @param filter 是否开启筛选
     * @return
     */
    public String homeContent(boolean filter) {
        PyLog.nw("homeContent" + "-" + name, paramLog(filter));
        PyObject po = pyApp.callAttr("homeContent", pySpider, filter);
        String rsp = po.toString();
        PyLog.nw("homeContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 首页最近更新数据 如果上面的homeContent中不包含首页最近更新视频的数据 可以使用这个接口返回
     *
     * @return
     */
    public String homeVideoContent() {
        PyLog.nw("homeVideoContent" + "-" + name, "");
        PyObject po = pyApp.callAttr("homeVideoContent", pySpider);
        String rsp = po.toString();
        PyLog.nw("homeVideoContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 分类数据
     *
     * @param tid
     * @param pg
     * @param filter
     * @param extend
     * @return
     */
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        PyLog.nw("categoryContent" + "-" + name, paramLog(tid, pg, filter, map2json(extend).toString()));
        PyObject po = pyApp.callAttr("categoryContent", pySpider, tid, pg, filter, map2json(extend).toString());
        String rsp = po.toString();
        PyLog.nw("categoryContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 详情数据
     *
     * @param ids
     * @return
     */
    public String detailContent(List<String> ids) {
        PyLog.nw("detailContent" + "-" + name, paramLog(list2json(ids).toString()));
        PyObject po = pyApp.callAttr("detailContent", pySpider, list2json(ids).toString());
        String rsp = po.toString();
        PyLog.nw("detailContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 搜索数据内容
     *
     * @param key
     * @param quick
     * @return
     */
    public String searchContent(String key, boolean quick) {
        PyLog.nw("searchContent" + "-" + name, paramLog(key, quick));
        PyObject po = pyApp.callAttr("searchContent", pySpider, key, quick);
        String rsp = po.toString();
        PyLog.nw("searchContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * 播放信息
     *
     * @param flag
     * @param id
     * @return
     */
    public String playerContent(String flag, String id, List<String> vipFlags) {
        PyLog.nw("playerContent" + "-" + name, paramLog(flag, id, list2json(vipFlags).toString()));
        PyObject po = pyApp.callAttr("playerContent", pySpider, flag, id, list2json(vipFlags).toString());
        String rsp = replaceLocalUrl(po.toString());
        PyLog.nw("playerContent" + "-" + name, rsp);
        return rsp;
    }

    /**
     * webview解析时使用 可自定义判断当前加载的 url 是否是视频
     *
     * @param url
     * @return
     */
    public boolean isVideoFormat(String url) {
        PyLog.nw("isVideoFormat" + "-" + name, url);
        PyObject po = pyApp.callAttr("isVideoFormat", pySpider, url);
        boolean rsp = po.toBoolean();
        PyLog.nw("isVideoFormat" + "-" + name, rsp + "");
        return rsp;
    }

    /**
     * 是否手动检测webview中加载的url
     *
     * @return
     */
    public boolean manualVideoCheck() {
        PyObject po = pyApp.callAttr("manualVideoCheck", pySpider);
        boolean rsp = po.toBoolean();
        PyLog.nw("manualVideoCheck" + "-" + name, rsp + "");
        return rsp;
    }

}
