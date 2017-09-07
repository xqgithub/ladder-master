package cc.cloudist.app.ui.shadowsocks;

import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import com.base.common.util.LogUtils;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;

import cc.cloudist.app.LadderApplication;
import cc.cloudist.app.R;
import cc.cloudist.app.data.local.PreferenceManager;
import cc.cloudist.app.common.Constants.Route;

public class ShadowSettingFragment extends PreferenceFragmentCompatDividers {

    private static final String TAG = LogUtils.makeLogTag(ShadowSettingFragment.class);

    public static ShadowSettingFragment newInstance() {
        return new ShadowSettingFragment();
    }

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.vpn_setting);

        PreferenceManager preferenceManager = LadderApplication.getInstance().getPreferenceManager();
        String route = preferenceManager.getRoute();

        final ListPreference listPref = (ListPreference) findPreference(getString(R.string.sp_key_route));
        listPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                LogUtils.d(TAG, "value: " + value.toString());
                if (value.toString().equalsIgnoreCase("all")) {
                    listPref.setTitle(getString(R.string.global_mode));
                    listPref.setSummary(getString(R.string.global_mode_summary));
                } else {
                    listPref.setTitle(getString(R.string.auto_mode));
                    listPref.setSummary(getString(R.string.auto_mode_summary));
                }

                return true;
            }
        });

        switch (route) {
            case Route.ALL:
                listPref.setTitle(getString(R.string.global_mode));
                listPref.setSummary(getString(R.string.global_mode_summary));
                break;

            case Route.BYPASS_LAN_CHN:
                listPref.setTitle(getString(R.string.auto_mode));
                listPref.setSummary(getString(R.string.auto_mode_summary));
                break;
        }
    }

}
