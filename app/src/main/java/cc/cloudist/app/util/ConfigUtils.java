package cc.cloudist.app.util;

import com.base.common.util.LogUtils;
import cc.cloudist.aidl.Config;
import cc.cloudist.app.LadderApplication;
import cc.cloudist.app.data.local.PreferenceManager;
import cc.cloudist.app.data.model.ShadowSocks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ConfigUtils {

    private static final String TAG = LogUtils.makeLogTag(ConfigUtils.class);

    private ConfigUtils() {
    }

    public static final String SHADOWSOCKS = "{\"server\": \"%s\", \"server_port\": %d, \"local_port\": %d, \"password\": \"%s\", \"method\":\"%s\", \"timeout\": %d}";
    public static final String REDSOCKS = "base {\n" +
            " log_debug = off;\n" +
            " log_info = off;\n" +
            " log = stderr;\n" +
            " daemon = off;\n" +
            " redirector = iptables;\n" +
            "}\n" +
            "redsocks {\n" +
            " local_ip = 127.0.0.1;\n" +
            " local_port = 8123;\n" +
            " ip = 127.0.0.1;\n" +
            " port = %d;\n" +
            " type = socks5;\n" +
            "}\n";

    public static String PDNSD_LOCAL =
            "global { \n" +
                    " perm_cache = 2048;\n" +
                    " cache_dir = \"%s\";\n" +
                    " server_ip = %s;\n" +
                    " server_port = %d;\n" +
                    " query_method = tcp_only;\n" +
                    " run_ipv4 = on;\n" +
                    " min_ttl = 15m;\n" +
                    " max_ttl = 1w;\n" +
                    " timeout = 10;\n" +
                    " daemon = off;\n" +
                    "}\n" +
                    "\n" +
                    "server {\n" +
                    " label = \"local\";\n" +
                    " ip = 127.0.0.1;\n" +
                    " port = %d;\n" +
                    " %s\n" +
                    " reject_policy = negate;\n" +
                    " reject_recursively = on;\n" +
                    " timeout = 5;\n" +
                    "}\n" +
                    "\n" +
                    "rr {\n" +
                    " name=localhost;\n" +
                    " reverse=on;\n" +
                    " a=127.0.0.1;\n" +
                    " owner=localhost;\n" +
                    " soa=localhost,root.localhost,42,86400,900,86400,86400;\n" +
                    "}";

    public static String PDNSD_DIRECT =
            "global {\n" +
                    " perm_cache = 2048;\n" +
                    " cache_dir = \"%s\";\n" +
                    " server_ip = %s;\n" +
                    " server_port = %d;\n" +
                    " query_method = tcp_only;\n" +
                    " run_ipv4 = on;\n" +
                    " min_ttl = 15m;\n" +
                    " max_ttl = 1w;\n" +
                    " timeout = 10;\n" +
                    " daemon = off;\n" +
                    "}\n" +
                    "\n" +
                    "server {\n" +
                    " label = \"china-servers\";\n" +
                    " ip = 114.114.114.114, 112.124.47.27;\n" +
                    " timeout = 4;\n" +
                    " reject = %s;\n" +
                    " reject_policy = fail;\n" +
                    " reject_recursively = on;\n" +
                    " exclude = %s;\n" +
                    " policy = included;\n" +
                    " uptest = none;\n" +
                    " preset = on;\n" +
                    "}\n" +
                    "\n" +
                    "server {\n" +
                    " label = \"local-server\";\n" +
                    " ip = 127.0.0.1;\n" +
                    " port = %d;\n" +
                    " %s\n" +
                    " reject_policy = negate;\n" +
                    " reject_recursively = on;\n" +
                    "}\n" +
                    "\n" +
                    "rr {\n" +
                    " name=localhost;\n" +
                    " reverse=on;\n" +
                    " a=127.0.0.1;\n" +
                    " owner=localhost;\n" +
                    " soa=localhost,root.localhost,42,86400,900,86400,86400;\n" +
                    "}";

    public static PrintWriter printToFile(File file) {
        PrintWriter print = null;
        try {
            print = new PrintWriter(file);
            return print;
        } catch (FileNotFoundException e) {
            LogUtils.e(TAG, e.getMessage());
            return null;
        }
    }

    public static Config loadFromSharedPreferences() {
        PreferenceManager preferenceManager = LadderApplication.getInstance().getPreferenceManager();
        ShadowSocks shadowSocks = preferenceManager.getShadowSocks();
        String route = preferenceManager.getRoute();

        return new Config(false, false, false, false, false, "shadowsocks", shadowSocks.address, shadowSocks.password,
                shadowSocks.encrypt, "", route, shadowSocks.remotePort, shadowSocks.localPort, -1);
    }

}
