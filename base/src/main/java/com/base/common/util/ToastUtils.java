package com.base.common.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    public static ToastUtils LONG = new ToastUtils(Toast.LENGTH_LONG);
    public static ToastUtils SHORT = new ToastUtils(Toast.LENGTH_SHORT);

    private final int duration;

    private ToastUtils(int duration) {
        this.duration = duration;
    }

    public void show(Context context, String text) {
        Toast.makeText(context, text, duration).show();
    }

    public void show(Context context, int resId) {
        Toast.makeText(context, resId, duration).show();
    }
}

