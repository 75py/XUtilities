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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nagopy.android.common.pref.FontListPreference;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModStatusBarClockSettingsGen;
import com.nagopy.android.xposed.utilities.util.Const;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * ステータスバーの時計をカスタマイズするモジュール.
 */
public class ModStatusBarClock extends AbstractXposedModule implements
        IXposedHookZygoteInit, IXposedHookLoadPackage,
        IXposedHookInitPackageResources {

    private static final String ADDITIONAL_FIELD_FORMAT = "modStatusBarClockFormat";

    @XResource
    private ModStatusBarClockSettingsGen mStatusBarClockSettings;

    private String modulePath;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!XUtil.isSystemUi(lpparam)) {
            // システムUI以外では何もしない
            return;
        }
        XLog.d(getClass().getSimpleName(), "handleLoadPackage");

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
                                        ADDITIONAL_FIELD_FORMAT);
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

        // ticker
        Class<?> clsTicker = XposedHelpers
                .findClass("com.android.systemui.statusbar.phone.PhoneStatusBar$MyTicker",
                        lpparam.classLoader);
        XposedBridge.hookAllConstructors(clsTicker, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View mStatusBarView = (View) param.args[2];
                int status_bar_contents = mStatusBarView.getResources().getIdentifier(
                        "status_bar_contents", "id", XConst.PKG_SYSTEM_UI);
                View mStatusBarContents = mStatusBarView.findViewById(status_bar_contents);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mStatusBarContents",
                        mStatusBarContents);

                int clock = mStatusBarContents.getResources().getIdentifier("clock", "id",
                        XConst.PKG_SYSTEM_UI);
                View clockView = mStatusBarView.findViewById(clock);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "clockView",
                        clockView);
            }
        });

        XposedHelpers.findAndHookMethod(clsTicker, "tickerStarting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View mStatusBarContents = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "mStatusBarContents");
                View clockView = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "clockView");

                // デフォルトの位置にある場合は何もしない
                Object currentPosition = clockView.getTag(R.id.tag_clock_current_position);
                if (currentPosition == null
                        || currentPosition.equals(Const.STATUS_BAR_CLOCK_POSITION_DEFAULT)) {
                    return;
                }

                clockView.setVisibility(View.GONE);
                Animation anim = mStatusBarContents.getAnimation();
                clockView.startAnimation(anim);
            }
        });

        XposedHelpers.findAndHookMethod(clsTicker, "tickerDone", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View mStatusBarContents = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "mStatusBarContents");
                View clockView = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "clockView");

                // デフォルトの位置にある場合は何もしない
                Object currentPosition = clockView.getTag(R.id.tag_clock_current_position);
                if (currentPosition == null
                        || currentPosition.equals(Const.STATUS_BAR_CLOCK_POSITION_DEFAULT)) {
                    return;
                }

                clockView.setVisibility(View.VISIBLE);
                Animation anim = mStatusBarContents.getAnimation();
                clockView.startAnimation(anim);
            }
        });

        XposedHelpers.findAndHookMethod(clsTicker, "tickerHalting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View mStatusBarContents = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "mStatusBarContents");
                View clockView = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "clockView");

                // デフォルトの位置にある場合は何もしない
                Object currentPosition = clockView.getTag(R.id.tag_clock_current_position);
                if (currentPosition == null
                        || currentPosition.equals(Const.STATUS_BAR_CLOCK_POSITION_DEFAULT)) {
                    return;
                }

                clockView.setVisibility(View.VISIBLE);
                Animation anim = mStatusBarContents.getAnimation();
                clockView.startAnimation(anim);
            }
        });
    }

    @Override
    public void handleInitPackageResources(
            final InitPackageResourcesParam resparam) throws Throwable {
        if (!XUtil.isSystemUi(resparam)) {
            // システムUI以外では何もしない
            return;
        }
        XLog.d(getClass().getSimpleName(), "handleInitPackageResources");

        // レイアウトをごにょごにょ
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout",
                "super_status_bar", new XC_LayoutInflated() {
                    @SuppressLint("NewApi")
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // 時計のビューを取得
                        TextView clock = (TextView) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "clock", "id", XConst.PKG_SYSTEM_UI));

                        // デフォルト値を保存
                        mStatusBarClockSettings.defaultStatusBarClockTextSize = clock
                                .getTextSize();
                        mStatusBarClockSettings.defaultStatusBarClockTextColor = clock
                                .getTextColors().getDefaultColor();
                        mStatusBarClockSettings.defaultGravity = clock
                                .getGravity();
                        mStatusBarClockSettings.defaultTypeface = clock
                                .getTypeface();

                        // モジュールリソース取得用の値をDaoに追加
                        mStatusBarClockSettings.moduleResources = XModuleResources
                                .createInstance(modulePath, resparam.res);

                        // モジュールの設定を反映
                        updateSettings(clock, mStatusBarClockSettings);

                        // 設定変更をリアルタイムで反映するためのレシーバーを登録
                        clock.getContext()
                                .registerReceiver(
                                        new StatusBarClockSettingChangedReceiver(
                                                clock, mStatusBarClockSettings),
                                        new IntentFilter(
                                                Const.ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED));
                    }
                });

    }

    /**
     * 設定変更を再起動せず反映するためのレシーバー.
     */
    private static class StatusBarClockSettingChangedReceiver extends
            com.nagopy.android.xposed.SettingChangedReceiver {

        public StatusBarClockSettingChangedReceiver(TextView clock,
                ModStatusBarClockSettingsGen clockModDao) {
            super(clock, clockModDao,
                    Const.ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED);
        }

        @Override
        protected void onDataChanged() {
            Object thisObj = thisObject.get();
            Object dataObj = dataObject.get();
            if (isNotNull(dataObj, thisObj)) {
                // 設定変更反映、表示更新
                TextView clockTextView = (TextView) thisObj;
                updateSettings(clockTextView,
                        (ModStatusBarClockSettingsGen) dataObj);
                updateClock(clockTextView);
            }
        }

    }

    /**
     * 時計の表示設定を変更する.
     * 
     * @param clock {@link TextView}(Clockクラスのインスタンス）
     * @param clockModDao {@link GenModStatusBarClockDao}
     */
    private static void updateSettings(TextView clock,
            ModStatusBarClockSettingsGen clockModDao) {
        if (clockModDao.masterModStatusBarEnable) { // モジュール有効
            // 文字サイズ
            clock.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    clockModDao.statusBarClockTextSize / 100f
                            * clockModDao.defaultStatusBarClockTextSize);
            // 色
            clock.setTextColor(clockModDao.statusBarClockTextColor);
            // 配置
            int gravityVertical = clockModDao.statusBarClockGravityBottom ? Gravity.BOTTOM
                    : Gravity.CENTER_VERTICAL;
            int gravityHorizontal = clockModDao.statusBarClockGravityRight ? Gravity.RIGHT
                    : Gravity.CENTER_HORIZONTAL;
            clock.setGravity(gravityHorizontal | gravityVertical);
            // フォント
            clock.setTypeface(FontListPreference.makeTypeface(
                    clockModDao.moduleResources.getAssets(),
                    clockModDao.statusBarClockTypefaceKbn,
                    clockModDao.statusBarClockTypefaceName,
                    clockModDao.statusBarClockTypefaceStyle));

            // 複数行を可能に
            clock.setSingleLine(false);

            // フォーマットを作成し、ADDITIONAL_FIELD_FORMATでセット
            String mClockFormatString = clockModDao.statusBarClockFormat;
            Locale locale = clockModDao.statusBarClockForceEnglish ? Locale.ENGLISH
                    : Locale.getDefault();
            final SimpleDateFormat mClockFormat = new SimpleDateFormat(
                    mClockFormatString, locale);
            XposedHelpers.setAdditionalInstanceField(clock,
                    ADDITIONAL_FIELD_FORMAT, mClockFormat);

            // 表示位置
            Object currentPosition = clock.getTag(R.id.tag_clock_current_position);
            if (clockModDao.statusBarClockPosition
                    .equals(Const.STATUS_BAR_CLOCK_POSITION_CENTER)) {
                if (currentPosition == null
                        || !currentPosition.equals(Const.STATUS_BAR_CLOCK_POSITION_CENTER)) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    LinearLayout viewSystemIconArea = (LinearLayout) clock.getParent();
                    FrameLayout viewStatusBar = (FrameLayout) viewSystemIconArea.getParent()
                            .getParent();
                    viewSystemIconArea.removeView(clock);
                    viewStatusBar.addView(clock, params);
                    clock.setTag(R.id.tag_clock_current_position,
                            Const.STATUS_BAR_CLOCK_POSITION_CENTER);
                }
            } else if (clockModDao.statusBarClockPosition
                    .equals(Const.STATUS_BAR_CLOCK_POSITION_DEFAULT)) {
                if (currentPosition == null
                        || !currentPosition.equals(Const.STATUS_BAR_CLOCK_POSITION_DEFAULT)) {
                    ViewGroup rootView = (ViewGroup) clock.getParent();
                    int system_icon_area = clock.getContext().getResources()
                            .getIdentifier("system_icon_area", "id", XConst.PKG_SYSTEM_UI);
                    ViewGroup viewSystemIconArea = (ViewGroup) rootView
                            .findViewById(system_icon_area);

                    rootView.removeView(clock);
                    viewSystemIconArea.addView(clock);

                    clock.setTag(R.id.tag_clock_current_position,
                            Const.STATUS_BAR_CLOCK_POSITION_DEFAULT);
                }
            }
        } else {// モジュール無効
            // デフォルト値をセットし、ADDITIONAL_FIELD_FORMATを削除
            clock.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    clockModDao.defaultStatusBarClockTextSize);
            clock.setTextColor(clockModDao.defaultStatusBarClockTextColor);
            clock.setGravity(clockModDao.defaultGravity);
            clock.setTypeface(clockModDao.defaultTypeface);
            XposedHelpers.removeAdditionalInstanceField(clock,
                    ADDITIONAL_FIELD_FORMAT);

            // 表示位置
            Object currentPosition = clock.getTag(R.id.tag_clock_current_position);
            if (currentPosition == null
                    || !currentPosition.equals(Const.STATUS_BAR_CLOCK_POSITION_DEFAULT)) {
                ViewGroup rootView = (ViewGroup) clock.getParent();
                int system_icon_area = clock.getContext().getResources()
                        .getIdentifier("system_icon_area", "id", XConst.PKG_SYSTEM_UI);
                ViewGroup viewSystemIconArea = (ViewGroup) rootView
                        .findViewById(system_icon_area);

                rootView.removeView(clock);
                viewSystemIconArea.addView(clock);

                clock.setTag(R.id.tag_clock_current_position,
                        Const.STATUS_BAR_CLOCK_POSITION_DEFAULT);
            }
        }
    }

    /**
     * 時計を更新する.
     * 
     * @param clock {@link TextView}(Clockクラスのインスタンス）
     */
    private static void updateClock(TextView clock) {
        XposedHelpers.callMethod(clock, "updateClock");
    }

}
