package cc.cloudist.app.ui.signin;

import com.base.common.mvp.MvpView;

import cc.cloudist.app.data.model.User;

public interface SignInView extends MvpView {

    void showProgress(boolean show);

    void showError(String message);

    void onSignInSuccessful(User user);
}
