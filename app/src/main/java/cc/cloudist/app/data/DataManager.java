package cc.cloudist.app.data;

import android.support.annotation.NonNull;

import cc.cloudist.app.data.local.PreferenceManager;
import cc.cloudist.app.data.model.Response;
import cc.cloudist.app.data.model.ShadowSocks;
import cc.cloudist.app.data.model.User;
import cc.cloudist.app.data.remote.CloudistService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

@Singleton
public class DataManager {

    private final PreferenceManager mPreferencesHelper;
    private final CloudistService mCloudistService;

    @Inject
    public DataManager(PreferenceManager preferenceManager, CloudistService cloudistService) {
        this.mPreferencesHelper = preferenceManager;
        this.mCloudistService = cloudistService;
    }

    public PreferenceManager getPreferences() {
        return mPreferencesHelper;
    }

    public Observable<ShadowSocks> getShadowSocksConfig() {
        return mCloudistService.getShadowSocksConfig()
                .map(new Func1<Response<List<ShadowSocks>>, ShadowSocks>() {
                    @Override
                    public ShadowSocks call(Response<List<ShadowSocks>> response) {
                        return response.results.get(0);
                    }
                })
                .doOnNext(new Action1<ShadowSocks>() {
                    @Override
                    public void call(ShadowSocks shadowSock) {
                        mPreferencesHelper.putShadowSocks(shadowSock);
                    }
                });
    }

    public Observable<retrofit2.Response<User>> signIn(@NonNull String username, @NonNull String password) {
        return mCloudistService.signIn(username, password)
                .doOnNext(new Action1<retrofit2.Response<User>>() {
                    @Override
                    public void call(retrofit2.Response<User> response) {
                        if (response.body() != null) {
                            mPreferencesHelper.putToken(response.body().sessionToken);
                            mPreferencesHelper.putUserId(response.body().objectId);
                        }
                    }
                });
    }
}
