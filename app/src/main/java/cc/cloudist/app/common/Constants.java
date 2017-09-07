package cc.cloudist.app.common;

import android.support.annotation.IntDef;

public class Constants {

    private Constants() {
    }

    public static final String HEADER_ID = "X-LC-Id";
    public static final String HEADER_KEY = "X-LC-Key";
    public static final String APP_ID = "o9v3Asrc5UIcDicj1W3y2vWK-gzGzoHsz";
    public static final String REST_KEY = "UgMz88KdV55tCX8RgyU7edG0";

    public static class State {
        private State() {
        }

        public static final int CONNECTING = 1;
        public static final int CONNECTED = 2;
        public static final int STOPPING = 3;
        public static final int STOPPED = 4;

        @IntDef({CONNECTED, CONNECTING, STOPPED, STOPPING})
        public @interface StateType {
        }

        public static boolean isAvailable(@StateType int state) {
            return (state != CONNECTED && state != CONNECTING);
        }
    }

    public static class Location {
        private Location() {
        }

        public static final int US = 1;
        public static final int JP = 2;
        public static final int HK = 3;
    }

    public static class Action {
        private Action() {
        }

        public static final String SERVICE = "com.zhgqthomas.github.ss.SERVICE";
        public static final String CLOSE = "com.zhgqthomas.github.ss.CLOSE";
        public static final String QUICK_SWITCH = "com.zhgqthomas.github.ss.QUICK_SWITCH";
    }

    public static class Route {
        public static final String ALL = "all";
        public static final String BYPASS_LAN = "bypass-lan";
        public static final String BYPASS_CHN = "bypass-china";
        public static final String BYPASS_LAN_CHN = "bypass-lan-china";
    }

    public static class Executable {
        public static final String REDSOCKS = "redsocks";
        public static final String PDNSD = "pdnsd";
        public static final String SS_LOCAL = "ss-local";
        public static final String SS_TUNNEL = "ss-tunnel";
        public static final String TUN2SOCKS = "tun2socks";
    }
}
