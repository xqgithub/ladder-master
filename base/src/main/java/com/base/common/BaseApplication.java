package com.base.common;

import android.app.Application;
import android.content.Context;

import com.facebook.drawee.backends.pipeline.Fresco;

public class BaseApplication extends Application {

    private static BaseApplication instance;
    private static Context mContext;

    public static BaseApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mContext = getApplicationContext();

        Fresco.initialize(this);
    }
}
