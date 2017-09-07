package cc.cloudist.app.ui.signin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;

import com.base.common.util.LogUtils;
import com.base.common.util.PatternUtils;
import com.base.common.util.SnackBarUtils;
import com.rengwuxian.materialedittext.MaterialEditText;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.OnTextChanged.Callback;
import cc.cloudist.app.R;
import cc.cloudist.app.data.model.User;
import cc.cloudist.app.ui.base.AppBaseActivity;
import cc.cloudist.app.ui.home.HomeActivity;


public class SignInActivity extends AppBaseActivity implements SignInView {

    private static final String TAG = LogUtils.makeLogTag(SignInActivity.class);

    @BindView(R.id.input_email)
    MaterialEditText mEmailInput;
    @BindView(R.id.input_password)
    MaterialEditText mPasswordInput;
    @BindView(R.id.btn_sign_in)
    AppCompatButton mSignInBtn;

    @Inject
    SignInPresenter mPresenter;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SignInActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        getActivityComponent().inject(this);
        mPresenter.attachView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.detachView(false);
    }

    @Override
    public void showProgress(boolean show) {
        if (show) {
            showLoadingDialog();
        } else {
            dismissDialog();
        }
    }

    @Override
    public void showError(String message) {
        SnackBarUtils.makeShort(mSignInBtn, message).show();
    }

    @Override
    public void onSignInSuccessful(User user) {
        startActivity(HomeActivity.getStartIntent(this, true));
    }

    @OnTextChanged(value = R.id.input_email, callback = Callback.AFTER_TEXT_CHANGED)
    void onEmailTextChanged() {
        String email = mEmailInput.getText().toString();

        if (TextUtils.isEmpty(email)) {
            mEmailInput.setError(getString(R.string.error_email_required));
            return;
        }

        if (isEmailValidate() && isPasswordValidate()) mSignInBtn.setEnabled(true);
    }

    @OnTextChanged(value = R.id.input_password, callback = Callback.AFTER_TEXT_CHANGED)
    void onPasswordTextChanged() {
        if (isPasswordValidate() && isEmailValidate()) mSignInBtn.setEnabled(true);
    }

    @OnClick(R.id.btn_sign_in)
    public void signIn() {
        final String email = mEmailInput.getText().toString();
        final String password = mPasswordInput.getText().toString();

        mPresenter.signIn(email, password);
    }

    private boolean isEmailValidate() {
        String email = mEmailInput.getText().toString();

        if (TextUtils.isEmpty(email) || !PatternUtils.isEmail(email)) {
            mEmailInput.setError(getString(R.string.error_invalid_email));
            return false;
        }

        return true;
    }

    private boolean isPasswordValidate() {
        String password = mPasswordInput.getText().toString();

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            mPasswordInput.setError(getString(R.string.error_password_too_short));
            return false;
        }

        return true;
    }
}
