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
import android.os.Build;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.TextClock;

import com.nagopy.android.common.util.GestureUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.utilities.XposedModules.XMinSdkVersion;
import com.nagopy.android.xposed.utilities.XposedModules.XTargetPackage;
import com.nagopy.android.xposed.utilities.service.TorchService;
import com.nagopy.android.xposed.utilities.setting.ModLockscreenTorchSettingsGen;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * ロックスクリーンでライトを点灯させるためのモジュール.
 */
@XMinSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR1)
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ModLockscreenTorch extends AbstractXposedModule implements IXposedHookLoadPackage {

    @XResource
    private ModLockscreenTorchSettingsGen mLockscreenTorchSettings;

    /** キーガードのパッケージ名 */
    private static final String PACKAGE_KEYGUARD = "com.android.keyguard";

    @XTargetPackage(PACKAGE_KEYGUARD)
    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // KeyguardStatusViewのクラスを取得
        Class<?> clsKeyguardStatusView = XposedHelpers.findClass(
                "com.android.keyguard.KeyguardStatusView", lpparam.classLoader);
        // onFinishInflateで時計部分にリスナーを付ける
        XposedHelpers.findAndHookMethod(clsKeyguardStatusView, "onFinishInflate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextClock mDateView = (TextClock) XposedHelpers.getObjectField(
                                param.thisObject, "mDateView");
                        TextClock mClockView = (TextClock) XposedHelpers.getObjectField(
                                param.thisObject, "mClockView");
                        Context context = mClockView.getContext().getApplicationContext();

                        // ダブルタップ、ロングタップのリスナーを作成してセットする
                        ClockTapTorchListener clockTapTorchListener = new ClockTapTorchListener(
                                context, mLockscreenTorchSettings.lockscreenClockDoubleTapTorch,
                                mLockscreenTorchSettings.lockscreenClockLongTapTorchToggle);
                        OnTouchListener onTouchListener = GestureUtil.makeOnTouchListener(
                                context, clockTapTorchListener);
                        mDateView.setOnTouchListener(onTouchListener);
                        mClockView.setOnTouchListener(onTouchListener);
                    }
                });
    }

    private static class ClockTapTorchListener extends SimpleOnGestureListener {
        private final Context mContext;
        /** ダブルタップでライトを点灯するかどうか */
        private final Boolean useDoubleTap;
        /** ロングタップでライトを点灯するかどうか */
        private final Boolean useLongTap;

        /**
         * コンストラクタ
         * 
         * @param context　コンテキスト
         * @param useDoubleTap ダブルタップでライトを点灯するかどうか
         * @param useLongTap ロングタップでライトを点灯するかどうか
         */
        public ClockTapTorchListener(Context context, Boolean useDoubleTap, Boolean useLongTap) {
            mContext = context;
            this.useDoubleTap = useDoubleTap;
            this.useLongTap = useLongTap;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            XLog.d("ClockTapTorchListener", "onDoubleTap:" + useDoubleTap);
            if (useDoubleTap) {
                // ダブルタップ点灯が有効の場合はトグルのブロードキャストを送信
                mContext.sendBroadcast(new Intent(TorchService.ACTION_TORCH_TOGGLE));
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            XLog.d("ClockTapTorchListener", "onLongPress:" + useLongTap);
            if (useLongTap) {
                // ロングタップ点灯が有効の場合はトグルのブロードキャストを送信
                mContext.sendBroadcast(new Intent(TorchService.ACTION_TORCH_TOGGLE));
            }
        }

    };
}
