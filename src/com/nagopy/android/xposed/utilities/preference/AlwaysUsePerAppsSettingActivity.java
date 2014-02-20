
package com.nagopy.android.xposed.utilities.preference;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.nagopy.android.common.helper.Preferences;
import com.nagopy.android.xposed.utilities.setting.AlwaysUsePerAppsList;
import com.nagopy.android.xposed.utilities.setting.AlwaysUsePerAppsList.PerAppsSetting;

public class AlwaysUsePerAppsSettingActivity extends Activity implements OnClickListener {

    private static final int ID_BUTTON_CHECK_ALL = 1;
    private static final int ID_BUTTON_DELETE = 2;

    private Preferences preferences;
    private AlwaysUsePerAppsList mAlwaysUsePerAppsList;
    private ListView mListView;
    private PerAppsAdapter mAdapter;
    private PackageManager mPackageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();

        preferences = new Preferences(context);
        updateList();
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        mListView = new ListView(context);
        mAdapter = new PerAppsAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        ToggleButton checkAllButton = new ToggleButton(context);
        checkAllButton.setText("Check/Uncheck all");
        checkAllButton.setTextOn("Check/Uncheck all");
        checkAllButton.setTextOff("Check/Uncheck all");
        checkAllButton.setOnClickListener(this);
        checkAllButton.setId(ID_BUTTON_CHECK_ALL);

        Button deleteButton = new Button(context);
        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(this);
        deleteButton.setId(ID_BUTTON_DELETE);

        LinearLayout buttonsLayout = new LinearLayout(context);
        buttonsLayout.addView(checkAllButton);
        buttonsLayout.addView(deleteButton);

        ll.addView(buttonsLayout);
        ll.addView(mListView, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        updateList();

        setContentView(ll);
    }

    private void updateList() {
        mAlwaysUsePerAppsList = preferences.getObject("always_use_per_apps",
                AlwaysUsePerAppsList.class);
    }

    private CharSequence getAppName(String packageName) {
        if (mPackageManager == null) {
            mPackageManager = getPackageManager();
        }
        try {
            return mPackageManager.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES)
                    .loadLabel(mPackageManager);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case ID_BUTTON_CHECK_ALL:
                // 表示中のもののチェック状態を切り替え
                int count = mListView.getCount();
                ToggleButton toggleButton = (ToggleButton) v;
                for (int i = 0; i < count; i++) {
                    mListView.setItemChecked(i, toggleButton.isChecked());
                }
                break;
            case ID_BUTTON_DELETE:
                SparseBooleanArray checked = mListView.getCheckedItemPositions();
                Set<AlwaysUsePerAppsList.PerAppsSetting> set = new HashSet<AlwaysUsePerAppsList.PerAppsSetting>();

                // XLog.d(checked);
                for (int i = 0; i < checked.size(); i++) {
                    if (checked.valueAt(i)) {
                        AlwaysUsePerAppsList.PerAppsSetting perAppsSetting = mAlwaysUsePerAppsList.list
                                .get(checked.keyAt(i));
                        set.add(perAppsSetting);
                    }
                }
                for (AlwaysUsePerAppsList.PerAppsSetting delete : set) {
                    mAlwaysUsePerAppsList.list.remove(delete);
                }
                // XLog.d(set);

                preferences.putObject("always_use_per_apps", mAlwaysUsePerAppsList);
                preferences.apply();

                int c = mListView.getCount();
                for (int i = 0; i < c; i++) {
                    mListView.setItemChecked(i, false);
                }
                break;
        }

        // XLog.d(preferences.getString("always_use_per_apps"));

        updateList();
        mAdapter.notifyDataSetChanged();
    }

    private class PerAppsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mAlwaysUsePerAppsList.list.size();
        }

        @Override
        public Object getItem(int position) {
            return mAlwaysUsePerAppsList.list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getApplicationContext(),
                        android.R.layout.simple_list_item_single_choice, null);
            }
            TextView textView = (TextView) convertView;
            AlwaysUsePerAppsList.PerAppsSetting item = (PerAppsSetting) getItem(position);
            textView.setText(getAppName(item.launchedFromPackageName) + " => "
                    + getAppName(item.targetPackageName));
            return textView;
        }
    }

}
