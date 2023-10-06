package com.github.tvbox.osc.util.js;

import android.net.Uri;
import android.text.TextUtils;

import com.github.tvbox.osc.base.App;
import com.p2p.P2PClass;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class jianpian {
    public static String JPUrlDec(String url) {
        if (App.getp2p() != null) {
            try {
                String[] split = URLDecoder.decode(url, "UTF-8")
                    .split("\\|");
                String replace = split[0].replace("xg://", "ftp://");
                if (replace.contains("xgplay://")) {
                    replace = split[0].replace("xgplay://", "ftp://");
                }
                if (TextUtils.isEmpty(App.burl)) {
                    App.getp2p()
                        .P2Pdoxstart(replace.getBytes("GBK"));
                    App.getp2p()
                        .P2Pdoxadd(replace.getBytes("GBK"));
                } else if (replace.equals(App.burl)) {
                    App.getp2p()
                        .P2Pdoxstart(replace.getBytes("GBK"));
                } else {
                    App.getp2p()
                        .P2Pdoxpause(App.burl.getBytes("GBK"));
                    App.getp2p()
                        .P2Pdoxdel(App.burl.getBytes("GBK"));
                    App.getp2p()
                        .P2Pdoxstart(replace.getBytes("GBK"));
                    App.getp2p()
                        .P2Pdoxadd(replace.getBytes("GBK"));
                }
                App.burl = replace;
                return "http://" + App.getp2p()
                    .getLocalAddress() + ":" + P2PClass.port + "/" + URLEncoder.encode(Uri.parse(replace)
                    .getLastPathSegment(), "GBK");
            } catch (Exception e) {
                return e.getLocalizedMessage();
            }
        }
        return "";
    }

    public static void pause() {
        if (TextUtils.isEmpty(App.burl) || App.getp2p() == null) {
            return;
        }
        try {
            App.getp2p()
                .P2Pdoxpause(App.burl.getBytes("GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void finish() {
        if (TextUtils.isEmpty(App.burl) || App.getp2p() == null) {
            return;
        }
        try {
            App.getp2p()
                .P2Pdoxpause(App.burl.getBytes("GBK"));
            App.getp2p()
                .P2Pdoxdel(App.burl.getBytes("GBK"));
            App.burl = "";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}