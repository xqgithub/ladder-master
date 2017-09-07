package cc.cloudist.app.service;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.base.common.util.DeviceUtils;
import com.base.common.util.LogUtils;
import com.ndk.System;

import net.Net;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import cc.cloudist.aidl.Config;
import cc.cloudist.app.R;
import cc.cloudist.app.common.Constants.Action;
import cc.cloudist.app.common.Constants.Route;
import cc.cloudist.app.common.Constants.State;
import cc.cloudist.app.common.GuardedProcess;
import cc.cloudist.app.common.thread.ShadowsocksThread;
import cc.cloudist.app.data.remote.CloudistService;
import cc.cloudist.app.ui.notification.NotificationHelper;
import cc.cloudist.app.util.ConfigUtils;
import cc.cloudist.app.util.Utils;

public class ShadowsocksService extends BaseVpnService {

    private static final String TAG = LogUtils.makeLogTag(CloudistService.class);

    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN = "26.26.26.%s";
    private static final String PRIVATE_VLAN6 = "fdfe:dcba:9876::%s";

    public ParcelFileDescriptor conn;
    public ShadowsocksThread shadowsocksThread;
    private NotificationHelper notification;

    private Process sslocalProcess;
    private Process sstunnelProcess;
    private Process pdnsdProcess;
    private Process tun2socksProcess;

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (VpnService.SERVICE_INTERFACE.equals(action)) {
            return super.onBind(intent);
        } else if (Action.SERVICE.equals(action)) {
            return binder;
        }

        return null;
    }

    public void onRevoke() {
        stopRunner(true);
    }

    @Override
    protected void stopRunner(boolean stopService) {
        if (shadowsocksThread != null) {
            shadowsocksThread.stopThread();
            shadowsocksThread = null;
        }

        if (notification != null) notification.destroy();

        changeState(State.STOPPING, null);

        killProcesses();

        if (conn != null) {

            try {
                conn.close();
            } catch (IOException e) {
                LogUtils.e(TAG, "stop runner: " + e.getMessage());
            }

            conn = null;
        }

        super.stopRunner(stopService);
    }

    private void killProcesses() {
        if (sslocalProcess != null) {
            sslocalProcess.destroy();
            sslocalProcess = null;
        }

        if (sstunnelProcess != null) {
            sstunnelProcess.destroy();
            sstunnelProcess = null;
        }
        if (tun2socksProcess != null) {
            tun2socksProcess.destroy();
            tun2socksProcess = null;
        }
        if (pdnsdProcess != null) {
            pdnsdProcess.destroy();
            pdnsdProcess = null;
        }
    }

    @Override
    protected void startRunner(Config config) {
        super.startRunner(config);
        shadowsocksThread = new ShadowsocksThread(this);
        shadowsocksThread.start();

        changeState(State.CONNECTING, null);

        if (config != null) {
            // reset the context
            killProcesses();

            // Resolve the server address
            boolean resolved;
            if (!Utils.isNumeric(config.proxy)) {
                String address = Utils.resolve(config.proxy, true);
                if (TextUtils.isEmpty(address)) {
                    resolved = false;
                } else {
                    config.proxy = address;
                    resolved = true;
                }
            } else {
                resolved = true;
            }

            if (!resolved) {
                changeState(State.STOPPED, getString(R.string.invalid_server));
                stopRunner(true);
            } else if (handleConnection()) {
//                SvpnGoThread = new Thread(SvpnGoRunnable);
//                SvpnGoThread.start();
                changeState(State.CONNECTED, null);
                notification = new NotificationHelper(this, getString(R.string.app_name), false);
            } else {
                changeState(State.STOPPED, getString(R.string.service_failed));
                stopRunner(true);
            }
        }
    }

    private boolean handleConnection() {
        startShadowsocksDaemon();
        if (!mConfig.isUdpDns) {
            startDnsDaemon();
            startDnsTunnel();
        }

        int fd = startShadowsocks();
//        LogUtils.i(TAG, "fd----->" + fd);
//        LogUtils.i(TAG, "sendFd(fd)----->" + sendFd(fd));
        return sendFd(fd);
    }

    public void startShadowsocksDaemon() {
        if (!mConfig.route.equals(Route.ALL)) {
            List<String> acls = new ArrayList<>();
            switch (mConfig.route) {
                case Route.BYPASS_LAN:
                    acls.addAll(Arrays.asList(getResources().getStringArray(R.array.private_route)));
                    break;

                case Route.BYPASS_CHN:
                    acls.addAll(Arrays.asList(getResources().getStringArray(R.array.chn_route)));
                    break;

                case Route.BYPASS_LAN_CHN:
                    acls.addAll(Arrays.asList(getResources().getStringArray(R.array.private_route)));
                    acls.addAll(Arrays.asList(getResources().getStringArray(R.array.chn_route)));
                    break;
            }

            PrintWriter printWriter = ConfigUtils
                    .printToFile(new File(getApplicationInfo().dataDir + "/acl.list"));
            for (String acl : acls) {
                printWriter.println(acl);
            }
            printWriter.close();
        }

//        String conf = String.format(Locale.ENGLISH, ConfigUtils.SHADOWSOCKS, mConfig.proxy,
//                mConfig.remotePort, mConfig.localPort, mConfig.sitekey, mConfig.encMethod, 600);
        String conf = String.format(Locale.ENGLISH, ConfigUtils.SHADOWSOCKS, "a.usip.pro",
                443, 1080, "34102130", "aes-256-cfb", 600);
        PrintWriter printWriter = ConfigUtils.printToFile(new File(getApplicationInfo().dataDir + "/ss-local-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        //执行命令build
        String[] cmd = {
                getApplicationInfo().dataDir + "/ss-local", "-V", "-u",
                "-b", "127.0.0.1",
                "-t", "600",
                "-P", getApplicationInfo().dataDir,
                "-c", getApplicationInfo().dataDir + "/ss-local-vpn.conf"
        };

        //加入 acl
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(cmd));

        if (mConfig.isAuth) list.add("-A");

        if (!mConfig.route.equals(Route.ALL)) {
            list.add("--acl");
            list.add(getApplicationInfo().dataDir + "/acl.list");
        }

        sslocalProcess = new GuardedProcess(list).start();
    }

    public void startDnsTunnel() {
        LogUtils.d(TAG, "startDnsTunnel");
//        String conf = String.format(Locale.ENGLISH, ConfigUtils.SHADOWSOCKS, mConfig.proxy, mConfig.remotePort, 8163,
//                mConfig.sitekey, mConfig.encMethod, 10);
        String conf = String.format(Locale.ENGLISH, ConfigUtils.SHADOWSOCKS, "a.usip.pro",
                443, 1080, "34102130", "aes-256-cfb", 10);
        LogUtils.i(TAG, "conf----->" + conf);
        PrintWriter printWriter = ConfigUtils.printToFile(new File(getApplicationInfo().dataDir + "/ss-tunnel-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        String[] cmd = {
                getApplicationInfo().dataDir + "/ss-tunnel"
                , "-V"
                , "-u"
                , "-t", "10"
                , "-b", "127.0.0.1"
                , "-l", "8163"
                , "-L", "8.8.8.8:53"
                , "-P", getApplicationInfo().dataDir
                , "-c", getApplicationInfo().dataDir + "/ss-tunnel-vpn.conf"
        };

        List<String> list = Arrays.asList(cmd);
        if (mConfig.isAuth) list.add("-A");

        sstunnelProcess = new GuardedProcess(list).start();
    }

    private void startDnsDaemon() {
        String ipv6 = mConfig.isIpv6 ? "" : "reject = ::/0;";
        String conf;
        LogUtils.d(TAG, "route: " + mConfig.route);
        if (mConfig.route.equals(Route.BYPASS_CHN) || mConfig.route.equals(Route.BYPASS_LAN_CHN)) {
            // TODO: 5/27/16 可以通过网络获取 reject 和 blacklist 列表
            String reject = getResources().getString(R.string.reject);
            String blackList = getResources().getString(R.string.black_list);

            conf = String.format(Locale.ENGLISH, ConfigUtils.PDNSD_DIRECT, getApplicationInfo().dataDir,
                    "0.0.0.0", 8153, reject, blackList, 8163, ipv6);
        } else {
            conf = String.format(Locale.ENGLISH, ConfigUtils.PDNSD_LOCAL, getApplicationInfo().dataDir,
                    "0.0.0.0", 8153, 8163, ipv6);
        }

        PrintWriter printWriter = ConfigUtils
                .printToFile(new File(getApplicationInfo().dataDir + "/pdnsd-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        String cmd = getApplicationInfo().dataDir + "/pdnsd -c "
                + getApplicationInfo().dataDir + "/pdnsd-vpn.conf";
        List<String> list = Arrays.asList(cmd.split(Pattern.quote(" ")));
        pdnsdProcess = new GuardedProcess(list).start();
    }

    private int startShadowsocks() {
        Builder builder = new Builder();
        builder.setSession(mConfig.profileName)
                .setMtu(VPN_MTU)
                .addAddress(String.format(Locale.ENGLISH, PRIVATE_VLAN, "1"), 24)
                .addDnsServer("8.8.8.8");

        if (mConfig.isIpv6) {
            builder.addAddress(String.format(Locale.ENGLISH, PRIVATE_VLAN6, "1"), 126);
            builder.addRoute("::", 0);
        }

        if (DeviceUtils.isLollipopOrAbove()) {
            if (mConfig.isProxyApps) {
                String[] proxiedAppString = mConfig.proxiedAppString.split(Pattern.quote("\n"));
                for (String pkg : proxiedAppString) {
                    try {
                        if (!mConfig.isBypassApps) {
                            builder.addAllowedApplication(pkg);
                        } else {
                            builder.addDisallowedApplication(pkg);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        LogUtils.e(TAG, "Invalid package name: " + e.getMessage());
                    }
                }
            }
        }

        if (mConfig.route.equals(Route.ALL) || mConfig.route.equals(Route.BYPASS_CHN)) {
            builder.addRoute("0.0.0.0", 0);
        } else {
            String[] routes = getResources().getStringArray(R.array.bypass_private_route);
            for (String route : routes) {
                String addr[] = route.split(Pattern.quote("/"));
                builder.addRoute(addr[0], Integer.valueOf(addr[1]));
            }
        }

        builder.addRoute("8.8.0.0", 16);

        try {
            conn = builder.establish();
            if (conn == null) changeState(State.STOPPED, getString(R.string.reboot_required));
        } catch (IllegalStateException e) {
            LogUtils.e(TAG, e.getMessage());
            changeState(State.STOPPED, e.getMessage());
            conn = null;
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
            conn = null;
        }

        if (conn == null) {
            stopRunner(true);
            return -1;
        }


        String apath = getApplicationInfo().dataDir + "/tun2socks";
        File apathfile = new File(apath);
        LogUtils.i(TAG, "apathfile----->" + apathfile.exists());

        final int fd = conn.getFd();
        String cmd = getApplicationInfo().dataDir +
                "/tun2socks --netif-ipaddr %s "
                + "--netif-netmask 255.255.255.0 "
                + "--socks-server-addr 127.0.0.1:%d "
                + "--tunfd %d "
                + "--tunmtu %d "
                + "--sock-path %s "
                + "--loglevel 5";

//        String formatCmd = String.format(Locale.ENGLISH, cmd, String.format(Locale.ENGLISH, PRIVATE_VLAN, "2"),
//                mConfig.localPort, fd, VPN_MTU, getApplicationInfo().dataDir + "/sock_path");
        String formatCmd = String.format(Locale.ENGLISH, cmd, String.format(Locale.ENGLISH, PRIVATE_VLAN, "2"),
                mConfig.localPort, fd, VPN_MTU, getApplicationInfo().dataDir + "/sock_path");
        if (mConfig.isIpv6)
            formatCmd += " --netif-ip6addr " + String.format(Locale.ENGLISH, PRIVATE_VLAN6, "2");
        if (mConfig.isUdpDns) {
            formatCmd += " --enable-udprelay";
        } else {
            formatCmd += String.format(Locale.ENGLISH, " --dnsgw %s:8153", String.format(Locale.ENGLISH, PRIVATE_VLAN, "1"));
        }

        List<String> list = Arrays.asList(formatCmd.split(Pattern.quote(" ")));

//        String bpath = getApplicationInfo().dataDir + "/sock_path";
//        File bpathfile = new File(bpath);
//        LogUtils.i(TAG, "bpathfile----->" + bpathfile.exists());

        LogUtils.d(TAG, formatCmd);
        tun2socksProcess = new GuardedProcess(list).start(new GuardedProcess.Callback() {
            @Override
            public void callback() {
                sendFd(fd);
            }
        });

        return fd;
    }

    public boolean sendFd(int fd) {
        if (fd != -1) {
            int tries = 1;
            while (tries < 5) {
                try {
                    Thread.sleep(1000 * tries);
                    if (System.sendfd(fd, getApplicationInfo().dataDir + "/sock_path") != -1) {
                        return true;
                    }

                    tries += 1;
                } catch (InterruptedException e) {
                    LogUtils.e(TAG, e.getMessage());
                }
            }
        }
        return false;
    }

    Thread SvpnGoThread;
    Runnable SvpnGoRunnable = new Runnable() {
        @Override
        public void run() {
            String var0 = "ss://chacha20-ietf-poly1305:asdf@119.23.64.170:8388";
            String var1 = "19|1234|34qcPxEJcrE4xVLa41J5";
            String var2 = "127.0.0.1:8838";
            String var3 = "127.0.0.1:8118";
            String var4 = "";
            String var5 = "";
            String var6 = "";
            Net.startSvpnGo(var0, var1, var2, var3, var4, var5, var6);
        }
    };


}
