package cc.cloudist.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.base.common.util.DeviceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cc.cloudist.app.R;
import cc.cloudist.app.data.model.ShadowSocks;
import cc.cloudist.app.common.Constants.Location;
import cc.cloudist.app.injection.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PreferenceManager {

    private static final String PREF_FILE_NAME = "shadow_pref_file";
    private static final String PREF_KEY_SHADOW_SOCKS = "pref_key_shadow_socks";
    private static final String PREF_KEY_TOKEN = "pref_key_token";
    private static final String PREF_KEY_ROUTE = "pref_key_route";
    private static final String PREF_KEY_USER_ID = "pref_key_user_id";
    private static final String PREF_KEY_SERVER_LOCATION = "pref_key_server_location";

    private final SharedPreferences mPref;
    private final SharedPreferences mSettingPref;
    private final Gson mGson;
    private final Context mContext;

    @Inject
    public PreferenceManager(@ApplicationContext Context context) {
        mContext = context;
        mSettingPref = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        mGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz")
                .create();
    }

    public void clear() {
        mPref.edit().clear().apply();
    }

    public void putShadowSocks(ShadowSocks shadowSocks) {
        mPref.edit().putString(PREF_KEY_SHADOW_SOCKS, mGson.toJson(shadowSocks)).apply();
    }

    public ShadowSocks getShadowSocks() {
        String shadowSocksJson = mPref.getString(PREF_KEY_SHADOW_SOCKS, null);
        if (TextUtils.isEmpty(shadowSocksJson)) return null;
        return mGson.fromJson(shadowSocksJson, ShadowSocks.class);
    }

    public void putToken(String token) {
        mPref.edit().putString(PREF_KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return mPref.getString(PREF_KEY_TOKEN, null);
    }

    public String getRoute() {
        return mSettingPref.getString(mContext.getString(R.string.sp_key_route), "all");
    }

    public void install() {
        mPref.edit().putBoolean(DeviceUtils.getVersionName(), true).apply();
    }

    public boolean isInstalled() {
        return mPref.getBoolean(DeviceUtils.getVersionName(), false);
    }

    public void putUserId(String userId) {
        mPref.edit().putString(PREF_KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return mPref.getString(PREF_KEY_USER_ID, null);
    }

    public void putServerLocation(int location) {
        mPref.edit().putInt(PREF_KEY_SERVER_LOCATION, location).apply();
    }

    public int getServerLocation() {
        return mPref.getInt(PREF_KEY_SERVER_LOCATION, Location.US);
    }
}
