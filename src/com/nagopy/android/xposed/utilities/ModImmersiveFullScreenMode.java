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
import android.view.inputmethod.InputMethodManager;

import com.nagopy.android.xposed.utilities.XposedModules.InitZygote;
import com.nagopy.android.xposed.utilities.XposedModules.XposedModule;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.setting.ModImmersiveFullScreenModeSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * Immersive full screen modeモジュール.
 */
@XposedModule(setting = ModImmersiveFullScreenModeSettingsGen.class)
@XMinSdkVersion(Build.VERSION_CODES.KITKAT)
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ModImmersiveFullScreenMode {

    // 動作モードの値
    private static final String MODE_DISABLE = "0";
    private static final String MODE_NORMAL = "1";
    private static final String MODE_ONLY_NAVI_BAR = "2";
    @SuppressWarnings("unused")
    private static final String MODE_CUSTOM = "3";

    @InitZygote
    public static void initZygote(StartupParam startupParam,
            final ModImmersiveFullScreenModeSettingsGen mSettings) throws Throwable {
        if (mSettings.immersiveMode.equals(MODE_DISABLE)) {
            // 無効になっている場合は何もしない
            return;
        }

        XposedHelpers.findAndHookMethod(Activity.class, "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;

                        if (mSettings.immersiveMode.equals(MODE_NORMAL)) {
                            hideSystemUI(activity);
                        } else if (mSettings.immersiveMode.equals(MODE_ONLY_NAVI_BAR)) {
                            hideNaviBar(activity);
                        } else {
                            String packageName = activity.getPackageName();
                            if (mSettings.immersiveModePackages.contains(packageName)) {
                                hideSystemUI(activity);
                            } else if (mSettings.immersiveNaviBarPackages.contains(packageName)) {
                                hideNaviBar(activity);
                            }
                        }
                    }
                });
        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;

                        if (mSettings.immersiveMode.equals(MODE_NORMAL)) {
                            hideSystemUI(activity);
                        } else if (mSettings.immersiveMode.equals(MODE_ONLY_NAVI_BAR)) {
                            hideNaviBar(activity);
                        } else {
                            String packageName = activity.getPackageName();
                            if (mSettings.immersiveModePackages.contains(packageName)) {
                                boolean hasFocus = (Boolean) param.args[0];
                                if (hasFocus) {
                                    hideSystemUI(activity);
                                }
                            } else if (mSettings.immersiveNaviBarPackages.contains(packageName)) {
                                boolean hasFocus = (Boolean) param.args[0];
                                if (hasFocus) {
                                    hideNaviBar(activity);
                                }
                            }
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
                        if (mCurRootView == null) {
                            // nullの場合は何もしない
                            return;
                        }

                        // 非表示にする
                        if (mSettings.immersiveMode.equals(MODE_NORMAL)) {
                            hideSystemUI(mCurRootView);
                        } else if (mSettings.immersiveMode.equals(MODE_ONLY_NAVI_BAR)) {
                            hideNaviBar(mCurRootView);
                        } else {
                            String packageName = mCurRootView.getContext().getPackageName();
                            if (mSettings.immersiveModePackages.contains(packageName)) {
                                hideSystemUI(mCurRootView);
                            } else if (mSettings.immersiveNaviBarPackages.contains(packageName)) {
                                hideNaviBar(mCurRootView);
                            }
                        }
                    }
                });
        XposedHelpers.findAndHookMethod(InputMethodManager.class, "showStatusIcon", IBinder.class,
                String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View mCurRootView = (View) XposedHelpers.getObjectField(param.thisObject,
                                "mCurRootView");
                        if (mCurRootView == null) {
                            // nullの場合は何もしない
                            return;
                        }
                        showSystemUI(mCurRootView);
                    }
                });
    }

    private static void showSystemUI(View view) {
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private static void hideSystemUI(Activity activity) {
        View view = activity.getWindow().getDecorView();
        hideSystemUI(view);
    }

    private static void hideNaviBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        hideNaviBar(view);
    }

    // This snippet hides the system bars.
    private static void hideSystemUI(View view) {
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

    /** ナビバーだけimmersiveにする */
    private static void hideNaviBar(View view) {
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

}
