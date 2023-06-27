package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.DcConfig;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.ApiHistoryDialogAdapter;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
public class DcDialog extends BaseDialog {
    private final ImageView ivQRCode;
    private final TextView tvAddress;
    // private EditText inputStoreApiName;
    private EditText inputStoreApiUrl;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_DC_CONFIG_CHANGE) {
            inputStoreApiUrl.setText((String) event.obj);
        }
    }

    public DcDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_store_api);
        //setCanceledOnTouchOutside(false);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAddress = findViewById(R.id.tvAddress);
        // inputStoreApiName = findViewById(R.id.inputStoreApiName);
        inputStoreApiUrl = findViewById(R.id.inputStoreApiUrl);

        String storeApiName = Hawk.get(HawkConfig.STORE_API_NAME, "");
        HashMap<String, String> map = Hawk.get(HawkConfig.STORE_API_MAP, new HashMap<>());
        if (map.containsKey(storeApiName)){
            // inputStoreApiName.setText(storeApiName);
            inputStoreApiUrl.setText(map.get(storeApiName));
        }

        findViewById(R.id.inputSubmit).setOnClickListener(v -> {
            // String name = inputStoreApiName.getText().toString().trim();
            String url = inputStoreApiUrl.getText().toString().trim();

            if (!url.isEmpty()) {
                if (!map.containsValue(url))
                    map.put(url, url);

                listener.onchange(url);

                Hawk.put(HawkConfig.STORE_API_MAP, map);
                Hawk.put(HawkConfig.STORE_API_NAME, url);
            }
            try {
                DcConfig.get().Subscribe(this.getContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
            dismiss();
        });

        findViewById(R.id.StoreApiHistory).setOnClickListener( v -> {
            ArrayList<String> history = Hawk.get(HawkConfig.STORE_API_NAME_HISTORY, new ArrayList<>());
            if (history.isEmpty())
                return;

            int idx = 0;
            if (history.contains(storeApiName))
                idx = history.indexOf(storeApiName);

            ApiHistoryDialog dialog = new ApiHistoryDialog(getContext());
            dialog.setTip("多源历史配置列表");
            dialog.setAdapter(new ApiHistoryDialogAdapter.SelectDialogInterface() {
                @Override
                public void click(String value) {
                    Hawk.put(HawkConfig.STORE_API_NAME, value);
                    try {
                        DcConfig.get().Subscribe(getContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                }

                @Override
                public void del(String value, ArrayList<String> data) {
                    HashMap<String, String> map = Hawk.get(HawkConfig.STORE_API_MAP, new HashMap<>());
                    map.remove(value);
                    Hawk.put(HawkConfig.STORE_API_MAP, map);
                    Hawk.put(HawkConfig.STORE_API_NAME_HISTORY, data);
                }
            }, history, idx);
            dialog.show();
        });
        refreshQRCode();
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("手机/电脑扫描上方二维码或者直接浏览器访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, AutoSizeUtils.mm2px(getContext(), 300), AutoSizeUtils.mm2px(getContext(), 300)));
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onchange(String data);
    }
}
