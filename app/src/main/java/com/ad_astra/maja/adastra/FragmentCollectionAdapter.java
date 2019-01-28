package com.ad_astra.maja.adastra;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

public class FragmentCollectionAdapter extends FragmentPagerAdapter {
    public FragmentCollectionAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case 0:
                return new StatsFragment();
            case 1:
                return new HomeFragment();
            default:
                return new GroupsFragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
