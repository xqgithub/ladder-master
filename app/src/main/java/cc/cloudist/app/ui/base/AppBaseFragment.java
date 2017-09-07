package cc.cloudist.app.ui.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.base.common.base.BaseFragment;

import butterknife.ButterKnife;
import cc.cloudist.app.LadderApplication;
import cc.cloudist.app.injection.component.DaggerFragmentComponent;
import cc.cloudist.app.injection.component.FragmentComponent;
import cc.cloudist.app.injection.module.FragmentModule;
import cc.cloudist.app.ui.widget.LoadingDialog;

public abstract class AppBaseFragment extends BaseFragment {

    private LoadingDialog mLoadingDialog;
    private FragmentComponent mFragmentComponent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(getLayoutRes(), container, false);
        // bind with butter knife
        ButterKnife.bind(this, view);

        return view;
    }

    protected abstract int getLayoutRes();

    protected FragmentComponent getFragmentComponent() {
        if (mFragmentComponent == null) {
            mFragmentComponent = DaggerFragmentComponent.builder()
                    .fragmentModule(new FragmentModule())
                    .applicationComponent(LadderApplication.getInstance().getComponent())
                    .build();
        }

        return mFragmentComponent;
    }

    protected void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = LoadingDialog.newInstance();
        }

        mLoadingDialog.show(getFragmentManager(), LoadingDialog.LOADING);
    }

    protected void dismissDialog() {
        if (mLoadingDialog != null) mLoadingDialog.dismiss();
    }
}
