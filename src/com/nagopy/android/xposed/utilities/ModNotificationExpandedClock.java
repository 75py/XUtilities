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

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nagopy.android.common.pref.FontListPreference;
import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.SettingChangedReceiver;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModNotificationExpandedClockSettingsGen;
import com.nagopy.android.xposed.utilities.util.Const;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * ロックスクリーンの時計をカスタマイズするモジュール.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ModNotificationExpandedClock extends AbstractXposedModule implements
        IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final String ADDITIONAL_FORMAT = Const.ADDITIONAL_DATE_FORMAT;

    private String modulePath;

    @XResource
    private ModNotificationExpandedClockSettingsGen mSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!VersionUtil.isJBmr1OrLater()) {
            log("initZygote. do nothing.");
            return;
        }

        log("initZygote");

        modulePath = startupParam.modulePath;

        log("initZygote end");
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!XUtil.isSystemUi(lpparam)) {
            return;
        }

        // Clockのクラスを取得
        final Class<?> clockClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.policy.Clock",
                lpparam.classLoader);

        // 時計の文字を返すメソッドを書き換え
        XposedHelpers.findAndHookMethod(clockClass, "getSmallTime",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param)
                            throws Throwable {
                        Object additionalInstanceField = XposedHelpers
                                .getAdditionalInstanceField(param.thisObject,
                                        ADDITIONAL_FORMAT);
                        log(additionalInstanceField);
                        if (additionalInstanceField == null) {
                            // モジュールで追加した値がない場合は元のメソッドを実行
                            return XUtil.invokeOriginalMethod(param);
                        }

                        // モジュールで設定したフォーマットを使用して時計の文字を作成する
                        Calendar mCalendar = (Calendar) XposedHelpers
                                .getObjectField(param.thisObject, "mCalendar");
                        SimpleDateFormat mClockFormat = (SimpleDateFormat) additionalInstanceField;
                        return mClockFormat.format(mCalendar.getTime());
                    }
                });

        Class<?> clsDateView = XposedHelpers.findClass(
                "com.android.systemui.statusbar.policy.DateView",
                lpparam.classLoader);
        // updateClock
        XposedHelpers.findAndHookMethod(clsDateView, "updateClock", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object mDateFormat = XposedHelpers.getObjectField(param.thisObject, "mDateFormat");
                if (mDateFormat == null) {
                    // フォーマットを更新

                    Object additionalInstanceField = XposedHelpers
                            .getAdditionalInstanceField(param.thisObject,
                                    ADDITIONAL_FORMAT);
                    if (additionalInstanceField == null) {
                        // モジュールで追加した値がない場合は元のメソッドを実行
                        return;
                    }

                    // フォーマットをセット
                    XposedHelpers.setObjectField(param.thisObject, "mDateFormat",
                            additionalInstanceField);
                }
            }
        });
    }

    @Override
    public void handleInitPackageResources(
            final InitPackageResourcesParam resparam) throws Throwable {
        if (!XUtil.isSystemUi(resparam)) {
            return;
        }
        if (!VersionUtil.isJBmr1OrLater()) {
            // 4.2未満では何もしない
            log("handleLoadPackage. do nothing.");
            return;
        }

        log("handleInitPackageResources target:" + resparam.packageName);
        // レイアウトをごにょごにょ
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout",
                "super_status_bar", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // 時計のビューを取得
                        ViewGroup datetimeViewGroup = (ViewGroup) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "datetime", "id", XConst.PKG_SYSTEM_UI));
                        TextView mDateView = (TextView) datetimeViewGroup
                                .findViewById(liparam.res.getIdentifier(
                                        "date", "id", XConst.PKG_SYSTEM_UI));
                        TextView mClockView = (TextView) datetimeViewGroup
                                .findViewById(liparam.res.getIdentifier(
                                        "clock", "id", XConst.PKG_SYSTEM_UI));
                        // デフォルト値を保存
                        mSettings.defaultTimeTextSize = mClockView
                                .getTextSize();
                        mSettings.defaultTimeTextColor = mClockView
                                .getTextColors().getDefaultColor();
                        mSettings.defaultTimeTypeface = mClockView
                                .getTypeface();
                        mSettings.defaultDateTextSize = mDateView
                                .getTextSize();
                        mSettings.defaultDateTextColor = mDateView
                                .getTextColors().getDefaultColor();
                        mSettings.defaultDateTypeface = mDateView
                                .getTypeface();

                        // モジュールリソース取得用の値をDaoに追加
                        mSettings.moduleResources = XModuleResources
                                .createInstance(modulePath, resparam.res);

                        // モジュールの設定を保存
                        updateSettings(mDateView, mClockView,
                                mSettings);
                        // 時計を更新
                        update(mDateView, mClockView);

                        // 設定変更をリアルタイムに反映させるためのレシーバーを登録
                        Context context = mClockView.getContext();
                        context.registerReceiver(
                                new ModNotificationExpandedClockSettingChangedReceiver(
                                        mDateView, mClockView, mSettings,
                                        Const.ACTION_NOTIFICATION_EXPANDED_CLOCK_SETTING_CHANGED),
                                new IntentFilter(
                                        Const.ACTION_NOTIFICATION_EXPANDED_CLOCK_SETTING_CHANGED));

                    }
                });
    }

    /**
     * 表示を更新する.
     * 
     * @param mDateView
     * @param mClockView
     */
    private static void update(TextView mDateView, TextView mClockView) {
        try {
            XposedHelpers.setObjectField(mDateView, "mDateFormat", null);
            XposedHelpers.callMethod(mDateView, "updateClock");

            XposedHelpers.callMethod(mClockView, "updateClock");
        } catch (Throwable t) {
            XLog.e(ModNotificationExpandedClock.class.getSimpleName(), t + ", " + mDateView + ", "
                    + mClockView);
        }
    }

    /**
     * 時計の表示設定を変更する.
     * 
     * @param mDateView {@link TextView}(Clockクラスのインスタンス）
     * @param clockModDao {@link GenModStatusBarClockDao}
     */
    private static void updateSettings(TextView mDateView, TextView mClockView,
            ModNotificationExpandedClockSettingsGen dao) {
        if (dao.masterModNotificationExpandedClockEnable) { // モジュール有効の場合
            // 時計の文字サイズ、色、フォント、フォーマットをセット
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dao.notificationExpandedClockTimeTextSize / 100f
                            * dao.defaultTimeTextSize);
            mClockView.setTextColor(dao.notificationExpandedClockTimeTextColor);
            Typeface timeTypeface = FontListPreference.makeTypeface(
                    dao.moduleResources.getAssets(),
                    dao.notificationExpandedClockTimeTypefaceKbn,
                    dao.notificationExpandedClockTimeTypefaceName,
                    dao.notificationExpandedClockTimeTypefaceStyle);
            XLog.d(ModNotificationExpandedClock.class.getSimpleName(),
                    String.format("%s %s %s", dao.notificationExpandedClockTimeTypefaceKbn,
                            dao.notificationExpandedClockTimeTypefaceName,
                            dao.notificationExpandedClockTimeTypefaceStyle));
            XLog.d(ModNotificationExpandedClock.class.getSimpleName(), "timeTypeface:"
                    + timeTypeface);
            mClockView.setTypeface(timeTypeface);
            SimpleDateFormat timeFormat = new SimpleDateFormat(
                    dao.notificationExpandedClockTimeFormat,
                    dao.notificationExpandedClockTimeForceEnglish ? Locale.ENGLISH
                            : Locale.getDefault());
            XposedHelpers.setAdditionalInstanceField(mClockView,
                    ADDITIONAL_FORMAT, timeFormat);

            // 日付の文字サイズ、色、フォント、フォーマットをセット
            mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dao.notificationExpandedClockDateTextSize / 100f
                            * dao.defaultDateTextSize);
            mDateView.setTextColor(dao.notificationExpandedClockDateTextColor);
            Typeface dateTypeface = FontListPreference.makeTypeface(
                    dao.moduleResources.getAssets(),
                    dao.notificationExpandedClockDateTypefaceKbn,
                    dao.notificationExpandedClockDateTypefaceName,
                    dao.notificationExpandedClockDateTypefaceStyle);
            mDateView.setTypeface(dateTypeface);
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    dao.notificationExpandedClockDateFormat,
                    dao.notificationExpandedClockDateForceEnglish ? Locale.ENGLISH
                            : Locale.getDefault());
            XposedHelpers.setAdditionalInstanceField(mDateView,
                    ADDITIONAL_FORMAT, dateFormat);
        } else { // モジュール無効の場合
            // 時計のデフォルト設定を反映
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dao.defaultTimeTextSize);
            mClockView.setTextColor(dao.defaultTimeTextColor);
            mClockView.setTypeface(dao.defaultTimeTypeface);
            XposedHelpers.removeAdditionalInstanceField(mClockView,
                    ADDITIONAL_FORMAT);

            // 日付のデフォルト設定を反映
            mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dao.defaultDateTextSize);
            mDateView.setTextColor(dao.defaultDateTextColor);
            mDateView.setTypeface(dao.defaultDateTypeface);
            XposedHelpers.removeAdditionalInstanceField(mDateView,
                    ADDITIONAL_FORMAT);
        }
    }

    /**
     * 設定変更を受け取るレシーバー.
     */
    public static class ModNotificationExpandedClockSettingChangedReceiver extends
            SettingChangedReceiver {

        private WeakReference<TextView> mClockView;
        private WeakReference<TextView> mDateView;

        protected ModNotificationExpandedClockSettingChangedReceiver(TextView mDateView,
                TextView mClockView, ModNotificationExpandedClockSettingsGen dataObject,
                String action) {
            super(dataObject, action);
            this.mDateView = new WeakReference<TextView>(mDateView);
            this.mClockView = new WeakReference<TextView>(mClockView);
        }

        @Override
        protected void onDataChanged() {
            TextView mDateView = this.mDateView.get();
            TextView mClockView = this.mClockView.get();
            Object dao = this.dataObject.get();
            if (isNotNull(mClockView, mDateView, dao)) {
                // 設定を反映し、表示を更新
                updateSettings(mDateView, mClockView,
                        (ModNotificationExpandedClockSettingsGen) dao);
                update(mDateView, mClockView);
            }
        }
    }

}
