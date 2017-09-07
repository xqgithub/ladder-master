package cc.cloudist.app.ui.shadowsocks;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.base.common.util.LogUtils;
import com.base.common.util.ToastUtils;
import com.github.jorgecastilloprz.FABProgressCircle;
import com.ndk.System;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import cc.cloudist.aidl.IShadowsocksServiceCallback;
import cc.cloudist.app.R;
import cc.cloudist.app.common.Console;
import cc.cloudist.app.common.Constants;
import cc.cloudist.app.common.Constants.Location;
import cc.cloudist.app.common.Constants.State;
import cc.cloudist.app.data.local.PreferenceManager;
import cc.cloudist.app.ui.base.ServiceBoundContext;
import cc.cloudist.app.ui.widget.LocationDotView;
import cc.cloudist.app.util.ConfigUtils;
import cc.cloudist.app.util.TrafficMonitor;

public class ShadowSocksFragment extends ServiceBoundContext implements ShadowSocksView {

    private static final String TAG = LogUtils.makeLogTag(ShadowSocksFragment.class);
    private static final int REQUEST_CONNECT = 1;

    @BindView(R.id.txt_traffic_out)
    TextView mTrafficOut;
    @BindView(R.id.txt_traffic_in)
    TextView mTrafficIn;
    @BindView(R.id.btn_hk)
    Button mHkBtn;
    @BindView(R.id.btn_us)
    Button mUsBtn;
    @BindView(R.id.btn_jp)
    Button mJpBtn;
    @BindView(R.id.connect_vpn)
    TextView mConnectVpn;
    @BindView(R.id.txt_limit)
    TextView mLimitTxt;
    @BindView(R.id.progress_connect_vpn)
    FABProgressCircle mConnectVpnProgress;
    @BindView(R.id.location_hk)
    LocationDotView mHkDot;
    @BindView(R.id.location_jp)
    LocationDotView mJpDot;
    @BindView(R.id.location_us)
    LocationDotView mUsDot;

    @Inject
    PreferenceManager mPreferHelper;
    @Inject
    ShadowSocksPresenter mPresenter;

    boolean isServiceStarted = false;
    boolean isDestroyed;
    int mState = State.STOPPED;
    Handler mUiHandler = new Handler();

    String[] EXECUTABLES = {
            Constants.Executable.PDNSD, Constants.Executable.REDSOCKS,
            Constants.Executable.SS_TUNNEL, Constants.Executable.SS_LOCAL,
            Constants.Executable.TUN2SOCKS
    };

    public static ShadowSocksFragment newInstance() {
        return new ShadowSocksFragment();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.fragment_shadow_socks;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getFragmentComponent().inject(this);
        mPresenter.attachView(this);
        mPresenter.getShadowsocksConfig();

        init();

        updateTraffic(0, 0, 0, 0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                attachService(mCallback);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.detachView(false);
        isDestroyed = true;
        detachService();
        mUiHandler.removeCallbacksAndMessages(null);
    }

    private void init() {
        mTrafficOut.setText(String.format(Locale.ENGLISH, getResources().getString(R.string.string_placeholder), 0));
        mTrafficIn.setText(String.format(Locale.ENGLISH, getResources().getString(R.string.string_placeholder), 0));

        int location = mPreferHelper.getServerLocation();
        showServerLocation(location);
    }

    private IShadowsocksServiceCallback.Stub mCallback = new IShadowsocksServiceCallback.Stub() {
        @Override
        public void stateChanged(final int state, final String msg) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mState == state) return;

                    switch (state) {
                        case State.CONNECTING:
                            mConnectVpn.setActivated(true);
                            mConnectVpn.setText(R.string.connecting);
                            mConnectVpn.setEnabled(false);
                            mConnectVpnProgress.show();
                            break;

                        case State.CONNECTED:
                            if (mState == State.CONNECTING) {
                                mConnectVpnProgress.beginFinalAnimation();
                            } else {
                                mConnectVpnProgress.postDelayed(hideCircle, 1000);
                            }
                            mConnectVpn.setActivated(true);
                            mConnectVpn.setText(R.string.connected);
                            mConnectVpn.setEnabled(true);
                            changeSwitch(true);
                            break;

                        case State.STOPPED:
                            mConnectVpn.setActivated(false);
                            mConnectVpn.setText(R.string.connect);
                            mConnectVpn.setEnabled(true);
                            mConnectVpnProgress.postDelayed(hideCircle, 1000);
                            changeSwitch(false);
                            if (!TextUtils.isEmpty(msg)) {
                                ToastUtils.SHORT.show(getActivity(), msg);
                                LogUtils.e(TAG, "Error to start VPN service: " + msg);
                            }
                            break;

                        case State.STOPPING:
                            mConnectVpn.setActivated(false);
                            mConnectVpn.setEnabled(false);
                            mConnectVpn.setText(R.string.stopping);
                            if (mState == State.CONNECTED) mConnectVpnProgress.show();
                            break;
                    }

                    mState = state;
                }
            });
        }

        @Override
        public void trafficUpdated(final long txRate, final long rxRate, final long txTotal, final long rxTotal) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTraffic(txRate, rxRate, txTotal, rxTotal);
                }
            });
        }
    };

    private void updateTraffic(long txRate, long rxRate, long txTotal, long rxTotal) {
        mTrafficOut.setText(String.format(Locale.ENGLISH, getString(R.string.string_placeholder), TrafficMonitor.formatTraffic(txTotal)));
        mTrafficIn.setText(String.format(Locale.ENGLISH, getString(R.string.string_placeholder), TrafficMonitor.formatTraffic(rxTotal)));
    }

    @Override
    protected void onServiceConnected() {
        if (mConnectVpn != null) mConnectVpn.setEnabled(true);
        updateState();

        if (!mPreferHelper.isInstalled()) {
            mPreferHelper.install();
            recovery();
            updateCurrentProfile();
        }
    }

    @Override
    protected void onServiceDisconnected() {
        if (mConnectVpn != null) mConnectVpn.setEnabled(false);
    }

    @Override
    public void binderDied() {
        detachService();
        crashRecovery();
        attachService(mCallback);
    }

    private void changeSwitch(boolean checked) {
        isServiceStarted = checked;
        mConnectVpn.setText(checked ? R.string.connected : R.string.connect);
        if (mConnectVpn.isEnabled()) {
            mConnectVpn.setEnabled(false);
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mConnectVpn.setEnabled(true);
                }
            }, 1000);
        }
    }

    private void copyAsset(String path) {
        AssetManager assetManager = getActivity().getAssets();
        String[] files = null;

        try {
            files = assetManager.list(path);
        } catch (IOException e) {
            LogUtils.e(TAG, e.getMessage());
        }

        if (files != null) {
            for (String file : files) {
                InputStream in = null;
                OutputStream out = null;

                try {
                    if (path.length() > 0) {
                        in = assetManager.open(path + "/" + file);
                    } else {
                        in = assetManager.open(file);
                    }

                    out = new FileOutputStream(getActivity().getApplicationInfo().dataDir + "/" + file);
                    copyFile(in, out);
                    in.close();
                    out.flush();
                    out.close();

                    in = null;
                    out = null;
                } catch (IOException e) {
                    LogUtils.e(TAG, e.getMessage());
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while (true) {
            read = in.read(buffer);
            if (read == -1) break;

            out.write(buffer, 0, read);
        }
    }

    private void crashRecovery() {
        List<String> cmd = new ArrayList<>();
        for (String executable : EXECUTABLES) {
            cmd.add(String.format(Locale.ENGLISH, "chmod 666 %s/%s-vpn.pid", getActivity().getApplicationInfo().dataDir, executable));
        }

        LogUtils.d(TAG, "chmod vpn.pid: " + cmd);
        Console.runCommand(cmd);

        cmd.clear();

        for (String task : EXECUTABLES) {
            try {
                File pidf = new File(getActivity().getApplicationInfo().dataDir + "/" + task + "-vpn.pid");
                int pid_vpn = new Scanner(pidf).useDelimiter("\\Z").nextInt();
                cmd.add(String.format(Locale.ENGLISH, "kill -9 %d", pid_vpn));
                Process.killProcess(pid_vpn);

            } catch (IOException e) {
                LogUtils.e(TAG, "pid_nat: " + e.getMessage());
            }

            cmd.add(String.format(Locale.ENGLISH, "rm -f %s/%s-vpn.conf", getActivity().getApplicationInfo().dataDir, task));
            cmd.add(String.format(Locale.ENGLISH, "rm -f %s/%s-vpn.pid", getActivity().getApplicationInfo().dataDir, task));
        }

        Console.runCommand(cmd);
    }

    private void cancelStart() {
        changeSwitch(false);
    }

    private void prepareStartService() {
        Intent intent = VpnService.prepare(getActivity());
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
                }
            });
        }
    }

    private Runnable hideCircle = new Runnable() {
        @Override
        public void run() {
            mConnectVpnProgress.hide();
        }
    };

    private void updateState() {
        try {
            if (bgService != null) {
                switch (bgService.getState()) {
                    case State.CONNECTING:
                        isServiceStarted = false;
                        mConnectVpn.setActivated(true);
                        mConnectVpn.setText(R.string.connecting);
                        mConnectVpnProgress.show();
                        break;

                    case State.CONNECTED:
                        isServiceStarted = true;
                        mConnectVpn.setActivated(true);
                        mConnectVpn.setText(R.string.connected);
                        mConnectVpnProgress.postDelayed(hideCircle, 100);
                        break;

                    case State.STOPPING:
                        isServiceStarted = false;
                        mConnectVpn.setText(R.string.stopping);
                        mConnectVpn.setActivated(false);
                        mConnectVpnProgress.show();
                        break;

                    default:
                        isServiceStarted = false;
                        mConnectVpn.setActivated(false);
                        mConnectVpn.setText(R.string.connect);
                        mConnectVpnProgress.postDelayed(hideCircle, 100);
                        break;
                }

                mState = bgService.getState();
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "update state: " + e.getMessage());
        }
    }

    private void updateCurrentProfile() {
        // TODO: 检查是否当前的配置更改了

        if (isServiceStarted) loadService();
    }


    @Override
    public void onResume() {
        super.onResume();

        updateCurrentProfile();

        updateState();
    }

    @Override
    public void onStart() {
        super.onStart();
        registerCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterCallback();
    }

    private void reset() {
        crashRecovery();

        copyAsset(System.getABI());

        List<String> commands = new ArrayList<>();
        for (String executable : EXECUTABLES) {
            commands.add("chmod 755 " + getActivity().getApplicationInfo().dataDir + "/" + executable);
        }

        LogUtils.d(TAG, "chmod executables: " + commands);
        Console.runCommand(commands);
    }

    private void recovery() {
        if (isServiceStarted) stopService();
        reset();

        // TODO: show progress
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                loadService();
                break;
            default:
                cancelStart();
                LogUtils.e(TAG, "Failed to start VpnService");
                break;
        }
    }

    private void stopService() {
        if (bgService != null) {
            try {
                bgService.use(null);
            } catch (RemoteException e) {
                LogUtils.e(TAG, "stop service: " + e.getMessage());
            }
        }
    }

    private void loadService() {
        try {
            bgService.use(ConfigUtils.loadFromSharedPreferences());
//            bgService.use(new Config(false, false, false, false, false, "testhaha", "a.usip.pro", "97629625",
//                    "aes-256-cfb", "", "all", 443, 1080, -1));
            changeSwitch(false);
        } catch (RemoteException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    private void showServerLocation(int location) {
        mUsBtn.setActivated(false);
        mHkBtn.setActivated(false);
        mJpBtn.setActivated(false);

        mUsDot.setVisibility(View.GONE);
        mJpDot.setVisibility(View.GONE);
        mHkDot.setVisibility(View.GONE);

        switch (location) {
            case Location.US:
                mUsBtn.setActivated(true);
                mUsDot.setVisibility(View.VISIBLE);
                mPreferHelper.putServerLocation(location);
                break;

            case Location.HK:
                mHkBtn.setActivated(true);
                mHkDot.setVisibility(View.VISIBLE);
                mPreferHelper.putServerLocation(location);
                break;

            case Location.JP:
                mJpBtn.setActivated(true);
                mJpDot.setVisibility(View.VISIBLE);
                mPreferHelper.putServerLocation(location);
                break;
        }
    }

    @OnClick(R.id.connect_vpn)
    public void onConnectVpnClicked() {
        if (isServiceStarted) {
            stopService();
        } else if (mPreferHelper.getShadowSocks() != null && bgService != null) {
            prepareStartService();
        } else {
            changeSwitch(false);
        }
    }

    @OnClick(R.id.btn_us)
    void onUsBtnClicked() {
        showServerLocation(Location.US);
    }

    @OnClick(R.id.btn_hk)
    void onHkBtnClicked() {
        showServerLocation(Location.HK);
    }

    @OnClick(R.id.btn_jp)
    void onJpBtnClicked() {
        showServerLocation(Location.JP);
    }
}
