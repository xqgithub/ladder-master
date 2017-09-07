package cc.cloudist.app.common;

import com.base.common.util.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class GuardedProcess extends Process {

    private static final String TAG = LogUtils.makeLogTag(GuardedProcess.class);

    private volatile Thread guardThread;
    private volatile boolean isDestroyed;
    private volatile Process process;
    private volatile IOException ioException;
    private List<String> cmd;

    public GuardedProcess(List<String> cmd) {
        this.cmd = cmd;
    }

    public interface Callback {
        void callback();
    }

    public GuardedProcess start() {
        return start(null);
    }


    public GuardedProcess start(final Callback onRestartCallback) {
        final Semaphore semaphore = new Semaphore(1);

        try {
            semaphore.acquire();

            guardThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        Callback callback = null;
                        while (!isDestroyed) {
                            LogUtils.d(TAG, "start process: " + cmd);
                            long startTime = System.currentTimeMillis();
                            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
                            if (callback == null) {
                                callback = onRestartCallback;
                            } else {
                                callback.callback();
                            }

                            semaphore.release();
                            int exitVal = process.waitFor();
                            if (System.currentTimeMillis() - startTime < 1000) {
                                LogUtils.w(TAG, "process exit too fast, stop guard: " + cmd);
                                isDestroyed = true;
                            }
                        }
                    } catch (InterruptedException e) {
                        LogUtils.d(TAG, "thread interrupt, destroy process: " + e.getMessage());
                        process.destroy();
                    } catch (IOException e) {
                        ioException = e;
                    } finally {
                        semaphore.release();
                    }
                }
            }, "GuardThread-" + cmd);

        } catch (InterruptedException e) {
            LogUtils.e(TAG, e.getMessage());
        }


        try {
            guardThread.start();
            semaphore.acquire();
        } catch (InterruptedException e) {
            LogUtils.e(TAG, e.getMessage());
        }

        if (ioException != null) {
            LogUtils.e(TAG, ioException.getMessage());
        }

        return this;
    }

    @Override
    public void destroy() {
        try {
            isDestroyed = true;
            guardThread.interrupt();
            process.destroy();
            guardThread.join();
        } catch (InterruptedException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    @Override
    public int exitValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getErrorStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int waitFor() throws InterruptedException {
        guardThread.join();
        return 0;
    }

    class StreamWatch extends Thread {
        InputStream is;
        String type;
        List<String> output = new ArrayList<String>();
        boolean debug = true;

        StreamWatch(InputStream is, String type) {
            this(is, type, false);
        }

        StreamWatch(InputStream is, String type, boolean debug) {
            this.is = is;
            this.type = type;
            this.debug = debug;
        }

        public void run() {
            PrintWriter pw = null;
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            try {
                while ((line = br.readLine()) != null) {
                    output.add(line);
                    if (debug)
                        System.out.println(type + ">" + line);
                }
                if (pw != null)
                    pw.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    if (isr != null) {
                        isr.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public List<String> getOutput() {
            return output;
        }
    }


}
