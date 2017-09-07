package cc.cloudist.app.ui.signin;

import android.support.annotation.NonNull;

import com.base.common.mvp.BasePresenter;
import com.base.common.util.GsonUtils;
import com.base.common.util.LogUtils;

import java.io.IOException;

import javax.inject.Inject;

import cc.cloudist.app.data.DataManager;
import cc.cloudist.app.data.model.Error;
import cc.cloudist.app.data.model.User;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SignInPresenter extends BasePresenter<SignInView> {

    private static final String TAG = LogUtils.makeLogTag(SignInPresenter.class);

    private final DataManager mDataManager;
    private Subscription mSubscription;

    @Inject
    public SignInPresenter(DataManager dataManager) {
        this.mDataManager = dataManager;
    }

    @Override
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        if (mSubscription != null && !retainInstance) mSubscription.unsubscribe();
    }

    public void signIn(@NonNull String username, @NonNull String password) {
        if (isViewAttached()) getMvpView().showProgress(true);
        mSubscription = mDataManager.signIn(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<retrofit2.Response<User>>() {
                    @Override
                    public void onCompleted() {
                        if (isViewAttached()) getMvpView().showProgress(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (isViewAttached()) getMvpView().showProgress(false);
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(retrofit2.Response<User> response) {
                        if (response.body() != null) {
                            LogUtils.d(TAG, "user not null");
                            if (isViewAttached()) getMvpView().onSignInSuccessful(response.body());
                            return;
                        }

                        try {
                            String errorJson = response.errorBody().string();
                            Error error = GsonUtils.fromJsonToObj(errorJson, Error.class);
                            if (isViewAttached()) getMvpView().showError(error.error);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
