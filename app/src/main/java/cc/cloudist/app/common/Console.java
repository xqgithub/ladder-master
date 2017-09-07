package cc.cloudist.app.common;

import com.base.common.util.LogUtils;

import java.util.Arrays;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class Console {

    private static final String TAG = LogUtils.makeLogTag(Console.class);

    private Console() {
    }

    private static Shell.Interactive openShell() {
        return new Shell.Builder().useSH()
                .setWatchdogTimeout(10)
                .open();
    }

    private static Shell.Interactive openRootShell(String context) {
        return new Shell.Builder().setShell(Shell.SU.shell(0, context))
                .setWantSTDERR(true)
                .setWatchdogTimeout(10)
                .open();
    }

    public static void runCommand(String... commands) {
        runCommand(Arrays.asList(commands));
    }

    public static void runCommand(List<String> commands) {
        final Shell.Interactive shell = openShell();
        shell.addCommand(commands, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                LogUtils.d(TAG, "" + output);
                if (exitCode < 0) {
                    shell.close();
                }
            }
        });
        shell.waitForIdle();
        shell.close();
    }

    public static String runRootCommand(String... command) {
        return runRootCommand(Arrays.asList(command));
    }

    public static String runRootCommand(List<String> commands) {
        final Shell.Interactive shell = openRootShell("u:r:init_shell:s0");
        final StringBuilder sb = new StringBuilder();
        shell.addCommand(commands, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode < 0) {
                    shell.close();
                } else {
                    for (String line : output) {
                        sb.append(line).append("\n");
                    }
                }
            }
        });

        if (shell.waitForIdle()) {
            shell.close();
            return sb.toString();
        } else {
            shell.close();
            return null;
        }
    }

    public static boolean isRoot() {
        return Shell.SU.available();
    }
}
