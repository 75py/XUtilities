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

import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nagopy.android.common.pref.FontListPreference;
import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.XposedModules.XModuleSettings;
import com.nagopy.android.xposed.utilities.XposedModules.XTargetPackage;
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
import de.robv.android.xposed.callbacks.XCallback;

/**
 * ステータスバーの時計をカスタマイズするモジュール.
 */
public class ModStatusBarClock extends AbstractXposedModule implements
        IXposedHookZygoteInit, IXposedHookLoadPackage,
        IXposedHookInitPackageResources {

    private static final String ADDITIONAL_FIELD_FORMAT = Const.ADDITIONAL_DATE_FORMAT;

    @XModuleSettings
    private ModStatusBarClockSettingsGen mStatusBarClockSettings;

    private String modulePath;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
    }

    @XTargetPackage(XConst.PKG_SYSTEM_UI)
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
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
        XposedBridge.hookAllConstructors(clsTicker, new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View mStatusBarView = (View) param.args[2];
                int clock = mStatusBarView.getResources().getIdentifier("clock", "id",
                        XConst.PKG_SYSTEM_UI);
                View clockView = mStatusBarView.getRootView().findViewById(clock);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "clockView",
                        clockView);
            }
        });

        XposedHelpers.findAndHookMethod(clsTicker, "tickerStarting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View clockView = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "clockView");

                // デフォルトの位置にある場合は何もしない
                Object currentPosition = clockView.getTag(R.id.tag_clock_current_position);
                if (currentPosition == null
                        || currentPosition.equals(Const.SB_CLOCK_POSITION_DEFAULT)) {
                    return;
                }

                clockView.setVisibility(View.GONE);

                ViewHolder viewHolder = (ViewHolder) clockView
                        .getTag(R.id.tag_status_bar_clock_view_holder);
                Animation anim = viewHolder.mStatusBarContents.getAnimation();
                clockView.startAnimation(anim);
            }
        });

        XposedHelpers.findAndHookMethod(clsTicker, "tickerDone", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View clockView = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "clockView");

                // デフォルトの位置にある場合は何もしない
                Object currentPosition = clockView.getTag(R.id.tag_clock_current_position);
                if (currentPosition == null
                        || currentPosition.equals(Const.SB_CLOCK_POSITION_DEFAULT)) {
                    return;
                }

                clockView.setVisibility(View.VISIBLE);

                ViewHolder viewHolder = (ViewHolder) clockView
                        .getTag(R.id.tag_status_bar_clock_view_holder);
                Animation anim = viewHolder.mStatusBarContents.getAnimation();
                clockView.startAnimation(anim);
            }
        });

        XposedHelpers.findAndHookMethod(clsTicker, "tickerHalting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View clockView = (View) XposedHelpers.getAdditionalInstanceField(
                        param.thisObject, "clockView");

                // デフォルトの位置にある場合は何もしない
                Object currentPosition = clockView.getTag(R.id.tag_clock_current_position);
                if (currentPosition == null
                        || currentPosition.equals(Const.SB_CLOCK_POSITION_DEFAULT)) {
                    return;
                }

                clockView.setVisibility(View.VISIBLE);

                ViewHolder viewHolder = (ViewHolder) clockView
                        .getTag(R.id.tag_status_bar_clock_view_holder);
                Animation anim = viewHolder.mStatusBarContents.getAnimation();
                if (anim != null) {
                    clockView.startAnimation(anim);
                }
            }
        });

        // キーガード表示中に時計が消えるように
        Class<?> clsPhoneStatusBar = XposedHelpers
                .findClass("com.android.systemui.statusbar.phone.PhoneStatusBar",
                        lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clsPhoneStatusBar, "showClock", boolean.class,
                new XC_MethodReplacement() {

                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        View mStatusBarView = (View) XposedHelpers.getObjectField(param.thisObject,
                                "mStatusBarView");
                        if (mStatusBarView == null)
                            return null;

                        boolean show = (Boolean) param.args[0];
                        int clockId = mStatusBarView.getResources().getIdentifier("clock", "id",
                                XConst.PKG_SYSTEM_UI);
                        View clock = mStatusBarView.getRootView().findViewById(clockId);
                        if (clock != null) {
                            clock.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                        return null;
                    }
                });
    }

    @XTargetPackage(XConst.PKG_SYSTEM_UI)
    @Override
    public void handleInitPackageResources(
            final InitPackageResourcesParam resparam) throws Throwable {
        // レイアウトをごにょごにょ
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout",
                "super_status_bar", new XC_LayoutInflated(-7575) { // 優先度低
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // 時計のビューを取得
                        TextView clock = (TextView) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "clock", "id", XConst.PKG_SYSTEM_UI));

                        // GravityBoxとの競合チェック、ViewHolderセットなど
                        setViewHolder(clock);

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

        private WeakReference<TextView> mClockView;

        public StatusBarClockSettingChangedReceiver(TextView clock,
                ModStatusBarClockSettingsGen settings) {
            super(settings, Const.ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED);
            mClockView = new WeakReference<TextView>(clock);
        }

        @Override
        protected void onDataChanged() {
            TextView clockView = mClockView.get();
            Object dataObj = dataObject.get();
            if (isNotNull(dataObj, clockView)) {
                // 設定変更反映、表示更新
                updateSettings(clockView, (ModStatusBarClockSettingsGen) dataObj);
                updateClock(clockView);
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
            int gravity = getGravityFromSettings(clockModDao);
            clock.setGravity(gravity);
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
            if (clockModDao.statusBarClockPosition.equals(Const.SB_CLOCK_POSITION_CENTER)) {
                if (currentPosition == null
                        || !currentPosition.equals(Const.SB_CLOCK_POSITION_CENTER)) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    params.gravity = Gravity.CENTER;
                    ViewGroup viewSystemIconArea = (ViewGroup) clock.getParent();
                    FrameLayout viewStatusBar = (FrameLayout) ((ViewHolder) clock
                            .getTag(R.id.tag_status_bar_clock_view_holder)).mStatusBarView;
                    viewSystemIconArea.removeView(clock);
                    viewStatusBar.addView(clock, params);
                    clock.setTag(R.id.tag_clock_current_position, Const.SB_CLOCK_POSITION_CENTER);
                }
            } else if (clockModDao.statusBarClockPosition.equals(Const.SB_CLOCK_POSITION_LEFT)) {
                // 左表示
                if (currentPosition == null
                        || !currentPosition.equals(Const.SB_CLOCK_POSITION_LEFT)) {
                    // 追加用LayoutParams作成
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    params.rightMargin = DimenUtil.getPixelFromDp(clock.getContext(), 6);

                    // 親View取得
                    ViewGroup viewSystemIconArea = (ViewGroup) clock.getParent();

                    // 追加する親Viewを取得
                    LinearLayout viewStatusBar = (LinearLayout) ((ViewHolder) clock
                            .getTag(R.id.tag_status_bar_clock_view_holder)).mStatusBarContents;

                    // 現在の親Viewから削除
                    viewSystemIconArea.removeView(clock);

                    // ターゲットのViewに追加
                    viewStatusBar.addView(clock, 0, params);

                    // 今の状態を保存
                    clock.setTag(R.id.tag_clock_current_position,
                            Const.SB_CLOCK_POSITION_LEFT);
                }
            } else if (clockModDao.statusBarClockPosition.equals(Const.SB_CLOCK_POSITION_DEFAULT)) {
                if (currentPosition == null
                        || !currentPosition.equals(Const.SB_CLOCK_POSITION_DEFAULT)) {
                    ViewGroup rootView = (ViewGroup) clock.getRootView();
                    int system_icon_area = clock.getContext().getResources()
                            .getIdentifier("system_icon_area", "id", XConst.PKG_SYSTEM_UI);
                    ViewGroup viewSystemIconArea = (ViewGroup) rootView
                            .findViewById(system_icon_area);

                    // Parentから削除
                    ViewGroup parentView = (ViewGroup) clock.getParent();
                    parentView.removeView(clock);
                    // system_icon_areaに追加
                    viewSystemIconArea.addView(clock);

                    clock.setTag(R.id.tag_clock_current_position,
                            Const.SB_CLOCK_POSITION_DEFAULT);
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
            if (currentPosition != null
                    && !currentPosition.equals(Const.SB_CLOCK_POSITION_DEFAULT)) {
                ViewGroup rootView = (ViewGroup) clock.getRootView();
                int system_icon_area = clock.getContext().getResources()
                        .getIdentifier("system_icon_area", "id", XConst.PKG_SYSTEM_UI);
                ViewGroup viewSystemIconArea = (ViewGroup) rootView
                        .findViewById(system_icon_area);

                // Parentから削除
                ViewGroup parentView = (ViewGroup) clock.getParent();
                parentView.removeView(clock);
                // system_icon_areaに追加
                viewSystemIconArea.addView(clock);

                clock.setTag(R.id.tag_clock_current_position,
                        Const.SB_CLOCK_POSITION_DEFAULT);
            }
        }
    }

    private static void setViewHolder(TextView clockView) {
        Context context = clockView.getContext();
        Resources resources = context.getResources();

        int status_bar_contents = resources.getIdentifier(
                "status_bar_contents", "id", XConst.PKG_SYSTEM_UI);
        int status_bar = resources.getIdentifier(
                "status_bar", "id", XConst.PKG_SYSTEM_UI);

        View rootView = clockView.getRootView();
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.mStatusBarView = rootView.findViewById(status_bar);
        viewHolder.mStatusBarContents = rootView.findViewById(status_bar_contents);
        clockView.setTag(R.id.tag_status_bar_clock_view_holder, viewHolder);
    }

    public static class ViewHolder {
        View mStatusBarView;
        View mStatusBarContents;
    }

    /**
     * 時計を更新する.
     * 
     * @param clock {@link TextView}(Clockクラスのインスタンス）
     */
    private static void updateClock(TextView clock) {
        XposedHelpers.callMethod(clock, "updateClock");
    }

    /**
     * 設定値をGravityの値に変換して返す.
     * 
     * @param settings 設定
     * @return {@link Gravity}
     */
    private static int getGravityFromSettings(ModStatusBarClockSettingsGen settings) {
        int gravity = Gravity.NO_GRAVITY;

        String horizontal = settings.statusBarClockGravityHorizontal;
        if (!TextUtils.isEmpty(horizontal)) {
            if (horizontal.equals(Const.SB_CLOCK_GRAVITY_CENTER_HORIZONTAL)) {
                gravity |= Gravity.CENTER_HORIZONTAL;
            } else if (horizontal.equals(Const.SB_CLOCK_GRAVITY_LEFT)) {
                gravity |= Gravity.LEFT;
            } else if (horizontal.equals(Const.SB_CLOCK_GRAVITY_RIGHT)) {
                gravity |= Gravity.RIGHT;
            }
        }

        String vertical = settings.statusBarClockGravityVertical;
        if (!TextUtils.isEmpty(vertical)) {
            if (vertical.equals(Const.SB_CLOCK_GRAVITY_CENTER_VERTICAL)) {
                gravity |= Gravity.CENTER_VERTICAL;
            } else if (vertical.equals(Const.SB_CLOCK_GRAVITY_TOP)) {
                gravity |= Gravity.TOP;
            } else if (vertical.equals(Const.SB_CLOCK_GRAVITY_BOTTOM)) {
                gravity |= Gravity.BOTTOM;
            }
        }

        return gravity;
    }

}
