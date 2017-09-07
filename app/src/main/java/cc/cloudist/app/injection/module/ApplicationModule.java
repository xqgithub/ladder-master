package cc.cloudist.app.injection.module;

import android.app.Application;
import android.content.Context;

import com.base.common.http.HttpClient;

import cc.cloudist.app.data.remote.CloudistService;
import cc.cloudist.app.data.remote.HeaderInterceptor;
import cc.cloudist.app.injection.ApplicationContext;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    protected final Application mApplication;

    public ApplicationModule(Application application) {
        mApplication = application;
    }

    @Provides
    @ApplicationContext
    Context provideContext() {
        return mApplication;
    }

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    CloudistService provideShadowService() {
        return HttpClient.getInstance(CloudistService.BASE_URL, new HeaderInterceptor())
                .createService(CloudistService.class);
    }
}
