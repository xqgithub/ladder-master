package cc.cloudist.app.injection.component;

import android.content.Context;

import cc.cloudist.app.LadderApplication;
import cc.cloudist.app.data.DataManager;
import cc.cloudist.app.data.local.PreferenceManager;
import cc.cloudist.app.injection.ApplicationContext;
import cc.cloudist.app.injection.module.ApplicationModule;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

    void inject(LadderApplication ladderApplication);

    @ApplicationContext
    Context context();

    DataManager dataManager();

    PreferenceManager preferenceManager();

    EventBus eventBus();
}
