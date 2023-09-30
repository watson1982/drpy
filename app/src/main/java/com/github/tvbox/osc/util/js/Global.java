package com.github.tvbox.osc.util.js;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.github.tvbox.osc.server.ControlManager;
import com.whl.quickjs.wrapper.ContextSetter;
import com.whl.quickjs.wrapper.Function;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.JSUtils;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Global {
    private QuickJSContext runtime;
    public ExecutorService executor;
    private final Timer timer;

    public Global(ExecutorService executor) {
        this.executor = executor;
        this.timer = new Timer();
    }

    @Keep
    @Function
    public String getProxy(boolean local) {
        return ControlManager.get().getAddress(local) + "proxy?do=js";
    }

    @Keep
    @Function
    public String joinUrl(String parent, String child) {
        return HtmlParser.joinUrl(parent, child);
    }

    @Keep
    @Function
    public String pd(String html, String rule, String add_url) {
        return HtmlParser.parseDomForUrl(html, rule, add_url);
    }

    @Keep
    @Function
    public String pdfh(String html, String rule) {
        return HtmlParser.parseDomForUrl(html, rule, "");
    }

    @Keep
    @Function
    public JSArray pdfa(String html, String rule) {

        return new JSUtils<String>().toArray(runtime, HtmlParser.parseDomForArray(html, rule));
    }

    @Keep
    @Function
    public JSArray pdfla(String html, String p1, String list_text, String list_url, String add_url) {
        return new JSUtils<String>().toArray(runtime, HtmlParser.parseDomForList(html, p1, list_text, list_url, add_url));
    }

    private JSObject req(String url, JSObject options) {
        try {
            Req req = Req.objectFrom(options.toJsonString());
            Response res = Connect.to(url, req).execute();
            return Connect.success(runtime, req, res);
        } catch (Exception e) {
            return Connect.error(runtime);
        }
    }

    @Keep
    @Function
    public JSObject _http(String url, JSObject options) {
        JSFunction complete = options.getJSFunction("complete");
        if (complete == null) return req(url, options);
        Req req = Req.objectFrom(options.toJsonString());
        Connect.to(url, req).enqueue(getCallback(complete, req));
        return null;
    }

    @Keep
    @Function
    public void setTimeout(JSFunction func, Integer delay) {
        func.hold();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!executor.isShutdown()) executor.submit(() -> {func.call();});
            }
        }, delay);
    }

    private Callback getCallback(JSFunction complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                executor.submit(() -> {
                    complete.call(Connect.success(runtime, req, res));
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                executor.submit(() -> {
                    complete.call(Connect.error(runtime));
                });
            }
        };
    }
    @Keep
    // 声明用于依赖注入的 QuickJSContext
    @ContextSetter
    public void setJSContext(QuickJSContext runtime) {
        this.runtime = runtime;
    }

}