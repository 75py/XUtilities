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
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.XposedModules.HandleInitPackageResources;
import com.nagopy.android.xposed.utilities.XposedModules.HandleLoadPackage;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XposedModule;
import com.nagopy.android.xposed.utilities.setting.ModNotificationSettingsGen;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@XposedModule(setting = ModNotificationSettingsGen.class)
public class ModNotification {

    @HandleInitPackageResources(summary = "ヘッダーを非表示にする", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void hideNotificationHeader(
            final String modulePath,
            final InitPackageResourcesParam resparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        if (!mNotificationSettings.hideNotificationHeader) {
            return;
        }

        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout", "super_status_bar",
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // ヘッダー
                        LinearLayout header = (LinearLayout) liparam.view.findViewById(
                                liparam.res.getIdentifier("header", "id", XConst.PKG_SYSTEM_UI));
                        header.setVisibility(View.GONE);
                    }
                });
    }

    @HandleInitPackageResources(summary = "キャリアを非表示にする", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void hideNotificationExpandedCarrier(
            final String modulePath,
            final InitPackageResourcesParam resparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        if (!mNotificationSettings.hideNotificationExpandedCarrier) {
            return;
        }
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout", "super_status_bar",
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // キャリア表示
                        TextView carrierLabel = (TextView) liparam.view.findViewById(
                                liparam.res.getIdentifier("carrier_label", "id",
                                        XConst.PKG_SYSTEM_UI));
                        carrierLabel.setVisibility(View.GONE);
                        carrierLabel.setPadding(0, 0, 0, 1000);
                    }
                });
    }

    @HandleInitPackageResources(summary = "下寄せ・ヘッダー移動", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void notificationExpandedGravityBottom(
            final String modulePath,
            final InitPackageResourcesParam resparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        if (!mNotificationSettings.notificationExpandedGravityBottom) {
            return;
        }

        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout", "super_status_bar",
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // ヘッダー
                        LinearLayout header = (LinearLayout) liparam.view.findViewById(
                                liparam.res.getIdentifier("header", "id", XConst.PKG_SYSTEM_UI));
                        // キャリア表示
                        TextView carrierLabel = (TextView) liparam.view.findViewById(
                                liparam.res.getIdentifier("carrier_label", "id",
                                        XConst.PKG_SYSTEM_UI));

                        // キャリア表示部分にヘッダー分のマージンをセット
                        FrameLayout.LayoutParams carrierLabelParams = (FrameLayout.LayoutParams) carrierLabel
                                .getLayoutParams();
                        carrierLabelParams.gravity = Gravity.TOP;
                        carrierLabelParams.topMargin = carrierLabel
                                .getContext().getResources()
                                .getDimensionPixelSize(
                                        liparam.res.getIdentifier(
                                                "notification_panel_header_height",
                                                "dimen",
                                                XConst.PKG_SYSTEM_UI));

                        // うまいことやる
                        LinearLayout parentLayout = (LinearLayout) header.getParent();
                        FrameLayout.LayoutParams parentLayoutParams = (FrameLayout.LayoutParams) parentLayout
                                .getLayoutParams();
                        parentLayoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;

                        ScrollView scrollView = (ScrollView) liparam.view.findViewById(
                                liparam.res.getIdentifier("scroll", "id", XConst.PKG_SYSTEM_UI));
                        FrameLayout.LayoutParams svParams = (FrameLayout.LayoutParams) scrollView
                                .getLayoutParams();
                        svParams.gravity = Gravity.BOTTOM;

                        FrameLayout svParent = (FrameLayout) scrollView.getParent();
                        LinearLayout.LayoutParams svParentParams = (LinearLayout.LayoutParams) svParent
                                .getLayoutParams();
                        svParentParams.height = 0;
                        svParentParams.weight = 1;
                    }
                });
    }

    @HandleInitPackageResources(summary = "ヘッダーをフッターにする", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void notificationHeaderBottom(
            final String modulePath,
            final InitPackageResourcesParam resparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        if (!mNotificationSettings.notificationHeaderBottom) {
            return;
        }
        resparam.res.hookLayout(XConst.PKG_SYSTEM_UI, "layout", "super_status_bar",
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        // ヘッダー
                        LinearLayout header = (LinearLayout) liparam.view.findViewById(
                                liparam.res.getIdentifier("header", "id", XConst.PKG_SYSTEM_UI));
                        LinearLayout parentLayout = (LinearLayout) header.getParent();

                        LinearLayout.LayoutParams p = (LayoutParams) header.getLayoutParams();
                        parentLayout.removeView(header);
                        parentLayout.addView(header, p);
                    }
                });
    }

    @HandleLoadPackage(summary = "順番を逆にしてみる", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void reverseNotification(
            final String modulePath,
            final LoadPackageParam lpparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        Class<?> clsNotificationData = XposedHelpers.findClass(
                "com.android.systemui.statusbar.NotificationData", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clsNotificationData, "get", int.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param)
                            throws Throwable {
                        int i = (Integer) param.args[0];
                        int size = (Integer) XposedHelpers.callMethod(param.thisObject, "size");
                        return XposedBridge.invokeOriginalMethod(param.method,
                                param.thisObject,
                                new Object[] {
                                    size - i - 1
                                });
                    }
                });
    }

    @HandleLoadPackage(summary = "全通知非表示", targetPackage = XConst.PKG_SYSTEM_UI)
    @XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void hideAllNotification(
            final String modulePath,
            final LoadPackageParam lpparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        Class<?> clsPhoneStatusBar = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clsPhoneStatusBar, "addNotification", IBinder.class,
                StatusBarNotification.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        StatusBarNotification stb = (StatusBarNotification) param.args[1];
                        String packageName = stb.getPackageName();
                        if (mNotificationSettings.hideAllNotificationPackages.contains(packageName)) {
                            // do nothing
                            return null;
                        } else {
                            return XUtil.invokeOriginalMethod(param);
                        }
                    }
                });
    }

    @HandleLoadPackage(summary = "アイコンだけを非表示にする", targetPackage = XConst.PKG_SYSTEM_UI)
    public static void hideNotificationIcon(
            final String modulePath,
            final LoadPackageParam lpparam,
            final ModNotificationSettingsGen mNotificationSettings
            ) throws Throwable {
        Class<?> clsStatusBarIcon = XposedHelpers.findClass(
                "com.android.internal.statusbar.StatusBarIcon", lpparam.classLoader);
        XposedBridge.hookAllConstructors(clsStatusBarIcon, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String iconPackage = (String) XposedHelpers.getObjectField(param.thisObject,
                        "iconPackage");
                if (mNotificationSettings.hideNotificationIconPackages.contains(iconPackage)) {
                    XposedHelpers.setBooleanField(param.thisObject, "visible", false);
                }
            }
        });
    }
}
