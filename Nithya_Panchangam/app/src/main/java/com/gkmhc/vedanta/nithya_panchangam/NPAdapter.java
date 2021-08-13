package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NPAdapter extends FragmentPagerAdapter {
    public static final int NP_TAB_PANCHANGAM = 0;
    public static final int NP_TAB_SANKALPAM = 1;
    public static final int NP_TAB_REMINDER = 2;
    public static final int NP_TAB_ALARM = 3;
    public static final int NP_TAB_STOPWATCH = 4;

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.panchangam_tab_heading,
                                                      R.string.sankalpam_tab_heading,
                                                      R.string.reminder_tab_heading,
                                                      R.string.alarm_tab_heading,
                                                      R.string.stopwatch_tab_heading};
    private final Context mContext;
    private Fragment panchangamFragment = null;
    private Fragment sankalpamFragment = null;
    private Fragment alarmFragment = null;
    private Fragment reminderFragment = null;
    private Fragment stopwatchFragment = null;

    public NPAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // Create a list of tabs
        // Tab 0 - Panchangam Tab
        // Tab 1 - Sankalpam Tab
        // Tab 2 - Alarm Tab
        // Tab 3 - Reminder Tab
        // Tab 4 - Stopwatch Tab
        switch (position) {
            case NP_TAB_PANCHANGAM:
                if (panchangamFragment == null) {
                    panchangamFragment = new Panchangam();
                }
                return panchangamFragment;
            case NP_TAB_SANKALPAM:
                if (sankalpamFragment == null) {
                    sankalpamFragment = new Sankalpam();
                }
                return sankalpamFragment;
            case NP_TAB_ALARM:
                if (alarmFragment == null) {
                    alarmFragment = new Alarm();
                }
                return alarmFragment;
            case NP_TAB_REMINDER:
                if (reminderFragment == null) {
                    reminderFragment = new Reminder();
                }
                return reminderFragment;
            case NP_TAB_STOPWATCH:
                if (stopwatchFragment == null) {
                    stopwatchFragment = new StopWatch();
                }
                return stopwatchFragment;
            default:
                // Should we handle this scenario?
                break;
        }
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return new Panchangam();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        // Tab 0 - Panchangam Tab Title
        // Tab 1 - Sankalpam Tab Title
        // Tab 2 - Reminder Tab Title
        // Tab 3 - Alarm Tab Title
        // Tab 4 - Stopwatch Tab Title
        switch (position) {
            case NP_TAB_PANCHANGAM:
                return mContext.getResources().getString(R.string.panchangam_tab_heading);
            case NP_TAB_SANKALPAM:
                return mContext.getResources().getString(R.string.sankalpam_tab_heading);
            case NP_TAB_ALARM:
                return mContext.getResources().getString(R.string.alarm_tab_heading);
            case NP_TAB_REMINDER:
                return mContext.getResources().getString(R.string.reminder_tab_heading);
            case NP_TAB_STOPWATCH:
                return mContext.getResources().getString(R.string.stopwatch_tab_heading);
        }
        if (position < TAB_TITLES.length) {
            return mContext.getResources().getString(TAB_TITLES[position]);
        }
        return "Undefined";
    }

    @Override
    public int getCount() {
        // Show total pages.
        return TAB_TITLES.length;
    }
}