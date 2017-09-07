package com.base.common.util;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;

import com.base.common.BaseApplication;

public class DeviceUtils {

    private static final String TAG = LogUtils.makeLogTag(DeviceUtils.class);

    private DeviceUtils() {
    }

    public static boolean isLollipopOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static String getVersionName() {
        Application application = BaseApplication.getInstance();
        try {
            return application.getPackageManager().getPackageInfo(application.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, e.getMessage());
            return null;
        }
    }
}
