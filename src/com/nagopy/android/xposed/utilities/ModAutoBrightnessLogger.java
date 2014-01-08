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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XConst;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.receiver.AutoBrightnessController;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 自動輝度調整のログを取得するためデバッグ用モジュール.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ModAutoBrightnessLogger extends AbstractXposedModule implements
        IXposedHookZygoteInit, IXposedHookLoadPackage,
        IXposedHookInitPackageResources {

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!BuildConfig.DEBUG) {
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

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!BuildConfig.DEBUG) {
            return;
        }
    }

    @Override
    public void handleInitPackageResources(
            final InitPackageResourcesParam resparam) throws Throwable {
        if (!BuildConfig.DEBUG) {
            return;
        }

        if (!XUtil.isSystemUi(resparam)) {
            // システムUI以外では何もしない
            return;
        }

        log("handleInitPackageResources");

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
                        luxTextView.setText("init");
                        parent.setGravity(Gravity.CENTER_VERTICAL);
                        parent.addView(luxTextView, 0);
                        AutoBrightnessController autoBrightnessChangedReceiver = new AutoBrightnessController(
                                luxTextView);
                        parent.getContext()
                                .registerReceiver(
                                        autoBrightnessChangedReceiver,
                                        new IntentFilter(
                                                AutoBrightnessController.ACTION_AUTO_BRIGHTNESS_CHANGED));
                    }
                });

        log("handleInitPackageResources finish");
    }

}
