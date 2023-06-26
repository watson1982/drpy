package com.quickjs.android;

import org.json.JSONArray;

public class JSArray extends JSObject {

    public JSArray(QuickJSContext context, long pointer) {
        super(context, pointer);
    }

    public int length() {
        checkReleased();
        return getContext().length(this);
    }

    public Object get(int index) {
        checkReleased();
        return getContext().get(this, index);
    }

    public void set(Object value, int index) {
        checkReleased();
        getContext().set(this, value, index);
    }

    public String stringify() {
        return getContext().stringify(this);
    }

    public JSONArray toJSONArray() {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < this.length(); i++) {
            Object obj = this.get(i);
            if (obj == null || obj instanceof JSFunction) {
                continue;
            }
            if (obj instanceof Number || obj instanceof String || obj instanceof Boolean) {
                jsonArray.put(obj);
            } else if (obj instanceof JSArray) {
                jsonArray.put(((JSArray) obj).toJSONArray());
            } else if (obj instanceof JSObject) {
                jsonArray.put(((JSObject) obj).toJSONObject());
            }
        }
        return jsonArray;
    }
}
