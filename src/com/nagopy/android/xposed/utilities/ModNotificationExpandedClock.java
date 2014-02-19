/*
 * Copyright (C) 2014 75py
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
import java.util.Date;
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
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XTargetPackage;
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
 * 通知ヘッダーの時計をカスタマイズするモジュール.
 */
@XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ModNotificationExpandedClock extends AbstractXposedModule implements
        IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final String ADDITIONAL_FORMAT = Const.ADDITIONAL_DATE_FORMAT;

    private String modulePath;

    @XResource
    private ModNotificationExpandedClockSettingsGen mSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // マスタも随時反映の対象なので、無効であっても処理は続行する
        // if (!mSettings.masterModNotificationExpandedClockEnable) {
        // // モジュールが無効なら何もしない
        // return;
        // }

        modulePath = startupParam.modulePath;
    }

    @XTargetPackage(XConst.PKG_SYSTEM_UI)
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // マスタも随時反映の対象なので、無効であっても処理は続行する
        // if (!mSettings.masterModNotificationExpandedClockEnable) {
        // // モジュールが無効なら何もしない
        // return;
        // }

        // Clockのクラスを取得
        final Class<?> clockClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);

        // 時計の文字を返すメソッドを書き換え
        XposedHelpers.findAndHookMethod(clockClass, "getSmallTime",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param)
                            throws Throwable {
                        Object additionalInstanceField = XposedHelpers
                                .getAdditionalInstanceField(param.thisObject,
                                        ADDITIONAL_FORMAT);
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

        // 日付フォーマットを反映できるようごにょごにょ
        Class<?> clsDateView = XposedHelpers.findClass(
                "com.android.systemui.statusbar.policy.DateView", lpparam.classLoader);
        if (VersionUtil.isKitKatOrLator()) {
            // KitKatの場合はフックで済ませる
            XposedHelpers.findAndHookMethod(clsDateView, "updateClock", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object mDateFormat = XposedHelpers.getObjectField(param.thisObject,
                            "mDateFormat");
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
        } else {
            // JBの場合は諦めて書き換える
            XposedHelpers.findAndHookMethod(clsDateView, "updateClock", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    Object additionalInstanceField = XposedHelpers
                            .getAdditionalInstanceField(param.thisObject,
                                    ADDITIONAL_FORMAT);
                    if (additionalInstanceField == null) {
                        // モジュールで追加した値がない場合は元のメソッドを実行
                        return XUtil.invokeOriginalMethod(param);
                    }

                    TextView thisTextView = (TextView) param.thisObject;
                    Date date = new Date();
                    SimpleDateFormat dateFormat = (SimpleDateFormat) additionalInstanceField;
                    thisTextView.setText(dateFormat.format(date));
                    return null;
                }
            });
        }
    }

    @XTargetPackage(XConst.PKG_SYSTEM_UI)
    @Override
    public void handleInitPackageResources(final InitPackageResourcesParam resparam)
            throws Throwable {
        // レイアウトをごにょごにょ
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout",
                "super_status_bar", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // 時計のビューを取得
                        int id_datetime = liparam.res.getIdentifier(
                                "datetime", "id", XConst.PKG_SYSTEM_UI);
                        ViewGroup datetimeViewGroup = (ViewGroup) liparam.view
                                .findViewById(id_datetime);
                        int id_date = liparam.res.getIdentifier("date", "id", XConst.PKG_SYSTEM_UI);
                        TextView mDateView = (TextView) datetimeViewGroup.findViewById(id_date);
                        int id_clock = liparam.res.getIdentifier(
                                "clock", "id", XConst.PKG_SYSTEM_UI);
                        TextView mClockView = (TextView) datetimeViewGroup.findViewById(id_clock);
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

                        // モジュールリソース取得用の値を保存
                        mSettings.moduleResources = XModuleResources
                                .createInstance(modulePath, resparam.res);

                        // モジュールの設定を保存
                        updateSettings(mDateView, mClockView, mSettings);
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
            XLog.e(ModNotificationExpandedClock.class.getSimpleName(), t + ", " + mDateView
                    + ", "
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
            ModNotificationExpandedClockSettingsGen setting) {
        if (setting.masterModNotificationExpandedClockEnable) {
            // モジュール有効の場合
            // 時計の文字サイズ、色、フォント、フォーマットをセット
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    setting.notificationExpandedClockTimeTextSize / 100f
                            * setting.defaultTimeTextSize);
            mClockView.setTextColor(setting.notificationExpandedClockTimeTextColor);
            Typeface timeTypeface = FontListPreference.makeTypeface(
                    setting.moduleResources.getAssets(),
                    setting.notificationExpandedClockTimeTypefaceKbn,
                    setting.notificationExpandedClockTimeTypefaceName,
                    setting.notificationExpandedClockTimeTypefaceStyle);
            mClockView.setTypeface(timeTypeface);
            SimpleDateFormat timeFormat = new SimpleDateFormat(
                    setting.notificationExpandedClockTimeFormat,
                    setting.notificationExpandedClockTimeForceEnglish ? Locale.ENGLISH
                            : Locale.getDefault());
            XposedHelpers.setAdditionalInstanceField(mClockView,
                    ADDITIONAL_FORMAT, timeFormat);

            // 日付の文字サイズ、色、フォント、フォーマットをセット
            mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    setting.notificationExpandedClockDateTextSize / 100f
                            * setting.defaultDateTextSize);
            mDateView.setTextColor(setting.notificationExpandedClockDateTextColor);
            Typeface dateTypeface = FontListPreference.makeTypeface(
                    setting.moduleResources.getAssets(),
                    setting.notificationExpandedClockDateTypefaceKbn,
                    setting.notificationExpandedClockDateTypefaceName,
                    setting.notificationExpandedClockDateTypefaceStyle);
            mDateView.setTypeface(dateTypeface);
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    setting.notificationExpandedClockDateFormat,
                    setting.notificationExpandedClockDateForceEnglish ? Locale.ENGLISH
                            : Locale.getDefault());
            XposedHelpers.setAdditionalInstanceField(mDateView,
                    ADDITIONAL_FORMAT, dateFormat);
        } else {
            // モジュール無効の場合
            // 時計のデフォルト設定を反映
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    setting.defaultTimeTextSize);
            mClockView.setTextColor(setting.defaultTimeTextColor);
            mClockView.setTypeface(setting.defaultTimeTypeface);
            XposedHelpers.removeAdditionalInstanceField(mClockView,
                    ADDITIONAL_FORMAT);

            // 日付のデフォルト設定を反映
            mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    setting.defaultDateTextSize);
            mDateView.setTextColor(setting.defaultDateTextColor);
            mDateView.setTypeface(setting.defaultDateTypeface);
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
            Object setting = this.dataObject.get();
            if (isNotNull(mClockView, mDateView, setting)) {
                // 設定を反映し、表示を更新
                updateSettings(mDateView, mClockView,
                        (ModNotificationExpandedClockSettingsGen) setting);
                update(mDateView, mClockView);
            }
        }
    }

}
