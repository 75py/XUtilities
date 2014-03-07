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

import org.apache.commons.lang3.StringUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XResources;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.utilities.XposedModules.HandleInitPackageResources;
import com.nagopy.android.xposed.utilities.XposedModules.InitZygote;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XposedModule;
import com.nagopy.android.xposed.utilities.receiver.AutoBrightnessController;
import com.nagopy.android.xposed.utilities.setting.ModBrightnessSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

@XposedModule(setting = ModBrightnessSettingsGen.class)
public class ModBrightness {

    @InitZygote(summary = "最低輝度")
    public static void minBrightness(StartupParam startupParam,
            ModBrightnessSettingsGen mBrightnessSettings) throws Throwable {
        XResources.setSystemWideReplacement("android", "integer", "config_screenBrightnessDim",
                mBrightnessSettings.minBrightness);
        if (VersionUtil.isJBmr1OrLater()) {
            XResources.setSystemWideReplacement("android", "integer",
                    "config_screenBrightnessSettingMinimum", mBrightnessSettings.minBrightness);
        }
    }

    @InitZygote(summary = "自動輝度調整")
    public static void initZygote(StartupParam startupParam,
            ModBrightnessSettingsGen mBrightnessSettings) throws Throwable {
        // 自動輝度調整
        if (StringUtils.isNotEmpty(mBrightnessSettings.configAutoBrightnessLevels)
                && StringUtils
                        .isNotEmpty(mBrightnessSettings.configAutoBrightnessLcdBacklightValues)) {
            int[] autoBrightnessLevels = makeIntArray(mBrightnessSettings.configAutoBrightnessLevels);
            int[] autoBrightnessLcdBacklightValues = makeIntArray(mBrightnessSettings.configAutoBrightnessLcdBacklightValues);
            if ((autoBrightnessLevels.length + 1) != autoBrightnessLcdBacklightValues.length) {
                throw new IllegalArgumentException("パラメータ数エラー");
            }
            XResources.setSystemWideReplacement("android", "array",
                    "config_autoBrightnessLevels",
                    autoBrightnessLevels);
            XResources.setSystemWideReplacement("android", "array",
                    "config_autoBrightnessLcdBacklightValues",
                    autoBrightnessLcdBacklightValues);

            XResources.setSystemWideReplacement("android", "bool",
                    "config_automatic_brightness_available", true);
        }
    }

    /**
     * カンマ区切り数値の文字列をint配列に変換する.
     * 
     * @param value カンマ区切り数値
     * @return int配列
     */
    private static int[] makeIntArray(String value) {
        String[] split = value.split(",");
        int length = split.length;
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            String trim = split[i].replace(" ", "");
            array[i] = Integer.parseInt(trim);
        }
        return array;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @InitZygote(summary = "輝度調整用デバッグ表示")
    public static void brightnessDebugger(
            final StartupParam startupParam,
            final ModBrightnessSettingsGen settings) throws Throwable {
        if (!settings.brightnessDebugger) {
            return;
        }

        // DisplayPowerControllerのクラスを取得
        final Class<?> displayPowerContoroll = XposedHelpers.findClass(
                "com.android.server.power.DisplayPowerController", null);

        // Contextをセットする
        XposedBridge.hookAllConstructors(displayPowerContoroll, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[1];
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mContext", context);

                XLog.d("mScreenAutoBrightnessSpline", XposedHelpers.getObjectField(
                        param.thisObject, "mScreenAutoBrightnessSpline"));
            }
        });

        XposedHelpers.findAndHookMethod(displayPowerContoroll, "updateAutoBrightness",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (SystemClock.elapsedRealtime() < 60 * 1000) {
                            // 起動一分以内は何もしない
                            return;
                        }

                        Context mContext = (Context) XposedHelpers.getAdditionalInstanceField(
                                param.thisObject, "mContext");
                        Object mScreenAutoBrightness = XposedHelpers.getObjectField(
                                param.thisObject, "mScreenAutoBrightness");
                        Object mAmbientLux = XposedHelpers.getObjectField(
                                param.thisObject, "mAmbientLux");
                        Intent intent = new Intent(
                                AutoBrightnessController.ACTION_AUTO_BRIGHTNESS_CHANGED);
                        intent.putExtra(AutoBrightnessController.EXTRA_BRIGHTNESS,
                                (Integer) mScreenAutoBrightness);
                        intent.putExtra(AutoBrightnessController.EXTRA_LUX,
                                (Float) mAmbientLux);

                        // センサー値、輝度をブロードキャストで送信
                        UserHandle user = (UserHandle) XposedHelpers.getStaticObjectField(
                                UserHandle.class, "ALL");
                        mContext.sendBroadcastAsUser(intent, user);
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @HandleInitPackageResources(targetPackage = XConst.PKG_SYSTEM_UI, summary = "輝度調整用デバッグ表示")
    public static void brightnessDebugger(
            final String modulePath,
            final InitPackageResourcesParam resparam,
            final ModBrightnessSettingsGen settings) throws Throwable {
        if (!settings.brightnessDebugger) {
            return;
        }

        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout",
                "super_status_bar", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        LinearLayout parent = (LinearLayout) liparam.view
                                .findViewById(liparam.res.getIdentifier(
                                        "system_icon_area", "id", XConst.PKG_SYSTEM_UI));

                        // 明るさ表示を追加
                        TextView luxTextView = new TextView(parent.getContext());
                        luxTextView.setTextSize(8);
                        luxTextView.setSingleLine(false);
                        luxTextView.setTextColor(Color.WHITE);
                        luxTextView.setText("");
                        parent.setGravity(Gravity.CENTER_VERTICAL);
                        parent.addView(luxTextView, 0);
                        AutoBrightnessController autoBrightnessChangedReceiver = new AutoBrightnessController(
                                luxTextView);
                        IntentFilter intentFilter = new IntentFilter(
                                AutoBrightnessController.ACTION_AUTO_BRIGHTNESS_CHANGED);
                        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                        parent.getContext()
                                .registerReceiver(autoBrightnessChangedReceiver, intentFilter);
                    }
                });
    }

}
