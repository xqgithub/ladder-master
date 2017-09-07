package cc.cloudist.app.util;

import android.text.TextUtils;

import com.base.common.util.LogUtils;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Utils {

    private static final String TAG = LogUtils.makeLogTag(Utils.class);

    private static final String DEFAULT_IPTABLES = "/system/bin/iptables";
    private static final String ALTERNATIVE_IPTABLES = "iptables";

    private Utils() {
    }

    public static String resolve(String host, int addressType) {
        try {
            Lookup lookup = new Lookup(host, addressType);
            SimpleResolver resolver = new SimpleResolver("114.114.114.114");
            resolver.setTimeout(5);
            lookup.setResolver(resolver);
            Record[] result = lookup.run();
            if (result == null) return null;
            List<Record> records = Arrays.asList(result);
            Collections.shuffle(records);
            for (Record record : records) {
                switch (addressType) {
                    case Type.A:
                        return ((ARecord) record).getAddress().getHostAddress();
                    case Type.AAAA:
                        return ((AAAARecord) record).getAddress().getHostAddress();
                }
            }

        } catch (TextParseException | UnknownHostException e) {
            LogUtils.e(TAG, e.getMessage());
        }

        return null;
    }

    public static String resolve(String host) {
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException e) {
            LogUtils.e(TAG, e.getMessage());
        }

        return null;
    }

    public static String resolve(String host, boolean enableIpv6) {
        if (enableIpv6 && isIpv6Support()) {
            String typeAAAA = resolve(host, Type.AAAA);
            if (TextUtils.isEmpty(typeAAAA)) return typeAAAA;
        }

        String typeA = resolve(host, Type.A);
        if (TextUtils.isEmpty(typeA)) return typeA;

        String result = resolve(host);
        if (TextUtils.isEmpty(result)) return result;

        return null;
    }

    public static boolean isIpv6Support() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        if (addr instanceof Inet6Address) {
                            LogUtils.d(TAG, "IPv6 address detected");
                            return true;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            LogUtils.e(TAG, "Failed to get interfaces' addresses." + e.getMessage());
        }

        return false;
    }

    public static boolean isNumeric(String address) {
        try {
            Method method = InetAddress.class.getMethod("isNumeric", String.class);
            return (boolean) method.invoke(null, address);
        } catch (NoSuchMethodException e) {
            LogUtils.e(TAG, "isNumeric: " + e.getMessage());
        } catch (InvocationTargetException e) {
            LogUtils.e(TAG, "isNumeric:" + e.getMessage());
        } catch (IllegalAccessException e) {
            LogUtils.e(TAG, "isNumeric:" + e.getMessage());
        }
        return false;
    }
}
