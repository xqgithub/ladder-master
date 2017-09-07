package cc.cloudist.app.ui.shadowsocks;

import com.base.common.mvp.BasePresenter;
import cc.cloudist.app.data.DataManager;
import cc.cloudist.app.data.model.ShadowSocks;

import javax.inject.Inject;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ShadowSocksPresenter extends BasePresenter<ShadowSocksView> {

    private final DataManager mDataManager;
    private Subscription mSubscription;

    @Inject
    public ShadowSocksPresenter(DataManager dataManager) {
        this.mDataManager = dataManager;
    }

    @Override
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        if (mSubscription != null) mSubscription.unsubscribe();
    }

    public void getShadowsocksConfig() {
        mSubscription = mDataManager.getShadowSocksConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ShadowSocks>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ShadowSocks shadowSocks) {
                    }
                });
    }
}
