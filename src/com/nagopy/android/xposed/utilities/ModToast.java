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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.common.util.ImageUtil;
import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.common.util.ViewUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModToastSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * {@link Toast}をロックスクリーン上よりも上位レイヤーに表示させるモジュール.
 */
public class ModToast extends AbstractXposedModule implements IXposedHookZygoteInit {

    @XResource
    private ModToastSettingsGen mToastSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!mToastSettings.setToastAboveLockscreen) {
            return;
        }

        // レイヤーとかをごにょごびょ
        Class<?> clsTN = XposedHelpers.findClass("android.widget.Toast$TN", null);
        XposedBridge.hookAllConstructors(clsTN, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mToastSettings.setToastAboveLockscreen) {
                    // トーストのレイヤーを変更
                    WindowManager.LayoutParams mParams = (LayoutParams) XposedHelpers
                            .getObjectField(param.thisObject, "mParams");
                    mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

                    // タイトルをつける（パーミッションチェック回避用）
                    XposedHelpers.setAdditionalInstanceField(mParams, "flg", true);
                    mParams.setTitle("Toast");
                }
            }
        });

        // パーミッションチェック回避用のチェック（気休め程度）
        XposedHelpers.findAndHookMethod(WindowManager.LayoutParams.class, "setTitle",
                CharSequence.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object flg = XposedHelpers.getAdditionalInstanceField(param.thisObject,
                                "flg");
                        CharSequence title = (CharSequence) param.args[0];
                        if (flg == null && TextUtils.equals(title, "Toast")) {
                            // フラグがついていないけどsetTitle("Toast")ってしてる場合
                            // 無効化する
                            param.setResult(null);
                        }
                    }
                });

        // Toastのパーミッションチェックをごにょごにょ
        Class<?> clsPhoneWindowManager = XposedHelpers.findClass(
                "com.android.internal.policy.impl.PhoneWindowManager", null);
        final int OKAY = 0; // TODO WindowManagerGlobal#ADD_OKAY を取得した方が良い
        XC_MethodReplacement checkAddPermissionReplacement = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object originalReturns = XUtil.invokeOriginalMethod(param);

                WindowManager.LayoutParams layoutParams = (LayoutParams) param.args[0];
                CharSequence title = layoutParams.getTitle();
                if (title != null && title.equals("Toast")) {
                    // トーストの場合、無条件でおっけーにしとく
                    return OKAY;
                } else {
                    return originalReturns;
                }
            }
        };
        if (VersionUtil.isJBmr2OrLater()) {
            XposedHelpers.findAndHookMethod(clsPhoneWindowManager, "checkAddPermission",
                    WindowManager.LayoutParams.class, int[].class, checkAddPermissionReplacement);
        } else {
            XposedHelpers.findAndHookMethod(clsPhoneWindowManager, "checkAddPermission",
                    WindowManager.LayoutParams.class, checkAddPermissionReplacement);
        }

        // show前にViewをごにょごにょ
        XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View mNextView = (View) XposedHelpers.getObjectField(param.thisObject, "mNextView");
                Context context = mNextView.getContext();

                // LinearLayoutの場合
                if (mNextView instanceof LinearLayout) {
                    LinearLayout originalLL = (LinearLayout) mNextView;

                    if (originalLL.getOrientation() == LinearLayout.HORIZONTAL) {
                        log("is HORIZONTAL!");
                        // LinearLayout横配置の場合
                        // たぶん、カスタムレイアウト

                        // TODO どーする？何もしないでおｋ？
                    } else {
                        log("is not HORIZONTAL!");
                        // LinearLayout縦配置の場合
                        // たぶん、デフォルトのレイアウト
                        // ほんとにデフォルトか確認
                        if (originalLL.getChildCount() == 1) {
                            // 子View一個かを確認
                            View childAt = originalLL.getChildAt(0);
                            if (childAt.getId() == android.R.id.message
                                    && childAt instanceof TextView) {
                                // 子View一個、idがmessage、TextView
                                // ここまで厳密にやつ必要あるか疑問だが……

                                // アイコン取得
                                Drawable icon = ImageUtil.getApplicationIcon(context,
                                        context.getPackageName());
                                if (icon != null) {
                                    // アイコンサイズをセット
                                    int iconSize = ImageUtil.getIconSize(context);
                                    icon.setBounds(0, 0, iconSize, iconSize);
                                    // アイコンをセット
                                    TextView messageTextView = (TextView) childAt;
                                    ViewUtil.setCompoundDrawablesRelative(messageTextView, icon,
                                            null, null, null);
                                    // 余白設定
                                    messageTextView.setCompoundDrawablePadding(DimenUtil
                                            .getPixelFromDp(context, 4));
                                    // 文字を中央になるよう調整
                                    messageTextView.setGravity(Gravity.CENTER_VERTICAL);
                                }
                            } else {
                                // 子View一個のLinearLayoutだが、TextViewじゃないらしい
                                // TODO どーする？
                            }
                        } else {
                            // 子Viewが二個以上
                            // TODO 何かするべき？
                        }
                    }
                } else {
                    log("is not LinearLayout!");
                    // LinearLayout以外の場合
                    // まず間違いなくカスタムレイアウト

                    // TODO どうする？何もしない？
                }
            }
        });

        // 表示時間調整
        Class<?> clsNotificationManagerService = XposedHelpers.findClass(
                "com.android.server.NotificationManagerService",
                null);
        final int MESSAGE_TIMEOUT = XposedHelpers.getStaticIntField(clsNotificationManagerService,
                "MESSAGE_TIMEOUT");
        Class<?> clsToastRecord = XposedHelpers.findClass(
                "com.android.server.NotificationManagerService$ToastRecord", null);
        if (VersionUtil.isKitKatOrLator()) {
            XposedHelpers
                    .findAndHookMethod(clsNotificationManagerService,
                            "scheduleTimeoutLocked", clsToastRecord,
                            new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param)
                                        throws Throwable {
                                    Object r = param.args[0];
                                    Handler mHandler = (Handler) XposedHelpers.getObjectField(
                                            param.thisObject,
                                            "mHandler");
                                    Message m = Message.obtain(mHandler, MESSAGE_TIMEOUT, r);
                                    int duration = XposedHelpers.getIntField(r, "duration");
                                    long delay = duration == Toast.LENGTH_LONG ? mToastSettings.toastLongDelay * 100
                                            : mToastSettings.toastShortDelay * 100;
                                    mHandler.sendMessageDelayed(m, delay);
                                    return null;
                                }
                            });
        } else {
            XposedHelpers
                    .findAndHookMethod(clsNotificationManagerService,
                            "scheduleTimeoutLocked", clsToastRecord, boolean.class,
                            new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param)
                                        throws Throwable {
                                    Object r = param.args[0];
                                    boolean immediate = (Boolean) param.args[1];
                                    Handler mHandler = (Handler) XposedHelpers.getObjectField(
                                            param.thisObject,
                                            "mHandler");
                                    Message m = Message.obtain(mHandler, MESSAGE_TIMEOUT, r);
                                    int duration = XposedHelpers.getIntField(r, "duration");
                                    long delay = immediate ? 0
                                            : (duration == Toast.LENGTH_LONG ?
                                                    mToastSettings.toastLongDelay * 100
                                                    : mToastSettings.toastShortDelay * 100);
                                    mHandler.sendMessageDelayed(m, delay);
                                    return null;
                                }
                            });
        }
    }
}
