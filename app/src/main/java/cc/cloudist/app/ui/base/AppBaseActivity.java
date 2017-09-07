package cc.cloudist.app.ui.base;

import com.base.common.base.BaseActivity;

import butterknife.ButterKnife;
import cc.cloudist.app.LadderApplication;
import cc.cloudist.app.injection.component.ActivityComponent;
import cc.cloudist.app.injection.component.DaggerActivityComponent;
import cc.cloudist.app.injection.module.ActivityModule;
import cc.cloudist.app.ui.widget.LoadingDialog;

public class AppBaseActivity extends BaseActivity {

    private ActivityComponent mActivityComponent;
    private LoadingDialog mLoadingDialog;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        ButterKnife.bind(this);
    }

    protected ActivityComponent getActivityComponent() {
        if (mActivityComponent == null) {
            mActivityComponent = DaggerActivityComponent.builder()
                    .activityModule(new ActivityModule(this))
                    .applicationComponent(LadderApplication.getInstance().getComponent())
                    .build();
        }
        return mActivityComponent;
    }

    protected void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = LoadingDialog.newInstance();
        }

        mLoadingDialog.show(getSupportFragmentManager(), LoadingDialog.LOADING);
    }

    protected void dismissDialog() {
        if (mLoadingDialog != null) mLoadingDialog.dismiss();
    }
}
