package cc.cloudist.app.injection.component;

import cc.cloudist.app.injection.PreFragment;
import cc.cloudist.app.injection.module.FragmentModule;
import cc.cloudist.app.ui.shadowsocks.ShadowSocksFragment;
import dagger.Component;

@PreFragment
@Component(dependencies = ApplicationComponent.class, modules = FragmentModule.class)
public interface FragmentComponent {

    void inject(ShadowSocksFragment shadowSocksFragment);
}
