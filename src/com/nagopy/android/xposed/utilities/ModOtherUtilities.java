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

import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.XResources;
import android.os.Build;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Button;

import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.XposedModules.HandleLoadPackage;
import com.nagopy.android.xposed.utilities.XposedModules.InitZygote;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XposedModule;
import com.nagopy.android.xposed.utilities.setting.ModOtherUtilitiesSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@XposedModule(setting = ModOtherUtilitiesSettingsGen.class)
public class ModOtherUtilities {

    @InitZygote(summary = "IME通知表示有無")
    public static void setShowOngoingImeSwitcher(StartupParam startupParam,
            ModOtherUtilitiesSettingsGen mOtherUtilitiesSettings) throws Throwable {
        XResources.setSystemWideReplacement("android", "bool", "show_ongoing_ime_switcher",
                mOtherUtilitiesSettings.showOngoingImeSwitcher);
    }

    @InitZygote(summary = "ActionMenuのテキスト表示")
    public static void setConfigAllowActionMenuItemTextWithIcon(StartupParam startupParam,
            ModOtherUtilitiesSettingsGen mOtherUtilitiesSettings) throws Throwable {
        XResources.setSystemWideReplacement("android", "bool",
                "config_allowActionMenuItemTextWithIcon",
                mOtherUtilitiesSettings.configAllowActionMenuItemTextWithIcon);
    }

    @InitZygote(summary = "カメラ シャッター音")
    @XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void setConfigCameraSoundForced(StartupParam startupParam,
            ModOtherUtilitiesSettingsGen mOtherUtilitiesSettings) throws Throwable {
        XResources.setSystemWideReplacement("android", "bool", "config_camera_sound_forced",
                mOtherUtilitiesSettings.configCameraSoundForced);
    }

    @InitZygote(summary = "音量ボタンでスリープ復帰")
    @XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void volumeRockerWake(StartupParam startupParam,
            ModOtherUtilitiesSettingsGen mOtherUtilitiesSettings) throws Throwable {
        if (!mOtherUtilitiesSettings.volumeRockerWake) {
            return;
        }

        // PhoneWindowManagerのクラスを取得
        Class<?> phoneWindowManager = XposedHelpers.findClass(
                "com.android.internal.policy.impl.PhoneWindowManager", null);
        // isWakeKeyWhenScreenOffを書き換え
        XposedHelpers.findAndHookMethod(phoneWindowManager, "isWakeKeyWhenScreenOff",
                int.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param)
                            throws Throwable {
                        int keyCode = (Integer) param.args[0];
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_VOLUME_DOWN:
                            case KeyEvent.KEYCODE_VOLUME_UP:
                                // 音量アップ・ダウンボタンの場合はtrueを返す
                                return true;
                            default:
                                // それ以外のボタンの場合は、オリジナルのメソッドを実行
                                return XUtil.invokeOriginalMethod(param);
                        }
                    }
                });
    }

    @HandleLoadPackage(summary = "メニューキー表示", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void setMenuVisibility(String modulePath, LoadPackageParam lpparam,
            ModOtherUtilitiesSettingsGen mOtherUtilitiesSettings) throws Throwable {
        if (!mOtherUtilitiesSettings.showMenuKey) {
            return;
        }
        Class<?> navigationBarViewClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.NavigationBarView", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(navigationBarViewClass, "setMenuVisibility",
                boolean.class, boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param)
                            throws Throwable {
                        Context mContext = (Context) XposedHelpers.getObjectField(
                                param.thisObject, "mContext");
                        KeyguardManager keyguardManager = (KeyguardManager) mContext
                                .getSystemService(Context.KEYGUARD_SERVICE);
                        if (keyguardManager.inKeyguardRestrictedInputMode()) {
                            return XUtil.invokeOriginalMethod(param);
                        }

                        // キーガード表示中以外ではtrue（表示）にして実行する
                        Object[] args = {
                                true, true
                        };
                        return XposedBridge.invokeOriginalMethod(param.method,
                                param.thisObject, args);
                    }
                });
        XposedBridge.hookAllConstructors(navigationBarViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "mShowMenu", true);
            }
        });
    }

    @HandleLoadPackage(targetPackage = XConst.PKG_KEYGUARD, summary = "緊急通報ボタン")
    @XMinSdkVersion(Build.VERSION_CODES.KITKAT)
    public static void preventEmergencyButtonMissTap(
            final String modulePath,
            final LoadPackageParam lpparam,
            final ModOtherUtilitiesSettingsGen settings) throws Throwable {
        if (!settings.preventEmergencyButtonMissTap) {
            return;
        }
        Class<?> clsEmergencyButton = XposedHelpers.findClass(
                "com.android.keyguard.EmergencyButton", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clsEmergencyButton, "onFinishInflate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Button button = (Button) param.thisObject;
                        XposedHelpers.setAdditionalInstanceField(button, "size",
                                button.getTextSize());
                    }
                });
        XposedHelpers.findAndHookMethod(clsEmergencyButton, "takeEmergencyCallAction",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Button button = (Button) param.thisObject;
                        Object flag = XposedHelpers.getAdditionalInstanceField(button, "flag");
                        if (flag == null) {
                            XposedHelpers.setAdditionalInstanceField(button, "flag", true);
                            button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                    button.getTextSize() * 1.25f);
                            param.setResult(null);
                        } else {
                            XposedHelpers.removeAdditionalInstanceField(button, "flag");
                            button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                    (Float) XposedHelpers
                                            .getAdditionalInstanceField(button, "size"));
                        }
                    }
                });
        XposedHelpers.findAndHookMethod(clsEmergencyButton, "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.removeAdditionalInstanceField(param.thisObject, "flag");
                        Button button = (Button) param.thisObject;
                        button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                (Float) XposedHelpers
                                        .getAdditionalInstanceField(button, "size"));
                    }
                });
    }
}
