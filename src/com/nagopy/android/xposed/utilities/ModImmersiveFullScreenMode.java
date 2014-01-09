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
import android.app.Activity;
import android.os.Build;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.utilities.setting.ModImmersiveFullScreenModeSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * Immersive full screen modeモジュール.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ModImmersiveFullScreenMode extends AbstractXposedModule implements
        IXposedHookZygoteInit {

    @XResource
    private ModImmersiveFullScreenModeSettingsGen mSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!mSettings.masterModImmersiveFullScreenModeEnabled) {
            return;
        }

        XposedHelpers.findAndHookMethod(Activity.class, "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        String packageName = activity.getPackageName();
                        if (!mSettings.immersiveModePackages.contains(packageName)) {
                            return;
                        }

                        Window window = activity.getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        View decorView = window.getDecorView();
                        hideSystemUI(decorView);
                    }
                });
        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        String packageName = activity.getPackageName();
                        if (!mSettings.immersiveModePackages.contains(packageName)) {
                            return;
                        }

                        boolean hasFocus = (Boolean) param.args[0];
                        if (hasFocus) {
                            Window window = activity.getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            View decorView = window.getDecorView();
                            hideSystemUI(decorView);
                        }
                    }
                });

        // 初回のビューを表示しない
        XposedHelpers.findAndHookMethod(
                "com.android.internal.policy.impl.ImmersiveModeConfirmation", null,
                "immersiveModeChanged", String.class, boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        return null;
                    }
                });

        // IME非表示アクションをごにょごにょ
        XposedHelpers.findAndHookMethod(InputMethodManager.class, "hideSoftInputFromWindow",
                IBinder.class, int.class, ResultReceiver.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View mCurRootView = (View) XposedHelpers.getObjectField(param.thisObject,
                                "mCurRootView");
                        String packageName = mCurRootView.getContext()
                                .getPackageName();
                        if (mSettings.immersiveModePackages.contains(packageName)) {
                            // 非表示にする
                            hideSystemUI(mCurRootView);
                        }
                    }
                });
    }

    // This snippet hides the system bars.
    private void hideSystemUI(View view) {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

}
