package cc.cloudist.app;

import com.base.common.BaseApplication;
import cc.cloudist.app.data.local.PreferenceManager;
import cc.cloudist.app.injection.component.ApplicationComponent;
import cc.cloudist.app.injection.component.DaggerApplicationComponent;
import cc.cloudist.app.injection.module.ApplicationModule;

import javax.inject.Inject;

public class LadderApplication extends BaseApplication {

    private static LadderApplication sInstance;

    public static LadderApplication getInstance() {
        return sInstance;
    }

    @Inject
    PreferenceManager mPreferenceManager;
    ApplicationComponent mApplicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        mApplicationComponent.inject(this);
        sInstance = this;
    }

    public ApplicationComponent getComponent() {
        return mApplicationComponent;
    }

    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }
}
