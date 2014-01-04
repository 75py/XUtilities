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
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.LinearLayout;

import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModNotificationSettingsGen;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ModNotification extends AbstractXposedModule implements IXposedHookLoadPackage,
        IXposedHookInitPackageResources {

    @XResource
    private ModNotificationSettingsGen mNotificationSettings;

    @Override
    public void handleInitPackageResources(final InitPackageResourcesParam resparam)
            throws Throwable {
        if (!XUtil.isSystemUi(resparam)) {
            return;
        }

        if (!mNotificationSettings.masterModNotificationEnable) {
            log("handleInitPackageResources. do nothing.");
            return;
        }

        log("handleInitPackageResources start");

        if (mNotificationSettings.hideNotificationHeader) {
            // 通知領域のヘッダーを非表示にする
            resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout", "super_status_bar",
                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam)
                                throws Throwable {
                            // ヘッダーを非表示にする
                            LinearLayout header = (LinearLayout) liparam.view.findViewById(
                                    liparam.res.getIdentifier("header", "id", XConst.PKG_SYSTEM_UI));
                            header.setVisibility(View.GONE);
                        }
                    });
        }

        log("handleInitPackageResources end");
    }

    /**
     * デバッグログを出力する.
     * 
     * @param msg メッセージ
     */
    private static void log(Object msg) {
        XLog.d(ModNotification.class.getSimpleName(), msg);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!XUtil.isSystemUi(lpparam)) {
            return;
        }

        if (!mNotificationSettings.masterModNotificationEnable) {
            log("handleLoadPackage. do nothing.");
            return;
        }

        log("handleLoadPackage start");

        // アイコンだけを非表示にする
        Class<?> clsStatusBarIcon = XposedHelpers.findClass(
                "com.android.internal.statusbar.StatusBarIcon", lpparam.classLoader);
        XposedBridge.hookAllConstructors(clsStatusBarIcon, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String iconPackage = (String) XposedHelpers.getObjectField(param.thisObject,
                        "iconPackage");
                if (mNotificationSettings.hideNotificationIconPackages.contains(iconPackage)) {
                    XLog.d("StatusBarIcon", "target package:" + iconPackage);
                    XposedHelpers.setBooleanField(param.thisObject, "visible", false);
                }
            }
        });// addNotification
        Class<?> clsPhoneStatusBar = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clsPhoneStatusBar, "addNotification", IBinder.class,
                StatusBarNotification.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        StatusBarNotification stb = (StatusBarNotification) param.args[1];
                        String packageName = stb.getPackageName();
                        if (mNotificationSettings.hideAllNotificationPackages.contains(packageName)) {
                            XLog.d("PhoneStatusBar.addNotification", "target package:"
                                    + packageName);
                            // do nothing
                            return null;
                        } else {
                            return XUtil.invokeOriginalMethod(param);
                        }
                    }
                });
        log("handleLoadPackage end");
    }
}
