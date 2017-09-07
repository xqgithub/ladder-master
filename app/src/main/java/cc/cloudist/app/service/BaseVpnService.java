package cc.cloudist.app.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;

import com.base.common.util.LogUtils;
import com.base.common.util.ToastUtils;
import cc.cloudist.aidl.Config;
import cc.cloudist.aidl.IShadowsocksService;
import cc.cloudist.aidl.IShadowsocksServiceCallback;
import cc.cloudist.app.R;
import cc.cloudist.app.common.Constants.Action;
import cc.cloudist.app.common.Constants.State;
import cc.cloudist.app.common.thread.TrafficMonitorThread;
import cc.cloudist.app.util.TrafficMonitor;

import java.util.Timer;
import java.util.TimerTask;

public class BaseVpnService extends VpnService {

    private static final String TAG = LogUtils.makeLogTag(BaseVpnService.class);

    private volatile int mState = State.STOPPED;
    protected volatile Config mConfig = null;

    protected Timer timer = null;
    protected TrafficMonitorThread trafficMonitorThread = null;

    final protected RemoteCallbackList<IShadowsocksServiceCallback> callbacks = new RemoteCallbackList<>();
    protected int callbacksCount;

    protected boolean closeReceiverRegistered;

    private BroadcastReceiver mCloseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ToastUtils.SHORT.show(context, R.string.shut_down);
            stopRunner(true);
        }
    };

    public IShadowsocksService.Stub binder = new IShadowsocksService.Stub() {
        @Override
        public int getState() throws RemoteException {
            return mState;
        }

        @Override
        public void registerCallback(IShadowsocksServiceCallback callback) throws RemoteException {
            if (callback != null && callbacks.register(callback)) {
                callbacksCount += 1;
                if (callbacksCount != 0 && timer == null) {
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            if (TrafficMonitor.updateRate()) updateTrafficRate();
                        }
                    };

                    timer = new Timer(true);
                    timer.schedule(task, 1000, 1000);
                }

                TrafficMonitor.updateRate();
                callback.trafficUpdated(TrafficMonitor.txRate,
                        TrafficMonitor.rxRate, TrafficMonitor.txTotal, TrafficMonitor.rxTotal);
            }
        }

        @Override
        public void unregisterCallback(IShadowsocksServiceCallback callback) throws RemoteException {
            if (callback != null && callbacks.unregister(callback)) {
                callbacksCount -= 1;
                if (callbacksCount == 0 && timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        }

        @Override
        public void use(Config config) throws RemoteException {
            synchronized (BaseVpnService.class) {
                switch (mState) {
                    case State.STOPPED:
                        if (config != null && checkConfig(config)) startRunner(config);
                        break;

                    case State.CONNECTED:
                        if (config == null) {
                            stopRunner(true);
                        } else if (config.profileId != mConfig.profileId && checkConfig(config)) {
                            stopRunner(false);
                            startRunner(config);
                        }
                        break;
                    default:
                        LogUtils.e(TAG, "Illegal state when invoking use: " + mState);
                        break;
                }
            }
        }
    };

    protected boolean checkConfig(Config config) {
        if (TextUtils.isEmpty(config.proxy) || TextUtils.isEmpty(config.sitekey)) {
            changeState(State.STOPPED, null);
            stopRunner(true);
            return false;
        }

        return true;
    }

    protected void startRunner(Config config) {
        mConfig = config;
        startService(new Intent(this, getClass()));
        TrafficMonitor.reset();
        trafficMonitorThread = new TrafficMonitorThread(getApplicationContext());
        trafficMonitorThread.start();

        if (!closeReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SHUTDOWN);
            filter.addAction(Action.CLOSE);
            registerReceiver(mCloseReceiver, filter);
            closeReceiverRegistered = true;
        }
    }

    protected void stopRunner(boolean stopService) {
        LogUtils.d(TAG, "BaseVpnService stopRunner");
        // clean up receiver
        if (closeReceiverRegistered) {
            unregisterReceiver(mCloseReceiver);
            closeReceiverRegistered = false;
        }

        // Make sure update total traffic when stopping the runner
        updateTrafficTotal(TrafficMonitor.txTotal, TrafficMonitor.rxTotal);

        TrafficMonitor.reset();
        if (trafficMonitorThread != null) {
            trafficMonitorThread.stopThread();
            trafficMonitorThread = null;
        }

        // change the state
        changeState(State.STOPPED, null);

        // stop the service if nothing has bound to it
        if (stopService) stopSelf();
    }

    private void updateTrafficTotal(long txTotal, long rxTotal) {
        LogUtils.d(TAG, "BaseVpnService updateTrafficTotal");
        // TODO: 可以在这里将用户使用的总流量进行保存
    }

    @State.StateType
    public int getState() {
        return mState;
    }

    protected void updateTrafficRate() {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (callbacksCount > 0) {
                    long txRate = TrafficMonitor.txRate;
                    long rxRate = TrafficMonitor.rxRate;
                    long txTotal = TrafficMonitor.txTotal;
                    long rxTotal = TrafficMonitor.rxTotal;
                    int size = callbacks.beginBroadcast();
                    for (int i = 0; i < size; i++) {
                        try {
                            callbacks.getBroadcastItem(i).trafficUpdated(txRate, rxRate, txTotal, rxTotal);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            LogUtils.e(TAG, "getBroadcast traffic updated wrong");
                        }
                    }

                    callbacks.finishBroadcast();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    protected void changeState(@State.StateType final int state, final String msg) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mState != state) {
                    if (callbacksCount > 0) {
                        int size = callbacks.beginBroadcast();
                        for (int i = 0; i < size; i++) {
                            try {
                                callbacks.getBroadcastItem(i).stateChanged(state, msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                LogUtils.e(TAG, "getBroadcast state changed wrong");
                            }
                        }

                        callbacks.finishBroadcast();
                    }
                }
                mState = state;
            }
        });
    }
}
