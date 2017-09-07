package cc.cloudist.app.ui.base;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.base.common.util.LogUtils;

import cc.cloudist.aidl.IShadowsocksService;
import cc.cloudist.aidl.IShadowsocksServiceCallback;
import cc.cloudist.app.common.Constants;
import cc.cloudist.app.service.ShadowsocksService;

public abstract class ServiceBoundContext extends AppBaseFragment implements IBinder.DeathRecipient {

    private static final String TAG = LogUtils.makeLogTag(ServiceBoundContext.class);

    private IShadowsocksServiceCallback.Stub callback;
    private ShadowsocksServiceConnection connection;
    private boolean callbackRegistered;

    protected IBinder binder;
    protected IShadowsocksService bgService;

    class ShadowsocksServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                binder = service;
                service.linkToDeath(ServiceBoundContext.this, 0);
                bgService = IShadowsocksService.Stub.asInterface(service);
                registerCallback();
                ServiceBoundContext.this.onServiceConnected();
            } catch (RemoteException e) {
                LogUtils.e(TAG, "onServiceConnected: " + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unregisterCallback();
            ServiceBoundContext.this.onServiceDisconnected();
            bgService = null;
            binder = null;
        }
    }

    protected void registerCallback() {
        if (bgService != null && callback != null && !callbackRegistered) {
            try {
                bgService.registerCallback(callback);
            } catch (RemoteException e) {
                LogUtils.e(TAG, "registerCallback: " + e.getMessage());
            }

            callbackRegistered = true;
        }
    }

    protected void unregisterCallback() {
        if (bgService != null && callback != null && callbackRegistered) {
            try {
                bgService.unregisterCallback(callback);
            } catch (RemoteException e) {
                LogUtils.e(TAG, "unregisterCallback: " + e.getMessage());
            }

            callbackRegistered = false;
        }
    }

    protected void attachService(IShadowsocksServiceCallback.Stub callback) {
        this.callback = callback;
        if (bgService == null) {
            Intent intent = new Intent(getActivity(), ShadowsocksService.class);
            intent.setAction(Constants.Action.SERVICE);

            connection = new ShadowsocksServiceConnection();
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
            getActivity().startService(intent);
        }
    }

    protected void detachService() {
        unregisterCallback();
        callback = null;
        if (connection != null) {
            getActivity().unbindService(connection);
            connection = null;
        }

        if (binder != null) {
            binder.unlinkToDeath(this, 0);
            binder = null;
        }

        bgService = null;
    }

    protected abstract void onServiceConnected();

    protected abstract void onServiceDisconnected();
}
