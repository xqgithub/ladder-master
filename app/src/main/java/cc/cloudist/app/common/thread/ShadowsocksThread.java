package cc.cloudist.app.common.thread;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.base.common.util.LogUtils;
import com.ndk.System;
import cc.cloudist.app.service.ShadowsocksService;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShadowsocksThread extends Thread {

    private static final String TAG = LogUtils.makeLogTag(ShadowsocksThread.class);

    private volatile boolean isRunning = true;
    private volatile LocalServerSocket mServerSocket = null;
    private ShadowsocksService shadowsocksService;

    private final String PATH;
    private Method getInt;

    public ShadowsocksThread(ShadowsocksService shadowsocksService) {
        this.shadowsocksService = shadowsocksService;
        PATH = shadowsocksService.getApplicationInfo().dataDir + "/protect_path";
        try {
            getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
        } catch (NoSuchMethodException e) {
            LogUtils.e(TAG, "NoSuchMethod: " + e.getMessage());
        }
    }

    public void closeServerSocket() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                LogUtils.e(TAG, "server socket closed wrong: " + e.getMessage());
            }

            mServerSocket = null;
        }
    }

    public void stopThread() {
        isRunning = false;
        closeServerSocket();
    }

    @Override
    public void run() {
        try {
            boolean isDelete = new File(PATH).delete();
            if (isDelete) LogUtils.d(TAG, "protect_path is delete");
        } catch (Exception e) {
            LogUtils.e(TAG, "delete file wrong : " + e.getMessage());
        }

        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            mServerSocket = new LocalServerSocket(localSocket.getFileDescriptor());
        } catch (IOException e) {
            LogUtils.e(TAG, "unable to bind: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);

        while (isRunning) {

            try {
                final LocalSocket socket = mServerSocket.accept();
                pool.execute(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            InputStream input = socket.getInputStream();
                            OutputStream output = socket.getOutputStream();

                            input.read();

                            FileDescriptor[] fds = socket.getAncillaryFileDescriptors();
                            if (fds != null && fds.length != 0) {
                                int fb = (int) getInt.invoke(fds[0]);
                                LogUtils.d(TAG, "file descriptor fb: " + fb);
                                boolean ret = shadowsocksService.protect(fb);

                                // Trick to close file descriptor
                                System.jniclose(fb);

                                if (ret) {
                                    output.write(0);
                                } else {
                                    output.write(1);
                                }
                            }

                            input.close();
                            output.close();
                        } catch (Exception e) {
                            LogUtils.e(TAG, "Error when protect socket: " + e.getMessage());
                        }

                        try {
                            socket.close();
                        } catch (IOException e) {
                            LogUtils.e(TAG, "socket closed wrong: " + e.getMessage());
                        }
                    }
                });
            } catch (IOException e) {
                LogUtils.e(TAG, "Error when accept socket: " + e.getMessage());
                return;
            }

        }
    }
}
