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

import android.annotation.TargetApi;
import android.content.res.XModuleResources;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.utilities.XposedModules.HandleInitPackageResources;
import com.nagopy.android.xposed.utilities.XposedModules.HandleLoadPackage;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XposedModule;
import com.nagopy.android.xposed.utilities.receiver.BatteryController;
import com.nagopy.android.xposed.utilities.setting.ModBatteryIconSettingsGen;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * ステータスバーのバッテリーアイコンをカスタマイズするモジュール.
 */
@XposedModule(setting = ModBatteryIconSettingsGen.class)
@XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ModBatteryIcon {

    private static final int BATTERY_VIEW_ID = 7575;

    @HandleLoadPackage(summary = "バッテリーアイコン変更（アニメーション）", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void handleLoadPackage(final String modulePath,
            final LoadPackageParam lpparam,
            final ModBatteryIconSettingsGen mBatteryIconSettings) throws Throwable {

        if (!mBatteryIconSettings.useCircleBatteryIcon) {
            return;
        }

        // バッテリーアイコン変更
        Class<?> clsPhoneStatusBarTransitions = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarTransitions",
                lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clsPhoneStatusBarTransitions, "applyMode", int.class,
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mLeftSide = XposedHelpers.getObjectField(param.thisObject,
                                "mLeftSide");
                        if (mLeftSide == null)
                            return; // pre-init
                        int mode = (Integer) param.args[0];
                        float newAlphaBC = (Float) XposedHelpers.callMethod(param.thisObject,
                                "getBatteryClockAlpha", mode);
                        View mView = (View) XposedHelpers.getObjectField(param.thisObject, "mView");
                        mView.findViewById(BATTERY_VIEW_ID).setAlpha(newAlphaBC);
                    }
                });
    }

    @HandleInitPackageResources(summary = "バッテリーアイコン変更", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void handleInitPackageResources(
            final String modulePath,
            final InitPackageResourcesParam resparam,
            final ModBatteryIconSettingsGen mBatteryIconSettings) throws Throwable {

        if (!mBatteryIconSettings.useCircleBatteryIcon) {
            return;
        }

        // レイアウトをごにょごにょ
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout",
                "super_status_bar", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        LinearLayout ll = (LinearLayout) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "signal_battery_cluster", "id", XConst.PKG_SYSTEM_UI));

                        // 元々のバッテリーアイコンを非表示にする
                        View batteryMeterView = liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "battery", "id", XConst.PKG_SYSTEM_UI));
                        batteryMeterView.setVisibility(View.GONE);

                        // アイコンのビューを作成
                        ImageView imageView = new ImageView(ll.getContext());
                        imageView.setId(BATTERY_VIEW_ID);
                        BatteryController batteryController = new BatteryController(
                                ll.getContext(), XModuleResources.createInstance(modulePath,
                                        resparam.res));
                        batteryController.addIconView(imageView);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMarginStart(DimenUtil.getPixelFromDp(ll.getContext(), 4));
                        ll.addView(imageView, params);
                    }
                });
    }

}
