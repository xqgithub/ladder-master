package cc.cloudist.app.common.thread;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.base.common.util.LogUtils;
import cc.cloudist.app.util.TrafficMonitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficMonitorThread extends Thread {

    private static final String TAG = LogUtils.makeLogTag(TrafficMonitorThread.class);

    private final String PATH;
    public volatile boolean isRunning = true;
    public volatile LocalServerSocket serverSocket = null;

    public TrafficMonitorThread(Context context) {
        PATH = context.getApplicationInfo().dataDir + "/stat_path";
    }

    public void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LogUtils.e(TAG, "TrafficMonitorThread server socket closed wrong: " + e.getMessage());
            }

            serverSocket = null;
        }
    }

    public void stopThread() {
        isRunning = false;
        closeServerSocket();
    }

    @Override
    public void run() {
        try {
            boolean isDeleted = new File(PATH).delete();
            if (isDeleted) LogUtils.d(TAG, "/stat_path is deleted");
        } catch (Exception e) {
            LogUtils.e(TAG, "delete file wrong: " + e.getMessage());
        }

        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(localSocket.getFileDescriptor());
        } catch (IOException e) {
            LogUtils.e(TAG, "unable to bind: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(1);

        while (isRunning) {
            try {
                final LocalSocket socket = serverSocket.accept();

                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream input = socket.getInputStream();
                            OutputStream output = socket.getOutputStream();

                            byte[] buffer = new byte[16];
                            if (input.read(buffer) != 16)
                                throw new IOException("Unexpected traffic stat length");
                            ByteBuffer stat = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                            TrafficMonitor.update(stat.getLong(0), stat.getLong(8));

                            output.write(0);

                            input.close();
                            output.close();
                        } catch (IOException e) {
                            LogUtils.e(TAG, "Error when recv traffic stat: " + e.getMessage());
                        }

                        try {
                            socket.close();
                        } catch (IOException e) {
                            LogUtils.e(TAG, "socket close wrong: " + e.getMessage());
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
