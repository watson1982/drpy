package com.github.tvbox.osc.util;

import com.orhanobut.hawk.Hawk;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class HawkConfig {
    public static final String API_URL = "api_url";
    public static final String SHOW_PREVIEW = "show_preview";
    public static final String API_HISTORY = "api_history";
    public static final String LIVE_URL = "live_url";
    public static final String LIVE_HISTORY = "live_history";
    public static final String EPG_URL = "epg_url";
    public static final String EPG_HISTORY = "epg_history";

    public static final String HOME_API = "home_api";
    public static final String DEFAULT_PARSE = "parse_default";
    public static final String DEBUG_OPEN = "debug_open";
    public static final String PARSE_WEBVIEW = "parse_webview"; // true 系统 false xwalk
    public static final String IJK_CODEC = "ijk_codec";
    public static final String PLAY_TYPE = "play_type";//0 系统 1 ijk 2 exo 10 MXPlayer
    public static final String PLAY_RENDER = "play_render"; //0 texture 2
    public static final String PLAY_SCALE = "play_scale"; //0 texture 2
    public static final String PLAY_TIME_STEP = "play_time_step"; //0 texture 2
    public static final String DOH_URL = "doh_url";
    public static final String HOME_REC = "home_rec"; // 0 豆瓣热播 1 数据源推荐 2 历史
    public static final String HOME_NUM = "home_num"; // No. of History
    public static final String SEARCH_VIEW = "search_view"; // 0 列表 1 缩略图
    public static final String STORAGE_DRIVE_SORT = "storage_drive_sort";
    public static final String LIVE_CHANNEL = "last_live_channel_name";
    public static final String LIVE_CHANNEL_REVERSE = "live_channel_reverse";
    public static final String LIVE_CROSS_GROUP = "live_cross_group";
    public static final String LIVE_CONNECT_TIMEOUT = "live_connect_timeout";
    public static final String LIVE_SHOW_NET_SPEED = "live_show_net_speed";
    public static final String LIVE_SHOW_TIME = "live_show_time";
    public static final String HOME_SHOW_SOURCE = "show_source";
    public static final String HOME_LOCALE = "language"; // 0 中文 1 英文
    public static final String PIC_IN_PIC = "pic_in_pic"; // true = on, false = off
    public static final String PLAY_SPEED = "play_speed"; // true = on, false = off
    public static final String PLAY_MSPEED = "play_mspeed";
    public static final String SUBTITLE_TEXT_SIZE = "subtitle_text_size";
    public static final String SUBTITLE_TIME_DELAY = "subtitle_time_delay";
    public static final String SEARCH_FILTER_KEY = "search_filter_key";
    public static final String FAST_SEARCH_MODE = "fast_search_mode";
    public static final String SOURCES_FOR_SEARCH = "checked_sources_for_search";
    public static final String IJK_CACHE_PLAY = "ijk_cache_play";
    public static final String EXO_TUNNELING = "exo_tunneling";

    public static final String API_MAP = "api_map";
    public static final String API_NAME_HISTORY = "api_name_history";
    public static final String API_NAME = "api_name";
    public static final String STORE_API = "store_api";
    public static final String STORE_API_NAME = "store_api_name";
    public static final String STORE_API_NAME_HISTORY = "store_api_name_history";
    public static final String STORE_API_MAP = "store_api_map";

    public static boolean isDebug(){
        return Hawk.get(DEBUG_OPEN, false);
    }
}