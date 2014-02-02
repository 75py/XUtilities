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

import java.lang.reflect.Field;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.util.XUtil;
import com.nagopy.android.xposed.utilities.setting.ModToastSettingsGen;
import com.nagopy.android.xposed.utilities.util.Const;

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
        if (!mToastSettings.masterModToastDaoEnable || !mToastSettings.setToastAboveLockscreen) {
            XLog.d(getClass().getSimpleName() + " do nothing.");
            return;
        }

        // Toast優先度アップ
        XposedBridge.hookAllConstructors(Toast.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // トーストの表示レイヤーを変更
                ModToast.updateToastType(param.thisObject, mToastSettings.setToastAboveLockscreen);
            }
        });

        // showメソッドを書き換え
        XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject,
                        "mContext");
                String packageName = mContext.getPackageName();
                Toast toast = (Toast) param.thisObject;

                if (StringUtils.equals(packageName, Const.PACKAGE_NAME)) {
                    // このモジュールのパッケージ名と一致する場合はオリジナルのメソッドを実行
                    // トーストを表示
                    log("invokeOriginalMethod, called by " + packageName);
                    ModToast.updateToastType(param.thisObject, true);
                    return XUtil.invokeOriginalMethod(param);
                }

                // パッケージ名がXUtilities以外の場合

                // パーミッションを持たないアプリの場合があるため、いったんトーストの情報を抜き取り、XUtilitiesの
                // レシーバーに投げる。こうすることで、パーミッションがないアプリのトーストも
                // TYPE_SYSTEM_OVERLAYで表示できる
                Class<?> idCls = XposedHelpers.findClass("com.android.internal.R$id", null);
                Field messageId = XposedHelpers.findField(idCls, "message");
                int id = messageId.getInt(null);
                View findViewById = toast.getView().findViewById(id);

                if (findViewById == null) {
                    // ビューが見つからない場合
                    // たぶん、カスタムビューを使用している
                    log("not found TextView(Custom Toast?). invokeOriginalMethod, called by "
                            + packageName);

                    // レイヤーをTYPE_TOASTに戻してオリジナルのメソッドを実行
                    ModToast.updateToastType(param.thisObject, false);
                    return XUtil.invokeOriginalMethod(param);
                }

                // トーストのTextViewが取得できた場合

                // ブロードキャスト用のIntentを作成
                log("make ACTION_SHOW_TOAST Intent");
                TextView tv = (TextView) findViewById;

                // 各パラメータをintentにセット
                Intent intent = new Intent(Const.ACTION_SHOW_TOAST);
                intent.putExtra(Const.EXTRA_TOAST_MESSAGE, tv.getText());
                intent.putExtra(Const.EXTRA_TOAST_DURATION, toast.getDuration());
                intent.putExtra(Const.EXTRA_TOAST_GRAVITY, toast.getGravity());
                intent.putExtra(Const.EXTRA_TOAST_HORIZONTAL_MARGIN,
                        toast.getHorizontalMargin());
                intent.putExtra(Const.EXTRA_TOAST_VERTICAL_MARGIN, toast.getVerticalMargin());
                intent.putExtra(Const.EXTRA_TOAST_X_OFFSET, toast.getXOffset());
                intent.putExtra(Const.EXTRA_TOAST_Y_OFFSET, toast.getYOffset());
                intent.putExtra(Const.EXTRA_TOAST_ORIGINAL_PACKAGE_NAME, packageName);

                // broadcast送信
                log("sendBroadcast");
                mContext.sendBroadcast(intent);

                return null;
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

        log(getClass().getSimpleName() + " mission complete!");
    }

    /**
     * トーストの表示レイヤーを変更する.
     * 
     * @param toast {@link Toast}
     * @param enableOverlay トーストをオーバーレイ表示する場合はtrue、通常表示の場合はfalse
     */
    public static void updateToastType(Object toast, boolean enableOverlay) {
        // Toastの中のフィールド「mTN」を取得
        Object mTN = XposedHelpers.getObjectField(toast, "mTN");
        // mParamsを取得
        WindowManager.LayoutParams mParams = (LayoutParams) XposedHelpers
                .getObjectField(mTN, "mParams");
        // 引数によってレイヤーを変更
        mParams.type = enableOverlay ? WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                : WindowManager.LayoutParams.TYPE_TOAST;
        // mParamsを更新
        XposedHelpers.setObjectField(mTN, "mParams", mParams);
    }
}
