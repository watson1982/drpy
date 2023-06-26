package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.event.InputMsgEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;



/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
public class LivePasswordDialog extends BaseDialog {
    private EditText inputPassword;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInputMsgEvent(InputMsgEvent inputMsgEvent) {
        /*View vFocus = SearchActivity.this.getWindow().getDecorView().findFocus();
        if (vFocus.getTag() == etSearch) {
            ((EditText) vFocus).setText(inputMsgEvent.getText());
        }*/
        inputPassword.setFocusableInTouchMode(true);
        inputPassword.requestFocus();
        inputPassword.setText(inputMsgEvent.getText());
    }

    public LivePasswordDialog(@NonNull @NotNull Context context) {
        super(context);
        setOwnerActivity((Activity) context);
        setContentView(R.layout.dialog_live_password);

        inputPassword = findViewById(R.id.input);
        findViewById(R.id.inputSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = inputPassword.getText().toString().trim();
                if (!password.isEmpty()) {
                    listener.onChange(password);
                    dismiss();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        listener.onCancel();
        dismiss();
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onChange(String password);
        void onCancel();
    }
}
