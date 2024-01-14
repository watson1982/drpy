package com.github.tvbox.osc.server;

import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.event.InputMsgEvent;
import com.github.tvbox.osc.event.LogEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.LocalIPAddress;
import com.github.tvbox.osc.util.OkGoHelper;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.github.tvbox.osc.util.StringUtils;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.nanohttpd.protocols.http.IHTTPSession;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
public class RemoteServer extends NanoHTTPD {
    private Context mContext;
    public static int serverPort = 9978;
    private int timeout;
    private boolean isStarted = false;
    private DataReceiver mDataReceiver;
    private ArrayList<RequestProcess> getRequestList = new ArrayList<>();
    private ArrayList<RequestProcess> postRequestList = new ArrayList<>();
    private static final Pattern p = Pattern.compile("[ |\t]*(boundary[ |\t]*=[ |\t]*['|\"]?[^\"^'^;^,]*['|\"]?)", Pattern.CASE_INSENSITIVE);
    /**
     * 当前websocket 连接
     */
    private final AtomicReference<WebSocket> wsReference = new AtomicReference<>();
    private Timer timer;
    private Timer getTimer(){
        // 懒加载
        if(timer == null){
            timer = new Timer(true);
        }
        return timer;
    }
    /**
     * 不支持跨域请求(CORS)
     */
    private boolean noCORS = false;
    private static final String ALLOW_METHODS = Joiner.on(',').join(Arrays.asList(Method.POST, Method.GET, Method.PUT, Method.DELETE));
    private static final String ALLOW_METHODS_CORS = ALLOW_METHODS + "," + Method.OPTIONS ;
    private static final String DEFAULT_ALLOW_HEADERS = Joiner.on(',').join(Arrays.asList("Content-Type"));

    public RemoteServer(int port, Context context) {
        super(port);
        mContext = context;
        EventBus.getDefault().register(this);
        addGetRequestProcess();
        addPostRequestProcess();
    }

    private void addGetRequestProcess() {
        getRequestList.add(new RawRequestProcess(this.mContext, "/", R.raw.index, MIME_HTML));
        getRequestList.add(new RawRequestProcess(this.mContext, "/log", R.raw.logtail, MIME_HTML));
        getRequestList.add(new RawRequestProcess(this.mContext, "/index.html", R.raw.index, MIME_HTML));
        getRequestList.add(new RawRequestProcess(this.mContext, "/style.css", R.raw.style, "text/css"));
        getRequestList.add(new RawRequestProcess(this.mContext, "/css.css", R.raw.css, "text/css"));
        getRequestList.add(new RawRequestProcess(this.mContext, "/ui.css", R.raw.ui, "text/css"));
        getRequestList.add(new RawRequestProcess(this.mContext, "/jquery.js", R.raw.jquery, "application/x-javascript"));
        getRequestList.add(new RawRequestProcess(this.mContext, "/confirm.js", R.raw.confirm, "application/x-javascript"));
        getRequestList.add(new RawRequestProcess(this.mContext, "/script.js", R.raw.script, "application/x-javascript"));
        getRequestList.add(new RawRequestProcess(this.mContext, "/favicon.ico", R.drawable.app_icon, "image/x-icon"));
    }

    private void addPostRequestProcess() {
        postRequestList.add(new InputRequestProcess(this));
    }

    @Override
    public void start(int timeout, boolean daemon) throws IOException {
        isStarted = true;
        this.timeout = timeout;
        super.start(timeout, daemon);
        EventBus.getDefault().post(new ServerEvent(ServerEvent.SERVER_SUCCESS));
        // 定时发送PING
        getTimer().schedule(new TimerTask() {

            @Override
            public void run() {
                if (RemoteServer.this.isAlive()){
                    synchronized (wsReference) {
                        try{
                            WebSocket wsSocket = wsReference.get();
                            if(wsSocket != null && wsSocket.isOpen()){
                                wsSocket.ping(" ".getBytes());
                            }
                        }catch (Throwable e) {
                            com.github.tvbox.osc.util.LOG.e(e);
                        }
                    }
                }
            }
        }, 0, timeout*3/4);
    }

    @Override
    public void stop() {
        super.stop();
        isStarted = false;
        EventBus.getDefault().unregister(this);
    }
    /**
     * 判断是否为CORS 预检请求请求(Preflight)
     * @param session
     * @return
     */
    private static boolean isPreflightRequest(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        return Method.OPTIONS.equals(session.getMethod())
                && headers.containsKey("Origin".toLowerCase())
                && headers.containsKey("Access-Control-Request-Method".toLowerCase())
                && headers.containsKey("Access-Control-Request-Headers".toLowerCase());
    }
    /**
     * 封装响应包
     * @param session http请求
     * @param resp 响应包
     * @return resp
     */
    private Response wrapResponse(IHTTPSession session,Response resp) {
        if(null != resp){
            Map<String, String> headers = session.getHeaders();
            resp.addHeader("Access-Control-Allow-Credentials", "true");
            // 如果请求头中包含'Origin',则响应头中'Access-Control-Allow-Origin'使用此值否则为'*'
            // nanohttd将所有请求头的名称强制转为了小写
            String origin = MoreObjects.firstNonNull(headers.get("Origin".toLowerCase()), "*");
            resp.addHeader("Access-Control-Allow-Origin", origin);

            String  requestHeaders = headers.get("Access-Control-Request-Headers".toLowerCase());
            if(requestHeaders != null){
                resp.addHeader("Access-Control-Allow-Headers", requestHeaders);
            }
        }
        return resp;
    }
    /**
     * 向响应包中添加CORS包头数据
     * @param session
     * @return
     */
    private Response responseCORS(IHTTPSession session) {
        Response resp = wrapResponse(session, newFixedLengthResponse(""));
        Map<String, String> headers = session.getHeaders();
        resp.addHeader("Access-Control-Allow-Methods",
                noCORS ? ALLOW_METHODS : ALLOW_METHODS_CORS);

        String requestHeaders = headers.get("Access-Control-Request-Headers".toLowerCase());
        String allowHeaders = MoreObjects.firstNonNull(requestHeaders, DEFAULT_ALLOW_HEADERS);
        resp.addHeader("Access-Control-Allow-Headers", allowHeaders);
        //resp.addHeader(HeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
        resp.addHeader("Access-Control-Max-Age", "0");
        return resp;
    }
    /**
     * 设置是否不支持跨域请求(CORS),默认false<br>
     * @param noCORS 要设置的 noCORS
     * @return 当前对象
     */
    public RemoteServer setNoCORS(boolean noCORS) {
        this.noCORS = noCORS;
        return this;
    }

    private boolean isWebSocketConnectionHeader(Map<String, String> headers) {
        String connection = headers.get("connection");
        return connection != null && connection.toLowerCase().contains("Upgrade".toLowerCase());
    }
    private boolean isWebsocketRequested(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String upgrade = headers.get("upgrade");
        boolean isCorrectConnection = isWebSocketConnectionHeader(headers);
        boolean isUpgrade = "websocket".equalsIgnoreCase(upgrade);
        return isUpgrade && isCorrectConnection;
    }
    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    /**
     * Translates the specified byte array into Base64 string.
     * <p>
     * Android has android.util.Base64, sun has sun.misc.Base64Encoder, Java 8
     * hast java.util.Base64, I have this from stackoverflow:
     * http://stackoverflow.com/a/4265472
     * </p>
     *
     * @param buf
     *            the byte array (not null)
     * @return the translated Base64 string (not null)
     */
    private static String encodeBase64(byte[] buf) {
        int size = buf.length;
        char[] ar = new char[(size + 2) / 3 * 4];
        int a = 0;
        int i = 0;
        while (i < size) {
            byte b0 = buf[i++];
            byte b1 = i < size ? buf[i++] : 0;
            byte b2 = i < size ? buf[i++] : 0;

            int mask = 0x3F;
            ar[a++] = ALPHABET[b0 >> 2 & mask];
            ar[a++] = ALPHABET[(b0 << 4 | (b1 & 0xFF) >> 4) & mask];
            ar[a++] = ALPHABET[(b1 << 2 | (b2 & 0xFF) >> 6) & mask];
            ar[a++] = ALPHABET[b2 & mask];
        }
        switch (size % 3) {
            case 1:
                ar[--a] = '=';
            case 2:
                ar[--a] = '=';
        }
        return new String(ar);
    }

    public static String makeAcceptKey(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        md.update(text.getBytes(), 0, text.length());
        byte[] sha1hash = md.digest();
        return encodeBase64(sha1hash);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Response serve(final IHTTPSession session) {
        if (isWebsocketRequested(session)) {
            if(isPreflightRequest(session)){
                return responseCORS(session);
            }
            Map<String, String> headers = session.getHeaders();
            if (!"13".equalsIgnoreCase(headers.get("sec-websocket-version"))) {
                return newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Invalid Websocket-Version " + headers.get("sec-websocket-version"));
            }

            if (!headers.containsKey("sec-websocket-key")) {
                return newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing Websocket-Key");
            }

            WebSocket webSocket = new DebugWebSocket(session);
            Response handshakeResponse = webSocket.getHandshakeResponse();
            try {
                handshakeResponse.addHeader("sec-websocket-accept", makeAcceptKey(headers.get("sec-websocket-key")));
            } catch (NoSuchAlgorithmException e) {
                return newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                        "The SHA-1 Algorithm required for websockets is not available on the server.");
            }

            if (headers.containsKey("sec-websocket-protocol")) {
                handshakeResponse.addHeader("sec-websocket-protocol", headers.get("sec-websocket-protocol").split(",")[0]);
            }

            return handshakeResponse;
        } else {
            EventBus.getDefault().post(new ServerEvent(ServerEvent.SERVER_CONNECTION));
            if(isPreflightRequest(session)){
                return responseCORS(session);
            }
            if (!StringUtils.isEmpty(session.getUri())) {
                String fileName = session.getUri().trim();
                if (fileName.indexOf('?') >= 0) {
                    fileName = fileName.substring(0, fileName.indexOf('?'));
                }
                if (session.getMethod() == Method.GET) {
                    for (RequestProcess process : getRequestList) {
                        if (process.isRequest(session, fileName)) {
                            return process.doResponse(session, fileName, session.getParms(), null);
                        }
                    }
                    if (fileName.equals("/proxy")) {
                        Map<String, String> params = session.getParms();
                        if (params.containsKey("do")) {
                            Object[] rs = ApiConfig.get().proxyLocal(params);
                            try {
                                //EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", "proxy") + rs[0]));
                                int code = (int) rs[0];
                                String mime = (String) rs[1];
                                InputStream stream = rs[2] != null ? (InputStream) rs[2] : null;

                                Response r = Response.newChunkedResponse(Status.lookup(code), mime, stream);
                                Map<PyObject, PyObject> headers;
                                if(rs.length > 3 && rs[3] != null){
                                    headers = (Map) rs[3];
                                    for (Map.Entry<PyObject, PyObject> entry : headers.entrySet()) {
                                        r.addHeader(entry.getKey().toString(), entry.getValue().toString());
                                    }
                                }
                                return r;
                            } catch (Throwable th) {
                                EventBus.getDefault().post(new LogEvent(String.format("【E/%s】=>>>", "proxy") + Log.getStackTraceString(th)));
                                return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "500");
                            }
                        }
                    } else if (fileName.startsWith("/file/")) {
                        try {
                            String f = fileName.substring(6);
                            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                            String file = root + "/" + f;
                            File localFile = new File(file);
                            if (localFile.exists()) {
                                if (localFile.isFile()) {
                                    return Response.newChunkedResponse(Status.OK, "application/octet-stream", new FileInputStream(localFile));
                                } else {
                                    return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, fileList(root, f));
                                }
                            } else {
                                return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "File " + file + " not found!");
                            }
                        } catch (Throwable th) {
                            return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, th.getMessage());
                        }
                    } else if (fileName.equals("/dns-query")) {
                        String name = session.getParms().get("name");
                        byte[] rs = null;
                        try {
                            rs = OkGoHelper.dnsOverHttps.lookupHttpsForwardSync(name);
                        } catch (Throwable th) {
                            rs = new byte[0];
                        }
                        return newFixedLengthResponse(Status.OK, "application/dns-message", new ByteArrayInputStream(rs), rs.length);
                    }
                } else if (session.getMethod() == Method.POST) {
                    Map<String, String> files = new HashMap<String, String>();
                    try {
                        if (session.getHeaders().containsKey("content-type")) {
                            String hd = session.getHeaders().get("content-type");
                            if (hd != null) {
                                // cuke: 修正中文乱码问题
                                if (hd.toLowerCase().contains("multipart/form-data") && !hd.toLowerCase().contains("charset=")) {
                                    Matcher matcher = p.matcher(hd);
                                    String boundary = matcher.find() ? matcher.group(1) : null;
                                    if (boundary != null) {
                                        session.getHeaders().put("content-type", "multipart/form-data; charset=utf-8; " + boundary);
                                    }
                                }
                            }
                        }
                        session.parseBody(files);
                    } catch (IOException IOExc) {
                        return createPlainTextResponse(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + IOExc.getMessage());
                    } catch (ResponseException rex) {
                        return createPlainTextResponse(rex.getStatus(), rex.getMessage());
                    }
                    for (RequestProcess process : postRequestList) {
                        if (process.isRequest(session, fileName)) {
                            return process.doResponse(session, fileName, session.getParms(), files);
                        }
                    }
                    try {
                        Map<String, String> params = session.getParms();
                        switch (fileName) {
                            case "/upload": {
                                String path = params.get("path");
                                for (String k : files.keySet()) {
                                    if (k.startsWith("files-")) {
                                        String fn = params.get(k);
                                        String tmpFile = files.get(k);
                                        File tmp = new File(tmpFile);
                                        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                                        File file = new File(root + "/" + path + "/" + fn);
                                        if (file.exists())
                                            file.delete();
                                        if (tmp.exists()) {
                                            if (fn.toLowerCase().endsWith(".zip")) {
                                                unzip(tmp, root + "/" + path);
                                            } else {
                                                FileUtils.copyFile(tmp, file);
                                            }
                                        }
                                        if (tmp.exists())
                                            tmp.delete();
                                    }
                                }
                                return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "OK");
                            }
                            case "/newFolder": {
                                String path = params.get("path");
                                String name = params.get("name");
                                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                                File file = new File(root + "/" + path + "/" + name);
                                if (!file.exists()) {
                                    file.mkdirs();
                                    File flag = new File(root + "/" + path + "/" + name + "/.tvbox_folder");
                                    if (!flag.exists())
                                        flag.createNewFile();
                                }
                                return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "OK");
                            }
                            case "/delFolder": {
                                String path = params.get("path");
                                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                                File file = new File(root + "/" + path);
                                if (file.exists()) {
                                    FileUtils.recursiveDelete(file);
                                }
                                return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "OK");
                            }
                            case "/delFile": {
                                String path = params.get("path");
                                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                                File file = new File(root + "/" + path);
                                if (file.exists()) {
                                    file.delete();
                                }
                                return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "OK");
                            }
                        }
                    } catch (Throwable th) {
                        return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "OK");
                    }
                }
            }
            //default page: index.html
            return getRequestList.get(0).doResponse(session, "", null, null);
        }
    }

    public void setDataReceiver(DataReceiver receiver) {
        mDataReceiver = receiver;
    }

    public DataReceiver getDataReceiver() {
        return mDataReceiver;
    }

    public boolean isStarting() {
        return isStarted;
    }

    public String getServerAddress() {
        String ipAddress = LocalIPAddress.getLocalIPAddress(mContext);
        return "http://" + ipAddress + ":" + RemoteServer.serverPort + "/";
    }

    public String getLoadAddress() {
        return "http://127.0.0.1:" + RemoteServer.serverPort + "/";
    }

    public static Response createPlainTextResponse(IStatus status, String text) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, text);
    }

    public static Response createJSONResponse(IStatus status, String text) {
        return newFixedLengthResponse(status, "application/json", text);
    }

    String fileTime(long time, String fmt) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        return sdf.format(date);
    }

    String fileList(String root, String path) {
        File file = new File(root + "/" + path);
        File[] list = file.listFiles();
        JsonObject info = new JsonObject();
        info.addProperty("remote", getServerAddress().replace("http://", "clan://"));
        info.addProperty("del", 0);
        if (StringUtils.isEmpty(path)) {
            info.addProperty("parent", ".");
        } else {
            info.addProperty("parent", file.getParentFile().getAbsolutePath().replace(root + "/", "").replace(root, ""));
        }
        if (list == null || list.length == 0) {
            info.add("files", new JsonArray());
            return info.toString();
        }
        Arrays.sort(list, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile()) return -1;
                return o1.isFile() && o2.isDirectory() ? 1 : o1.getName().compareTo(o2.getName());
            }
        });
        JsonArray result = new JsonArray();
        for (File f : list) {
            if (f.getName().startsWith(".")) {
                if (f.getName().equals(".tvbox_folder")) {
                    info.addProperty("del", 1);
                }
                continue;
            }
            JsonObject fileObj = new JsonObject();
            fileObj.addProperty("name", f.getName());
            fileObj.addProperty("path", f.getAbsolutePath().replace(root + "/", ""));
            fileObj.addProperty("time", fileTime(f.lastModified(), "yyyy/MM/dd aHH:mm:ss"));
            fileObj.addProperty("dir", f.isDirectory() ? 1 : 0);
            result.add(fileObj);
        }
        info.add("files", result);
        return info.toString();
    }

    void unzip(File zipFilePath, String destDirectory) throws Throwable {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipFile zip = new ZipFile(zipFilePath);
        Enumeration<ZipEntry> iter = (Enumeration<ZipEntry>) zip.entries();
        while (iter.hasMoreElements()) {
            ZipEntry entry = iter.nextElement();
            InputStream is = zip.getInputStream(entry);
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(is, filePath);
            } else {
                File dir = new File(filePath);
                if (!dir.exists())
                    dir.mkdirs();
                File flag = new File(dir + "/.tvbox_folder");
                if (!flag.exists())
                    flag.createNewFile();
            }
        }
    }

    void extractFile(InputStream inputStream, String destFilePath) throws Throwable {
        File dst = new File(destFilePath);
        if (dst.exists())
            dst.delete();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFilePath));
        byte[] bytesIn = new byte[2048];
        int len = inputStream.read(bytesIn);
        while (len > 0) {
            bos.write(bytesIn, 0, len);
            len = inputStream.read(bytesIn);
        }
        bos.close();
    }

    private class DebugWebSocket extends WebSocket {

        public DebugWebSocket(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            synchronized (wsReference) {
                wsReference.set(this);
            }
        }

        @Override
        protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
            try {
                send("服务关闭");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            String tag = message.getTextPayload();
            if (TextUtils.isEmpty(tag)) {
                try {
                    send("输入不能为空");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    send(tag);
                    //这里可以实现输入命令的 执行函数
                    EventBus.getDefault().post(new InputMsgEvent(tag));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {

        }

        @Override
        protected void onException(IOException exception) {
            try {
                send(exception.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onReceiveEvent(LogEvent logEvent) {
        if(logEvent != null) {
            if (RemoteServer.this.isAlive()){
                synchronized (wsReference) {
                    try{
                        WebSocket wsSocket = wsReference.get();
                        if(wsSocket != null && wsSocket.isOpen()){
                            wsSocket.send(logEvent.getText());
                        }
                    } catch (Throwable e) {
                        com.github.tvbox.osc.util.LOG.e(e);
                    }
                }
            }

        }
    }
}