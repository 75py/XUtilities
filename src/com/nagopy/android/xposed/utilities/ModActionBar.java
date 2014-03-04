/*
 * Copyright (C) 2014 75py
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

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

import com.nagopy.android.xposed.utilities.XposedModules.InitZygote;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XposedModule;
import com.nagopy.android.xposed.utilities.setting.ModActionBarSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * アクションバーを下に移動するモジュール.
 */
@XposedModule(setting = ModActionBarSettingsGen.class)
@XMinSdkVersion(Build.VERSION_CODES.KITKAT)
public class ModActionBar {

    @InitZygote
    public static void initZygote(StartupParam startupParam,
            final ModActionBarSettingsGen mSettings) throws Throwable {
        if (mSettings.actionBarBottomEnable) {
            // for KitKat
            // アクションバーを下に表示
            Class<?> clsActionBarOverlayLayout = XposedHelpers.findClass(
                    "com.android.internal.widget.ActionBarOverlayLayout", null);
            XposedHelpers.findAndHookMethod(clsActionBarOverlayLayout, "onLayout", boolean.class,
                    int.class, int.class, int.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) XposedHelpers.callMethod(param.thisObject,
                                    "getContext");
                            String packageName = context.getPackageName();

                            if (!mSettings.actionBarBottomPackages.contains(packageName)) {
                                // 対象に含まれない場合、何もしない
                                return;
                            }

                            // ActionBarのViewを取得
                            int action_bar_container = context.getResources().getIdentifier(
                                    "action_bar_container", "id", "android");
                            View actionBar = (View) XposedHelpers.callMethod(param.thisObject,
                                    "findViewById", action_bar_container);
                            // アクションバーの高さをとっておく
                            final int actionBarHeight = actionBar.getMeasuredHeight();

                            // 下のアクションバーを取っとく
                            int split_action_bar = context.getResources().getIdentifier(
                                    "split_action_bar", "id", "android");

                            // パラメータの値を取得
                            int left = (Integer) param.args[1];
                            int top = (Integer) param.args[2];
                            int right = (Integer) param.args[3];
                            int bottom = (Integer) param.args[4];

                            final int parentLeft = (Integer) XposedHelpers.callMethod(
                                    param.thisObject,
                                    "getPaddingLeft");
                            @SuppressWarnings("unused")
                            final int parentRight = right - left
                                    - (Integer) XposedHelpers.callMethod(param.thisObject,
                                            "getPaddingRight");

                            final int parentTop = (Integer) XposedHelpers.callMethod(
                                    param.thisObject,
                                    "getPaddingTop");
                            final int parentBottom = bottom - top
                                    - (Integer) XposedHelpers.callMethod(param.thisObject,
                                            "getPaddingBottom");

                            int count = (Integer) XposedHelpers.callMethod(param.thisObject,
                                    "getChildCount");
                            for (int i = 0; i < count; i++) {
                                final View child = (View) XposedHelpers.callMethod(
                                        param.thisObject,
                                        "getChildAt", i);
                                if (child.getVisibility() != View.GONE) {
                                    final MarginLayoutParams marginLayoutParams = (MarginLayoutParams) child
                                            .getLayoutParams();

                                    final int w = child.getMeasuredWidth();
                                    final int h = child.getMeasuredHeight();

                                    int childLeft = parentLeft + marginLayoutParams.leftMargin;
                                    int childTop;
                                    if (child.getId() == split_action_bar) {
                                        // もともと下のアクションバーの場合
                                        childTop = parentBottom - h
                                                - marginLayoutParams.bottomMargin;
                                        childTop -= actionBarHeight;
                                    } else if (child.getId() == action_bar_container) {
                                        // アクションバーの場合
                                        childTop = parentBottom - h
                                                - marginLayoutParams.bottomMargin;
                                    } else if (child.getId() == android.R.id.content) {
                                        // メインのViewの場合
                                        childTop = parentTop + marginLayoutParams.topMargin
                                                - actionBarHeight;
                                    } else {
                                        // それ以外
                                        childTop = parentTop + marginLayoutParams.topMargin;
                                    }

                                    child.layout(childLeft, childTop, childLeft + w, childTop
                                            + h);
                                }
                            }
                        }
                    });
        }
    }

}
