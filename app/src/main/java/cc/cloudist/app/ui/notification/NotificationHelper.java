package cc.cloudist.app.ui.notification;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.content.ContextCompat;

import com.base.common.util.DeviceUtils;
import com.base.common.util.LogUtils;
import cc.cloudist.aidl.IShadowsocksServiceCallback;
import cc.cloudist.app.R;
import cc.cloudist.app.common.Constants.Action;
import cc.cloudist.app.common.Constants.State;
import cc.cloudist.app.service.BaseVpnService;
import cc.cloudist.app.ui.home.HomeActivity;
import cc.cloudist.app.util.TrafficMonitor;

import java.util.Locale;

public class NotificationHelper {

    private static final String TAG = LogUtils.makeLogTag(NotificationHelper.class);

    private NotificationManager notificationManager;
    private KeyguardManager keyguardManager;
    private BaseVpnService service;
    private boolean showOnUnlock;
    private boolean isVisible = true;
    private NotificationCompat.Builder builder;
    private BigTextStyle style;
    private boolean callbackRegistered;
    private IShadowsocksServiceCallback.Stub callback;

    private BroadcastReceiver lockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update(intent.getAction(), false);
        }
    };

    public NotificationHelper(final BaseVpnService service, String title, boolean visible) {
        this.service = service;
        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        keyguardManager = (KeyguardManager) service.getSystemService(Context.KEYGUARD_SERVICE);
        showOnUnlock = visible && DeviceUtils.isLollipopOrAbove();
        builder = new NotificationCompat.Builder(service)
                .setWhen(0)
                .setColor(ContextCompat.getColor(service, R.color.accent_a700))
                .setTicker(service.getString(R.string.forward_success))
                .setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(service, 0,
                        new Intent(service, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), 0))
                .setSmallIcon(R.drawable.ic_shadowsocks)
                .addAction(R.drawable.ic_navigation_close, service.getString(R.string.stop),
                        PendingIntent.getBroadcast(service, 0, new Intent(Action.CLOSE), 0));

        style = new BigTextStyle(builder);

        callback = new IShadowsocksServiceCallback.Stub() {
            @Override
            public void stateChanged(int state, String msg) throws RemoteException {
            }

            @Override
            public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
                String txR = TrafficMonitor.formatTraffic(txRate);
                String rxR = TrafficMonitor.formatTraffic(rxRate);
                builder.setContentText(String.format(Locale.ENGLISH, service.getString(R.string.traffic_summary), txR, rxR));
                style.bigText(String.format(Locale.ENGLISH, service.getString(R.string.stat_summary), txR, rxR,
                        TrafficMonitor.formatTraffic(txTotal), TrafficMonitor.formatTraffic(rxTotal)));
                show();
            }
        };

        boolean isScreenOn = ((PowerManager) service.getSystemService(Context.POWER_SERVICE)).isScreenOn();
        update(isScreenOn ? Intent.ACTION_SCREEN_ON : Intent.ACTION_SCREEN_OFF, true);

        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        if (showOnUnlock) screenFilter.addAction(Intent.ACTION_USER_PRESENT);
        service.registerReceiver(lockReceiver, screenFilter);
    }

    private void update(String action, boolean forceShow) {
        if (forceShow || service.getState() == State.CONNECTED) {
            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                    setVisible(false, forceShow);
                    unregisterCallback();
                    break;

                case Intent.ACTION_SCREEN_ON:
                    setVisible(showOnUnlock && !keyguardManager.inKeyguardRestrictedInputMode(), forceShow);
                    registerCallback();
                    break;

                case Intent.ACTION_USER_PRESENT:
                    setVisible(showOnUnlock, forceShow);
                    break;
            }
        }
    }

    private void registerCallback() {
        try {
            service.binder.registerCallback(callback);
            callbackRegistered = true;
        } catch (RemoteException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    private void unregisterCallback() {
        if (!callbackRegistered) return;
        try {
            service.binder.unregisterCallback(callback);
            callbackRegistered = false;
        } catch (RemoteException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    public void setVisible(boolean visible, boolean forceShow) {
        if (isVisible != visible) {
            isVisible = visible;
            builder.setPriority(visible ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN);
            show();
        } else if (forceShow) {
            show();
        }
    }

    private void show() {
        service.startForeground(1, builder.build());
    }

    public void destroy() {
        if (lockReceiver != null) {
            service.unregisterReceiver(lockReceiver);
            lockReceiver = null;
        }

        unregisterCallback();
        service.stopForeground(true);
        notificationManager.cancel(1);
    }
}
