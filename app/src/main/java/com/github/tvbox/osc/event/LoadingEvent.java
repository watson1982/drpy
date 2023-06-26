package com.github.tvbox.osc.event;

public class LoadingEvent {
    private boolean show;
    private String text;

    public LoadingEvent(String str, boolean z) {
        this.text = str;
        this.show = z;
    }

    public boolean isShow() {
        return this.show;
    }

    public void setShow(boolean z) {
        this.show = z;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String str) {
        this.text = str;
    }
}
