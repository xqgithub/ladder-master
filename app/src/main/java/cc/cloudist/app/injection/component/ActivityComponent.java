package cc.cloudist.app.injection.component;

import cc.cloudist.app.injection.PerActivity;
import cc.cloudist.app.injection.module.ActivityModule;
import cc.cloudist.app.ui.SplashActivity;
import cc.cloudist.app.ui.signin.SignInActivity;

import dagger.Component;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(SplashActivity splashActivity);

    void inject(SignInActivity signInActivity);
}
