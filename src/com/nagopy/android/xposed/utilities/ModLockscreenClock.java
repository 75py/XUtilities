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
import android.widget.TextClock;
import android.widget.TextView;

import com.nagopy.android.common.pref.FontListPreference;
import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.SettingChangedReceiver;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModLockscreenClockSettingsGen;
import com.nagopy.android.xposed.utilities.util.Const;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

/**
 * ロックスクリーンの時計をカスタマイズするモジュール.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ModLockscreenClock extends AbstractXposedModule implements
        IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private static final String ADDITIONAL_FORMAT = Const.ADDITIONAL_DATE_FORMAT;

    private String modulePath;

    @XResource
    private ModLockscreenClockSettingsGen mLockscreenClockSettings;

    /** フォーマットの保存キー */
    // private static final String ADDITIONAL_FIELD_FORMAT =
    // "modLockscreenClockFormat";

    /** キーガードのパッケージ名 */
    private static final String PACKAGE_KEYGUARD = "com.android.keyguard";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!VersionUtil.isJBmr1OrLater()) {
            log("initZygote. do nothing.");
            return;
        }

        log("initZygote");

        modulePath = startupParam.modulePath;

        // chooseFormatをフック
        // フォーマットに「秒がある場合は秒更新を行う」ロジックがもともとあるので、それを使用するため
        XposedHelpers.findAndHookMethod(TextClock.class, "chooseFormat",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param)
                            throws Throwable {
                        Object format = XposedHelpers
                                .getAdditionalInstanceField(param.thisObject,
                                        ADDITIONAL_FORMAT);
                        if (format == null) {
                            // 追加フィールドがない場合は何もしない
                            return;
                        }
                        // このモジュールで追加した値がある場合は、mFormat12と24に値をセット
                        // オリジナルのchooseFormatでmFormat24またはmFormat12がmFormatに入る
                        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) format;
                        String formatString = simpleDateFormat.toPattern();
                        XposedHelpers.setObjectField(param.thisObject,
                                "mFormat24", formatString);
                        XposedHelpers.setObjectField(param.thisObject,
                                "mFormat12", formatString);
                    }
                });

        // 時計更新メソッドを書き換え
        XposedHelpers.findAndHookMethod(TextClock.class, "onTimeChanged",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param)
                            throws Throwable {
                        Object format = XposedHelpers
                                .getAdditionalInstanceField(param.thisObject,
                                        ADDITIONAL_FORMAT);
                        if (format == null) {
                            // 追加フィールドがない場合は通常の処理を実行
                            return XUtil.invokeOriginalMethod(param);
                        } else {
                            // 設定値を使用して時計を更新
                            TextClock textClock = (TextClock) param.thisObject;
                            Calendar mTime = (Calendar) XposedHelpers
                                    .getObjectField(textClock, "mTime");
                            mTime.setTimeInMillis(System.currentTimeMillis());
                            SimpleDateFormat simpleDateFormat = (SimpleDateFormat) format;
                            textClock.setText(simpleDateFormat.format(mTime
                                    .getTime()));
                            return null;
                        }
                    }
                });

        log("initZygote end");
    }

    @Override
    public void handleInitPackageResources(
            final InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(PACKAGE_KEYGUARD)) {
            // キーガード以外では何もしない
            return;
        }
        if (!VersionUtil.isJBmr1OrLater()) {
            // 4.2未満では何もしない
            log("handleLoadPackage. do nothing.");
            return;
        }
        log("handleInitPackageResources target:" + resparam.packageName);
        // レイアウトをごにょごにょ
        resparam.res.hookLayout(PACKAGE_KEYGUARD, "layout",
                "keyguard_status_view", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // 時計のビューを取得
                        TextClock mDateView = (TextClock) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "date_view", "id", PACKAGE_KEYGUARD));
                        TextClock mClockView = (TextClock) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "clock_view", "id", PACKAGE_KEYGUARD));
                        // デフォルト値を保存
                        mLockscreenClockSettings.defaultTimeTextSize = mClockView
                                .getTextSize();
                        mLockscreenClockSettings.defaultTimeTextColor = mClockView
                                .getTextColors().getDefaultColor();
                        mLockscreenClockSettings.defaultTimeTypeface = mClockView
                                .getTypeface();
                        mLockscreenClockSettings.defaultDateTextSize = mDateView
                                .getTextSize();
                        mLockscreenClockSettings.defaultDateTextColor = mDateView
                                .getTextColors().getDefaultColor();
                        mLockscreenClockSettings.defaultDateTypeface = mDateView
                                .getTypeface();

                        // モジュールリソース取得用の値をDaoに追加
                        mLockscreenClockSettings.moduleResources = XModuleResources
                                .createInstance(modulePath, resparam.res);

                        // モジュールの設定を保存
                        updateSettings(mDateView, mClockView,
                                mLockscreenClockSettings);
                        // 時計を更新
                        update(mDateView, mClockView);

                        // 設定変更をリアルタイムに反映させるためのレシーバーを登録
                        Context context = mClockView.getContext();
                        context.registerReceiver(
                                new ModLockscreenClockSettingChangedReceiver(
                                        mDateView,
                                        mClockView,
                                        mLockscreenClockSettings,
                                        Const.ACTION_LOCKSCREEN_CLOCK_SETTING_CHANGED),
                                new IntentFilter(
                                        Const.ACTION_LOCKSCREEN_CLOCK_SETTING_CHANGED));

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
        // initを実行
        XposedHelpers.callMethod(mDateView, "init");
        XposedHelpers.callMethod(mClockView, "init");
    }

    /**
     * 時計の表示設定を変更する.
     * 
     * @param mDateView {@link TextView}(Clockクラスのインスタンス）
     * @param clockModDao {@link GenModStatusBarClockDao}
     */
    private static void updateSettings(TextView mDateView, TextView mClockView,
            ModLockscreenClockSettingsGen dao) {
        if (dao.masterModLockscreenClockEnable) { // モジュール有効の場合
            // 時計の文字サイズ、色、フォント、フォーマットをセット
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dao.lockscreenClockTimeTextSize / 100f
                            * dao.defaultTimeTextSize);
            mClockView.setTextColor(dao.lockscreenClockTimeTextColor);
            Typeface timeTypeface = FontListPreference.makeTypeface(
                    dao.moduleResources.getAssets(),
                    dao.lockscreenClockTimeTypefaceKbn,
                    dao.lockscreenClockTimeTypefaceName,
                    dao.lockscreenClockTimeTypefaceStyle);
            mClockView.setTypeface(timeTypeface);
            SimpleDateFormat timeFormat = new SimpleDateFormat(
                    dao.lockscreenClockTimeFormat,
                    dao.lockscreenClockTimeForceEnglish ? Locale.ENGLISH
                            : Locale.getDefault());
            XposedHelpers.setAdditionalInstanceField(mClockView,
                    ADDITIONAL_FORMAT, timeFormat);

            // 日付の文字サイズ、色、フォント、フォーマットをセット
            mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    dao.lockscreenClockDateTextSize / 100f
                            * dao.defaultDateTextSize);
            mDateView.setTextColor(dao.lockscreenClockDateTextColor);
            Typeface dateTypeface = FontListPreference.makeTypeface(
                    dao.moduleResources.getAssets(),
                    dao.lockscreenClockDateTypefaceKbn,
                    dao.lockscreenClockDateTypefaceName,
                    dao.lockscreenClockDateTypefaceStyle);
            mDateView.setTypeface(dateTypeface);
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    dao.lockscreenClockDateFormat,
                    dao.lockscreenClockDateForceEnglish ? Locale.ENGLISH
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
    public static class ModLockscreenClockSettingChangedReceiver extends
            SettingChangedReceiver {

        private WeakReference<TextView> mDateView;

        protected ModLockscreenClockSettingChangedReceiver(TextView mDateView,
                TextView mClockView, ModLockscreenClockSettingsGen dataObject,
                String action) {
            super(mClockView, dataObject, action);
            this.mDateView = new WeakReference<TextView>(mDateView);
        }

        @Override
        protected void onDataChanged() {
            Object thisObj = thisObject.get();
            TextView mDateView = this.mDateView.get();
            Object dao = this.dataObject.get();
            if (isNotNull(thisObj, mDateView, dao)) {
                TextView mClockView = (TextView) thisObj;
                // 設定を反映し、表示を更新
                updateSettings(mDateView, mClockView,
                        (ModLockscreenClockSettingsGen) dao);
                update(mDateView, mClockView);
            }
        }
    }

}
