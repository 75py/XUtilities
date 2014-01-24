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
import android.view.KeyEvent;

import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModOtherUtilitiesSettingsGen;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModOtherUtilities extends AbstractXposedModule implements IXposedHookZygoteInit,
        IXposedHookLoadPackage {

    @XResource
    private ModOtherUtilitiesSettingsGen mOtherUtilitiesSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!mOtherUtilitiesSettings.masterModOtherUtilitiesEnable) {
            XLog.d(getClass().getSimpleName(), "initZygote, do nothing.");
            return;
        }

        // IME通知表示有無
        XResources.setSystemWideReplacement("android", "bool", "show_ongoing_ime_switcher",
                mOtherUtilitiesSettings.showOngoingImeSwitcher);

        // ActionMenuのテキスト表示
        XResources.setSystemWideReplacement("android", "bool",
                "config_allowActionMenuItemTextWithIcon",
                mOtherUtilitiesSettings.configAllowActionMenuItemTextWithIcon);

        // カメラ シャッター音
        if (VersionUtil.isJBmr1OrLater()) {
            XResources.setSystemWideReplacement("android", "bool", "config_camera_sound_forced",
                    mOtherUtilitiesSettings.configCameraSoundForced);
        }

        // 音量ボタンでスリープ復帰
        if (VersionUtil.isJBmr1OrLater() && mOtherUtilitiesSettings.volumeRockerWake) {
            // 4.2以上、設定が有効の場合は処理を開始

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
                            XLog.d("PhoneWindowManager", "isWakeKeyWhenScreenOff:" + keyCode);
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

        XLog.d(getClass().getSimpleName() + " mission complete!");
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!XUtil.isSystemUi(lpparam)) {
            return;
        }

        if (!mOtherUtilitiesSettings.masterModOtherUtilitiesEnable) {
            XLog.d(getClass().getSimpleName(), "handleLoadPackage, do nothing.");
            return;
        }

        if (mOtherUtilitiesSettings.showMenuKey) {
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
    }
}