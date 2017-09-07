package com.base.common.callback;

import android.view.View;

public abstract class OnSingleClick implements View.OnClickListener {
    public static long lastTime;

    public abstract void singleClick(View v);

    @Override
    public void onClick(View v) {
        if (onDoubleClick()) {
            return;
        }
        singleClick(v);
    }

    public boolean onDoubleClick() {
        boolean flag = false;
        long time = System.currentTimeMillis() - lastTime;

        if (time < 500) {
            flag = true;
        }
        lastTime = System.currentTimeMillis();
        return flag;
    }
}
