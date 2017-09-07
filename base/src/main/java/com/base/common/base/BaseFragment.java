package com.base.common.base;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment {

    private Handler mUiHandler = new Handler(Looper.getMainLooper());


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            onVisibleToUser();
        } else {
            onInvisibleToUser();
        }
    }

    protected void onInvisibleToUser() {
    }

    protected void onVisibleToUser() {
    }

    protected void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            mUiHandler.post(action);
        } else {
            action.run();
        }
    }


}
