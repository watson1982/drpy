package com.github.tvbox.osc.ui.activity;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.bean.VodSeriesGroup;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesFlagAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesGroupAdapter;
import com.github.tvbox.osc.ui.dialog.QuickSearchDialog;
import com.github.tvbox.osc.ui.fragment.PlayFragment;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HeavyTaskUtil;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.util.js.jianpian;
import com.github.tvbox.osc.util.thunder.Thunder;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.jieba_android.JiebaSegmenter;
import com.jieba_android.RequestCallback;
import com.lzy.okgo.OkGo;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;
import com.github.tvbox.osc.util.StringUtils;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class DetailActivity extends BaseActivity {
    private static final int PIP_BOARDCAST_ACTION_PREV = 0;
    private static final int PIP_BOARDCAST_ACTION_PLAYPAUSE = 1;
    private static final int PIP_BOARDCAST_ACTION_NEXT = 2;
    private static PlayFragment playFragment = null;
    private final List<Movie.Video> quickSearchData = new ArrayList<>();
    public String vodId;
    public String sourceKey;
    boolean seriesSelect = false;
    boolean PIP = Hawk.get(HawkConfig.PIC_IN_PIC, false);
    // preview : true 开启 false 关闭
    VodInfo previewVodInfo = null;
    boolean showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true);
    boolean fullWindows = false;
    ViewGroup.LayoutParams windowsPreview = null;
    ViewGroup.LayoutParams windowsFull = null;
    private LinearLayout llLayout;
    private FragmentContainerView llPlayerFragmentContainer;
    private View llPlayerFragmentContainerBlock;
    private View llPlayerPlace;
    private ImageView ivThumb;
    private TextView tvName;
    private TextView tvYear;
    private TextView tvSite;
    private TextView tvArea;
    private TextView tvLang;
    private TextView tvType;
    private TextView tvActor;
    private TextView tvDirector;
    private TextView tvDes;
    private TextView tvPlay;
    private TextView tvSort;
    private TextView tvQuickSearch;
    private TextView tvCollect;
    private TvRecyclerView mGridViewFlag;
    private TvRecyclerView mGridView;
    private LinearLayout mEmptyPlayList;
    private SourceViewModel sourceViewModel;
    private Movie.Video mVideo;
    private VodInfo vodInfo;
    private SeriesFlagAdapter seriesFlagAdapter;
    private SeriesAdapter seriesAdapter;
    private View seriesFlagFocus = null;
    private BroadcastReceiver pipActionReceiver;
    private ImageView tvPlayUrl;
    private TextView tvPlayUrl1;
    private HashMap<String, String> mCheckSources = null;
    private SeriesGroupAdapter seriesGroupAdapter;
    private TvRecyclerView mSeriesGroupView;
    private List<Runnable> pauseRunnable = null;
    private String preFlag = "";
    private List<List<VodInfo.VodSeries>> uu;
    private int GroupCount;
    private int GroupIndex = 0;
    private String searchTitle = "";
    private boolean hadQuickStart = false;
    private List<String> quickSearchWord = new ArrayList<>();
    private ExecutorService searchExecutorService = null;

    public static int getNum(String str) {
        try {
            Matcher matcher = Pattern.compile("\\d+").matcher(str);
            if (!matcher.find()) {
                return 0;
            }
            String group = matcher.group(0);
            if (TextUtils.isEmpty(group)) {
                return 0;
            }
            return Integer.parseInt(group);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_detail;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        llLayout = findViewById(R.id.llLayout);
        llPlayerPlace = findViewById(R.id.previewPlayerPlace);
        llPlayerFragmentContainer = findViewById(R.id.previewPlayer);
        llPlayerFragmentContainerBlock = findViewById(R.id.previewPlayerBlock);
        ivThumb = findViewById(R.id.ivThumb);
        llPlayerPlace.setVisibility(showPreview ? View.VISIBLE : View.GONE);
        ivThumb.setVisibility(!showPreview ? View.VISIBLE : View.GONE);
        tvName = findViewById(R.id.tvName);
        tvYear = findViewById(R.id.tvYear);
        tvSite = findViewById(R.id.tvSite);
        tvArea = findViewById(R.id.tvArea);
        tvLang = findViewById(R.id.tvLang);
        tvType = findViewById(R.id.tvType);
        tvActor = findViewById(R.id.tvActor);
        tvDirector = findViewById(R.id.tvDirector);
        tvDes = findViewById(R.id.tvDes);
        tvPlay = findViewById(R.id.tvPlay);
        tvSort = findViewById(R.id.tvSort);
        tvCollect = findViewById(R.id.tvCollect);
        tvQuickSearch = findViewById(R.id.tvQuickSearch);
        tvPlayUrl = findViewById(R.id.tvPlayUrl);
        tvPlayUrl1 = findViewById(R.id.tvPlayUrl1);
        mEmptyPlayList = findViewById(R.id.mEmptyPlaylist);
        mGridView = findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, shouldMoreColumns() ? 6 : 7));
        seriesAdapter = new SeriesAdapter();
        mGridView.setAdapter(seriesAdapter);
        mGridViewFlag = findViewById(R.id.mGridViewFlag);
        mGridViewFlag.setHasFixedSize(true);
        mGridViewFlag.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        seriesFlagAdapter = new SeriesFlagAdapter();
        mGridViewFlag.setAdapter(seriesFlagAdapter);
        if (showPreview) {
            playFragment = new PlayFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.previewPlayer, playFragment).commit();
            getSupportFragmentManager().beginTransaction().show(playFragment).commitAllowingStateLoss();
            tvPlay.setText(getString(R.string.det_expand));
        } else {
            tvPlay.setVisibility(View.VISIBLE);
            tvPlay.requestFocus();
        }
        mSeriesGroupView = findViewById(R.id.mSeriesGroupView);
        mSeriesGroupView.setHasFixedSize(true);
        mSeriesGroupView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        seriesGroupAdapter = new SeriesGroupAdapter();
        mSeriesGroupView.setAdapter(seriesGroupAdapter);

        //禁用播放地址焦点
        tvPlayUrl1.setFocusable(false);
        tvSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vodInfo != null && vodInfo.seriesMap.size() > 0) {
                    vodInfo.reverseSort = !vodInfo.reverseSort;
                    preFlag = "";
                    if (vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.playIndex) {
                        vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).selected = false;
                    }
                    vodInfo.reverse();
                    if (vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.playIndex) {
                        vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).selected = true;
                    }
                    refreshList();
                    insertVod(sourceKey, vodInfo);
                    //seriesAdapter.notifyDataSetChanged();
                }
            }
        });
        tvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                if (showPreview) {
                    toggleFullPreview();
                } else {
                    jumpToPlay();
                }
            }
        });
        // takagen99 : Added click Image Thummb or Preview Window to play video
        ivThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                jumpToPlay();
            }
        });
        llPlayerFragmentContainerBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                toggleFullPreview();
            }
        });
        tvQuickSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuickSearch();
                QuickSearchDialog quickSearchDialog = new QuickSearchDialog(DetailActivity.this);
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, quickSearchData));
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
                quickSearchDialog.show();
                if (pauseRunnable != null && pauseRunnable.size() > 0) {
                    searchExecutorService = Executors.newFixedThreadPool(5);
                    for (Runnable runnable : pauseRunnable) {
                        searchExecutorService.execute(runnable);
                    }
                    pauseRunnable.clear();
                    pauseRunnable = null;
                }
                quickSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        try {
                            if (searchExecutorService != null) {
                                pauseRunnable = searchExecutorService.shutdownNow();
                                searchExecutorService = null;
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                });
            }
        });
        tvCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = tvCollect.getText().toString();
                        if (getString(R.string.det_fav_unstar).equals(text)) {
                            RoomDataManger.insertVodCollect(sourceKey, vodInfo);
                            Toast.makeText(DetailActivity.this, getString(R.string.det_fav_add), Toast.LENGTH_SHORT).show();
                            tvCollect.setText(getString(R.string.det_fav_star));
                        } else {
                            RoomDataManger.deleteVodCollect(sourceKey, vodInfo);
                            Toast.makeText(DetailActivity.this, getString(R.string.det_fav_del), Toast.LENGTH_SHORT).show();
                            tvCollect.setText(getString(R.string.det_fav_unstar));
                        }
                    }
                });
            }
        });
        tvPlayUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //获取剪切板管理器
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        //设置内容到剪切板
                        cm.setPrimaryClip(ClipData.newPlainText(null, vodInfo.seriesMap.get(vodInfo.playFlag).get(0).url));
                        Toast.makeText(DetailActivity.this, getString(R.string.det_url), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        tvPlayUrl1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //获取剪切板管理器
                        ClipboardManager cm = (ClipboardManager) getSystemService(mContext.CLIPBOARD_SERVICE);
                        //设置内容到剪切板
                        cm.setPrimaryClip(ClipData.newPlainText(null, tvPlayUrl1.getText().toString().replace("播放地址：", "")));
                        Toast.makeText(DetailActivity.this, getString(R.string.det_url), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                seriesSelect = false;
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                seriesSelect = true;
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        mGridViewFlag.setOnItemListener(new TvRecyclerView.OnItemListener() {
            private void refresh(View itemView, int position) {
                String newFlag = seriesFlagAdapter.getData().get(position).name;
                if (vodInfo != null && !vodInfo.playFlag.equals(newFlag)) {
                    for (int i = 0; i < vodInfo.seriesFlags.size(); i++) {
                        VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(i);
                        if (flag.name.equals(vodInfo.playFlag)) {
                            flag.selected = false;
                            seriesFlagAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    vodInfo.seriesFlags.get(position).selected = true;

                    // clean pre flag select status
                    if (vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.playIndex) {
                        vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).selected = false;
                    }
                    vodInfo.playFlag = newFlag;

                    seriesFlagAdapter.notifyItemChanged(position);

                    refreshList();
                }
                seriesFlagFocus = itemView;
            }

            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }
        });
        seriesAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
                    boolean reload = false;
                    if (vodInfo.playIndex != GroupIndex * GroupCount + position) {
                        for (int i = 0; i < seriesAdapter.getData().size(); i++){
                            VodInfo.VodSeries Series = seriesAdapter.getData().get(i);
                            Series.selected = false;
                            seriesAdapter.notifyItemChanged(i);
                        }
                        seriesAdapter.getData().get(position).selected = true;
                        seriesAdapter.notifyItemChanged(position);
                        vodInfo.playIndex = GroupIndex * GroupCount + position;
                        reload = true;
                    }
                    //解决当前集不刷新的BUG
                    if (!vodInfo.playFlag.equals(preFlag)) {
                        reload = true;
                    }
                    //选集全屏 想选集不全屏的注释下面一行
                    if (showPreview && !fullWindows) toggleFullPreview();
                    if (reload || !showPreview) jumpToPlay();
                }
            }
        });
        mSeriesGroupView.setOnItemListener(new TvRecyclerView.OnItemListener() {

            public void refresh(View itemView, int position) {

                if (GroupIndex != position) {
                    seriesGroupAdapter.getData().get(GroupIndex).selected = false;
                    seriesGroupAdapter.notifyItemChanged(GroupIndex);
                    seriesGroupAdapter.getData().get(position).selected = true;
                    seriesGroupAdapter.notifyItemChanged(position);
                    GroupIndex = position;
                    seriesAdapter.setNewData(uu.get(position));
                }

            }

            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
            }
        });

        setLoadSir(llLayout);
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    private void jumpToPlay() {
        if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
            preFlag = vodInfo.playFlag;
            //更新播放地址
            setTextShow(tvPlayUrl1, "播放地址", vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).url);
            Bundle bundle = new Bundle();
            //保存历史
            insertVod(sourceKey, vodInfo);
            bundle.putString("sourceKey", sourceKey);
            bundle.putSerializable("VodInfo", vodInfo);
            if (showPreview) {
                if (previewVodInfo == null) {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(vodInfo);
                        oos.flush();
                        oos.close();
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
                        previewVodInfo = (VodInfo) ois.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (previewVodInfo != null) {
                    previewVodInfo.playerCfg = vodInfo.playerCfg;
                    previewVodInfo.playFlag = vodInfo.playFlag;
                    previewVodInfo.playIndex = vodInfo.playIndex;
                    previewVodInfo.seriesMap = vodInfo.seriesMap;
                    bundle.putSerializable("VodInfo", previewVodInfo);
                }
                playFragment.setData(bundle);
            } else {
                jumpActivity(PlayActivity.class, bundle);
            }
        }
    }

    private void refreshList() {
        try {
            if (vodInfo.seriesMap.get(vodInfo.playFlag).size() <= vodInfo.playIndex) {
                vodInfo.playIndex = 0;
            }
            if (vodInfo.seriesMap.get(vodInfo.playFlag) != null) {
                vodInfo.seriesMap.get(vodInfo.playFlag).get(this.vodInfo.playIndex).selected = true;
            }

            List<VodSeriesGroup> seriesGroupList = getSeriesGroupList();
            seriesGroupList.get(GroupIndex).selected = true;
            seriesGroupAdapter.setNewData(seriesGroupList);

            seriesAdapter.setNewData(uu.get(GroupIndex));

            mGridView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mGridView.scrollToPosition(vodInfo.playIndex % GroupCount);
                    mSeriesGroupView.scrollToPosition(GroupIndex);
                }
            }, 100);
        } catch (Exception e) {
            //Log.e("refreshList", e.getMessage());
        }
    }

    private List<VodSeriesGroup> getSeriesGroupList() {
        List<VodSeriesGroup> arrayList = new ArrayList<>();
        if(uu != null){
            uu.clear();
        } else {
            uu = new ArrayList<>();
        }
        try {
            List<VodInfo.VodSeries> vodSeries = vodInfo.seriesMap.get(vodInfo.playFlag);
            int size = vodSeries.size();
            GroupCount = size > 2500.0d ? 300 : size > 1500.0d ? 200 : size > 1000.0d ? 150 : size > 500.0d ? 100 : size > 300.0d ? 50 : size > 100.0d ? 30 : 20;
            GroupIndex = (int) Math.floor(vodInfo.playIndex / (GroupCount + 0.0f));
            if (GroupIndex < 0) {
                GroupIndex = 0;
            }
            if (size > GroupCount) {
                /*int start = getNum(vodSeries.get(0).name);
                int end = getNum(vodSeries.get(size - 1).name);
                if (end == 1 && start > 1) {
                    this.vodInfo.reverse();
                    vodSeries = vodInfo.seriesMap.get(vodInfo.playFlag);
                }*/
                int GroupSize = (int) Math.ceil(size / (GroupCount + 0.0f));
                for(int i = 0; i < GroupSize; i++){
                    mSeriesGroupView.setVisibility(View.VISIBLE);
                    int s = (i * GroupCount) + 1;
                    int e = (i + 1) * GroupCount;
                    List<VodInfo.VodSeries> info = new ArrayList<>();
                    if (e < size) {
                        for (int j = s - 1; j < e; j++) {
                            info.add(vodSeries.get(j));
                        }
                        arrayList.add(new VodSeriesGroup(s + "-" + e));
                        //arrayList.add(s + "-" + e);
                    } else {
                        for (int j = s - 1; j < size; j++) {
                            info.add(vodSeries.get(j));
                        }
                        arrayList.add(new VodSeriesGroup(s + "-" + size));
                        //arrayList.add(s + "-" + size);
                    }
                    uu.add(info);
                }
            } else {
                arrayList.add(new VodSeriesGroup("1-" + size));
                //arrayList.add("1-" + size);
                uu.add(vodSeries);
                mSeriesGroupView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            //Log.e("getFilter", e.getMessage());
        }
        return arrayList;
    }

    private void setTextShow(TextView view, String tag, String info) {
        if (info == null || StringUtils.isEmpty(info.trim())) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(Html.fromHtml(getHtml(tag, info)));
    }

    private String removeHtmlTag(String info) {
        if (info == null)
            return "";
        return info.replaceAll("\\<.*?\\>", "").replaceAll("\\s", "");
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.detailResult.observe(this, new Observer<AbsXml>() {
            @Override
            public void onChanged(AbsXml absXml) {
                if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
                    showSuccess();
                    if (!TextUtils.isEmpty(absXml.msg) && !absXml.msg.equals("数据列表")) {
                        Toast.makeText(DetailActivity.this, absXml.msg, Toast.LENGTH_SHORT)
                            .show();
                        showEmpty();
                        return;
                    }
                    mVideo = absXml.movie.videoList.get(0);
                    
                    mVideo.id = vodId;
                    if (TextUtils.isEmpty(mVideo.name)) mVideo.name = "片名被猫爪吃了";
                    vodInfo = new VodInfo();
                    vodInfo.setVideo(mVideo);
                    vodInfo.sourceKey = mVideo.sourceKey;

                    tvName.setText(mVideo.name);
                    setTextShow(tvSite, getString(R.string.det_source), ApiConfig.get().getSource(mVideo.sourceKey).getName());
                    setTextShow(tvYear, getString(R.string.det_year), mVideo.year == 0 ? "" : String.valueOf(mVideo.year));
                    setTextShow(tvArea, getString(R.string.det_area), mVideo.area);
                    setTextShow(tvLang, getString(R.string.det_lang), mVideo.lang);
                    setTextShow(tvType, getString(R.string.det_type), mVideo.type);
                    setTextShow(tvActor, getString(R.string.det_actor), mVideo.actor);
                    setTextShow(tvDirector, getString(R.string.det_dir), mVideo.director);
                    setTextShow(tvDes, getString(R.string.det_des), removeHtmlTag(mVideo.des));
                    if (!TextUtils.isEmpty(mVideo.pic)) {
                        Picasso.get()
                                .load(DefaultConfig.checkReplaceProxy(mVideo.pic))
                                .transform(new RoundTransformation(MD5.string2MD5(mVideo.pic + mVideo.name))
                                        .centerCorp(true)
                                        .override(AutoSizeUtils.mm2px(mContext, 300), AutoSizeUtils.mm2px(mContext, 400))
                                        .roundRadius(AutoSizeUtils.mm2px(mContext, 15), RoundTransformation.RoundType.ALL))
                                .placeholder(R.drawable.img_loading_placeholder)
                                .error(R.drawable.img_loading_placeholder)
                                .into(ivThumb);
                    } else {
                        ivThumb.setImageResource(R.drawable.img_loading_placeholder);
                    }

                    if (vodInfo.seriesMap != null && vodInfo.seriesMap.size() > 0) {
                        mGridViewFlag.setVisibility(View.VISIBLE);
                        mGridView.setVisibility(View.VISIBLE);
                        //tvPlay.setVisibility(View.VISIBLE);
                        mEmptyPlayList.setVisibility(View.GONE);

                        VodInfo vodInfoRecord = RoomDataManger.getVodInfo(sourceKey, vodId);
                        // 读取历史记录
                        if (vodInfoRecord != null) {
                            vodInfo.playIndex = Math.max(vodInfoRecord.playIndex, 0);
                            vodInfo.playFlag = vodInfoRecord.playFlag;
                            vodInfo.playerCfg = vodInfoRecord.playerCfg;
                            vodInfo.reverseSort = vodInfoRecord.reverseSort;
                        } else {
                            vodInfo.playIndex = 0;
                            vodInfo.playFlag = null;
                            vodInfo.playerCfg = "";
                            vodInfo.reverseSort = false;
                        }

                        if (vodInfo.reverseSort) {
                            vodInfo.reverse();
                        }

                        if (vodInfo.playFlag == null || !vodInfo.seriesMap.containsKey(vodInfo.playFlag))
                            vodInfo.playFlag = (String) vodInfo.seriesMap.keySet().toArray()[0];

                        int flagScrollTo = 0;
                        for (int j = 0; j < vodInfo.seriesFlags.size(); j++) {
                            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(j);
                            if (flag.name.equals(vodInfo.playFlag)) {
                                flagScrollTo = j;
                                flag.selected = true;
                            } else
                                flag.selected = false;
                        }
                        //设置播放地址
                        setTextShow(tvPlayUrl1, "播放地址", vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).url);
                        seriesFlagAdapter.setNewData(vodInfo.seriesFlags);
                        mGridViewFlag.scrollToPosition(flagScrollTo);

                        refreshList();
                        if (showPreview) {
                            jumpToPlay();
                            llPlayerFragmentContainer.setVisibility(View.VISIBLE);
                            llPlayerFragmentContainerBlock.setVisibility(View.VISIBLE);
                            llPlayerFragmentContainerBlock.requestFocus();
                        }
                        // startQuickSearch();
                    } else {
                        mGridViewFlag.setVisibility(View.GONE);
                        mGridView.setVisibility(View.GONE);
                        tvPlay.setVisibility(View.GONE);
                        mEmptyPlayList.setVisibility(View.VISIBLE);
                    }
                } else {
                    showEmpty();
                    llPlayerFragmentContainer.setVisibility(View.GONE);
                    llPlayerFragmentContainerBlock.setVisibility(View.GONE);
                }
            }
        });
    }

    private String getHtml(String label, String content) {
        if (content == null) {
            content = "";
        }
        if (label.length() > 0) {
            label = label + ": ";
        }
        return label + "<font color=\"#FFFFFF\">" + content + "</font>";
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""));
        }
    }

    private void loadDetail(String vid, String key) {
        if (vid != null) {
            vodId = vid;
            sourceKey = key;
            showLoading();
            sourceViewModel.getDetail(sourceKey, vodId);

            boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
            if (isVodCollect) {
                tvCollect.setText(getString(R.string.det_fav_star));
            } else {
                tvCollect.setText(getString(R.string.det_fav_unstar));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null) {
                if (event.obj instanceof Integer) {
                    int index = (int) event.obj;
                    int mGroupIndex = (int) Math.floor(index / (GroupCount + 0.0f));

                    if (mGroupIndex != GroupIndex) {
                        seriesGroupAdapter.getData().get(GroupIndex).selected = false;
                        seriesGroupAdapter.notifyItemChanged(GroupIndex);
                        seriesGroupAdapter.getData().get(mGroupIndex).selected = true;
                        seriesGroupAdapter.notifyItemChanged(mGroupIndex);
                        seriesAdapter.setNewData(uu.get(mGroupIndex));
                        GroupIndex = mGroupIndex;
                        mSeriesGroupView.scrollToPosition(mGroupIndex);
                    }
                    if (index != vodInfo.playIndex) {
                        //seriesAdapter.setNewData(uu.get(GroupIndex));
                        seriesAdapter.getData().get(vodInfo.playIndex % GroupCount).selected = false;
                        seriesAdapter.notifyItemChanged(vodInfo.playIndex % GroupCount);
                        seriesAdapter.getData().get(index % GroupCount).selected = true;
                        seriesAdapter.notifyItemChanged(index % GroupCount);
                        vodInfo.playIndex = index;
                        mGridView.scrollToPosition(index % GroupCount);
                        //保存历史
                        insertVod(sourceKey, vodInfo);
                    }

                } else if (event.obj instanceof JSONObject) {
                    vodInfo.playerCfg = event.obj.toString();
                    //保存历史
                    insertVod(sourceKey, vodInfo);
                }

            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
            if (event.obj != null) {
                Movie.Video video = (Movie.Video) event.obj;
                loadDetail(video.id, video.sourceKey);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD_CHANGE) {
            if (event.obj != null) {
                String word = (String) event.obj;
                switchSearchWord(word);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private void switchSearchWord(String word) {
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchData.clear();
        searchTitle = word;
        searchResult();
    }

    private void startQuickSearch() {
        initCheckedSourcesForSearch();
        if (hadQuickStart)
            return;
        hadQuickStart = true;
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchWord.clear();
        searchTitle = mVideo.name;
        quickSearchData.clear();
        // 分词
        JiebaSegmenter.getJiebaSegmenterSingleton().getDividedStringAsync(searchTitle, new RequestCallback<ArrayList<String>>() {
            @Override
            public void onSuccess(ArrayList<String> result) {
                quickSearchWord = result;
                searchResult();
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
            }

            @Override
            public void onError(String errorMsg) {

            }
        });

        /*OkGo.<String>get("http://api.pullword.com/get.php?source=" + URLEncoder.encode(searchTitle) + "&param1=0&param2=0&json=1")
                .tag("fenci")
                .execute(new AbsCallback<String>() {
                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        if (response.body() != null) {
                            return response.body().string();
                        } else {
                            throw new IllegalStateException("网络请求错误");
                        }
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        String json = response.body();
                        quickSearchWord.clear();
                        try {
                            for (JsonElement je : new Gson().fromJson(json, JsonArray.class)) {
                                quickSearchWord.add(je.getAsJsonObject().get("t").getAsString());
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                        quickSearchWord.add(searchTitle);
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });*/


    }

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable() || !bean.isQuickSearch()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.getQuickSearch(key, searchTitle);
                }
            });
        }
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                // 去除当前相同的影片
                if (video.sourceKey.equals(sourceKey) && video.id.equals(vodId))
                    continue;
                data.add(video);
            }
            quickSearchData.addAll(data);
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, data));
        }
    }

    private void insertVod(String sourceKey, VodInfo vodInfo) {
        try {
            vodInfo.playNote = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).name;
        } catch (Throwable th) {
            vodInfo.playNote = "";
        }
        RoomDataManger.insertVodRecord(sourceKey, vodInfo);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        HeavyTaskUtil.executeNewTask(new Runnable() {
            @Override
            public void run() {
                Thunder.stop(false);//停止磁力下载
                jianpian.finish();//停止p2p下载
                OkGo.getInstance().cancelTag("fenci");
                OkGo.getInstance().cancelTag("detail");
                OkGo.getInstance().cancelTag("quick_search");
            }
        });
        EventBus.getDefault().unregister(this);
    }

    // takagen99 : Commented out to allow monitor Click Event
    //@Override
    //public boolean dispatchTouchEvent(MotionEvent ev) {
    //    if (showPreview && !fullWindows) {
    //        Rect editTextRect = new Rect();
    //        llPlayerFragmentContainerBlock.getHitRect(editTextRect);
    //        if (editTextRect.contains((int) ev.getX(), (int) ev.getY())) {
    //            return true;
    //        }
    //    }
    //    return super.dispatchTouchEvent(ev);
    //}

    @Override
    public void onUserLeaveHint() {
        // takagen99 : Additional check for external player
        if (supportsPiPMode() && showPreview && !playFragment.extPlay && PIP) {
            List<RemoteAction> actions = new ArrayList<>();
            actions.add(generateRemoteAction(android.R.drawable.ic_media_previous, PIP_BOARDCAST_ACTION_PREV, "Prev", "Play Previous"));
            actions.add(generateRemoteAction(android.R.drawable.ic_media_play, PIP_BOARDCAST_ACTION_PLAYPAUSE, "Play", "Play/Pause"));
            actions.add(generateRemoteAction(android.R.drawable.ic_media_next, PIP_BOARDCAST_ACTION_NEXT, "Next", "Play Next"));
            PictureInPictureParams params = new PictureInPictureParams.Builder().setActions(actions).build();
            if (!fullWindows) {
                toggleFullPreview();
            }
            enterPictureInPictureMode(params);
            playFragment.getVodController().hideBottom();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private RemoteAction generateRemoteAction(int iconResId, int actionCode, String title, String desc) {
        final PendingIntent intent =
                PendingIntent.getBroadcast(
                        DetailActivity.this,
                        actionCode,
                        new Intent("PIP_VOD_CONTROL").putExtra("action", actionCode),
                        0);
        final Icon icon = Icon.createWithResource(DetailActivity.this, iconResId);
        return (new RemoteAction(icon, title, desc, intent));
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (supportsPiPMode() && isInPictureInPictureMode) {
            pipActionReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !intent.getAction().equals("PIP_VOD_CONTROL") || playFragment.getVodController() == null) {
                        return;
                    }

                    int currentStatus = intent.getIntExtra("action", 1);
                    if (currentStatus == PIP_BOARDCAST_ACTION_PREV) {
                        playFragment.playPrevious();
                    } else if (currentStatus == PIP_BOARDCAST_ACTION_PLAYPAUSE) {
                        playFragment.getVodController().togglePlay();
                    } else if (currentStatus == PIP_BOARDCAST_ACTION_NEXT) {
                        playFragment.playNext();
                    }
                }
            };
            registerReceiver(pipActionReceiver, new IntentFilter("PIP_VOD_CONTROL"));

        } else {
            unregisterReceiver(pipActionReceiver);
            pipActionReceiver = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (fullWindows) {
            if (playFragment.onBackPressed())
                return;
            toggleFullPreview();
            mGridView.requestFocus();
            return;
        }
        if (seriesSelect) {
            if (seriesFlagFocus != null && !seriesFlagFocus.isFocused()) {
                seriesFlagFocus.requestFocus();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    void toggleFullPreview() {
        if (windowsPreview == null) {
            windowsPreview = llPlayerFragmentContainer.getLayoutParams();
        }
        if (windowsFull == null) {
            windowsFull = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Full Window flag
        fullWindows = !fullWindows;
        llPlayerFragmentContainer.setLayoutParams(fullWindows ? windowsFull : windowsPreview);
        llPlayerFragmentContainerBlock.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mGridView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mGridViewFlag.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mSeriesGroupView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);

        // 全屏下禁用详情页几个按键的焦点 防止上键跑过来 : Disable buttons when full window
        tvPlay.setFocusable(!fullWindows);
        tvSort.setFocusable(!fullWindows);
        tvCollect.setFocusable(!fullWindows);
        tvQuickSearch.setFocusable(!fullWindows);
    }
}