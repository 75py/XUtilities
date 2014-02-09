/*
 * Copyright (C) 2013 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.xposed.utilities;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.nagopy.android.xposed.ProcessorStringUtil;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.utilities.util.AnalyticsUtil;
import com.nagopy.android.xposed.utilities.util.Const;

/**
 * 設定画面を表示するアクティビティ.
 */
public class PrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(
                sp.getBoolean(Const.KEY_GA_OPTOUT, false));
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        // ヘッダー読み込み
        loadHeadersFromResource(R.xml.pref_header, target);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // 初回起動ならダイアログを表示
        if (defaultSharedPreferences.getBoolean(Const.KEY_FIRST_FLAG, true)) {
            // フラグを消す
            defaultSharedPreferences.edit().putBoolean(Const.KEY_FIRST_FLAG, false).apply();

            GASettingDialogFragment dialogFragment = new GASettingDialogFragment();
            dialogFragment.show(getFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navi up
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // 変更された値によってブロードキャストを送信するか調べる
        Object newValue = sharedPreferences.getAll().get(key);
        XLog.d("changed:" + key + "," + newValue);

        Intent intent = new Intent();
        intent.putExtra("target", ProcessorStringUtil.snakeToCamel(key));
        intent.putExtra("value", newValue instanceof String ? (String) newValue
                : newValue instanceof Integer ? (Integer) newValue
                        : newValue instanceof Boolean ? (Boolean) newValue : "");
        // Preferenceのタイトルのリソースは全てR.string.title_キーにしているので、それを使用してタイトルのリソースIDを取得
        int titleId = getResources().getIdentifier("title_" + key, "string", getPackageName());
        switch (titleId) {
            case R.string.title_master_mod_status_bar_enable:
            case R.string.title_status_bar_clock_text_size:
            case R.string.title_status_bar_clock_text_color:
            case R.string.title_status_bar_clock_force_english:
            case R.string.title_status_bar_clock_format:
            case R.string.title_status_bar_clock_gravity_bottom:
            case R.string.title_status_bar_clock_gravity_right:
                intent.setAction(Const.ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED);
                break;
            case R.string.title_master_mod_lockscreen_clock_enable:
            case R.string.title_lockscreen_clock_date_format:
            case R.string.title_lockscreen_clock_date_text_color:
            case R.string.title_lockscreen_clock_date_text_size:
            case R.string.title_lockscreen_clock_date_force_english:
            case R.string.title_lockscreen_clock_time_format:
            case R.string.title_lockscreen_clock_time_text_color:
            case R.string.title_lockscreen_clock_time_text_size:
            case R.string.title_lockscreen_clock_time_force_english:
                intent.setAction(Const.ACTION_LOCKSCREEN_CLOCK_SETTING_CHANGED);
                break;
            case R.string.ga_dllow_anonymous_usage_reports:
                // OptOut
                GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(
                        sharedPreferences.getBoolean(Const.KEY_GA_OPTOUT, false));
                return;
        }
        // フォント選択で変更があったとき用の処理
        if (key.startsWith("status_bar_clock_typeface")) {
            intent.setAction(Const.ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED);
        }
        if (key.startsWith("lockscreen_clock_date_typeface")
                || key.startsWith("lockscreen_clock_time_typeface")) {
            intent.setAction(Const.ACTION_LOCKSCREEN_CLOCK_SETTING_CHANGED);
        }

        if (intent.getAction() != null) {
            // ブロードキャストを送信
            XLog.d("sendBroadcast:" + key + "," + sharedPreferences.getAll().get(key));
            sendBroadcast(intent);
        }

        // 設定変更をGAでトラッキング
        AnalyticsUtil.pushSettingChengedEvent(getApplicationContext(), key, newValue);
    }

    /**
     * 各モジュールの設定画面用フラグメント.<br>
     * XMLのextraで、name="xml_name"、value="@xml/resource"として使用する。
     */
    public static class ModPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // XMLのリソースを取得してセット
            // extraで渡される値は「res/xml/abc.xml」なので、前後を削除
            // TODO これで良いのか確認？
            String name = getArguments().getString("xml_name").replace("res/xml/", "")
                    .replace(".xml", "");
            int id = getActivity().getResources().getIdentifier(name, "xml",
                    getActivity().getPackageName());
            addPreferencesFromResource(id);
        }

        @Override
        public void onResume() {
            super.onResume();
            // ナビの戻るボタンを表示
            getActivity().getActionBar().setDisplayShowHomeEnabled(true);
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        @Override
        public void onStart() {
            super.onStart();

            // 設定画面名をGAでトラッキング
            AnalyticsUtil.pushOpenScreenEvent(getActivity().getApplicationContext(),
                    getArguments().getString("xml_name"));
        }

        @Override
        public void onStop() {
            super.onStop();

            // 設定画面名をGAでトラッキング
            AnalyticsUtil.pushCloseScreenEvent(getActivity().getApplicationContext(),
                    getArguments().getString("xml_name"));
        }
    }

    public static class ModImmersivePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_mod_immersive_full_screen_mode);

            Preference masterPreference = findPreference("master_mod_immersive_full_screen_mode_enabled");
            Preference modePreference = findPreference("immersive_mode");
            final Preference immersiveModeApps = findPreference("immersive_mode_packages");
            final Preference hideNaviBarApps = findPreference("immersive_navi_bar_packages");
            final SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(getActivity()
                            .getApplicationContext());
            Boolean isCustom = sp
                    .getBoolean("master_mod_immersive_full_screen_mode_enabled", false)
                    && sp.getString("immersive_mode",
                            getString(R.string.immersive_mode_disable)).equals(
                            getString(R.string.immersive_mode_custom));
            immersiveModeApps.setEnabled(isCustom);
            hideNaviBarApps.setEnabled(isCustom);

            OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String key = preference.getKey();

                    Boolean isCustom;
                    if (key.equals("master_mod_immersive_full_screen_mode_enabled")) {
                        isCustom = (Boolean) newValue
                                && sp.getString("immersive_mode",
                                        getString(R.string.immersive_mode_disable)).equals(
                                        getString(R.string.immersive_mode_custom));
                    } else {
                        isCustom = sp
                                .getBoolean("master_mod_immersive_full_screen_mode_enabled", false)
                                && newValue.equals(getString(R.string.immersive_mode_custom));
                    }
                    immersiveModeApps.setEnabled(isCustom);
                    hideNaviBarApps.setEnabled(isCustom);
                    return true;
                }
            };
            masterPreference.setOnPreferenceChangeListener(onPreferenceChangeListener);
            modePreference.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            // ナビの戻るボタンを表示
            getActivity().getActionBar().setDisplayShowHomeEnabled(true);
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
