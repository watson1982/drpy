package com.github.catvod.crawler;

import android.util.Log;

import com.github.tvbox.osc.event.LogEvent;

import org.greenrobot.eventbus.EventBus;

public class SpiderDebug {
    public static void log(Throwable th) {
        try {
            android.util.Log.d("SpiderLog", th.getMessage(), th);
            EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", "SpiderLog") + Log.getStackTraceString(th)));
        } catch (Throwable th1) {
            th1.printStackTrace();
        }
    }

    public static void log(String msg) {
        try {
            android.util.Log.d("SpiderLog", msg);
            EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", "SpiderLog") + msg));
        } catch (Throwable th1) {
            th1.printStackTrace();
        }
    }

    public static String ec(int i) {
        return "";
    }
}
