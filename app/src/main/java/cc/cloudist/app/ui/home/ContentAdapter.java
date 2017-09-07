package cc.cloudist.app.ui.home;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Arrays;
import java.util.List;

public class ContentAdapter extends FragmentPagerAdapter {

    private List<Fragment> mFragments;
    private List<String> mTitles;

    public ContentAdapter(FragmentManager fm, List<Fragment> fragments, String[] titles) {
        super(fm);

        if (fragments.size() != titles.length) {
            throw new IllegalArgumentException("fragments size should equal to titles length");
        }

        mFragments = fragments;
        mTitles = Arrays.asList(titles);

    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position);
    }
}
