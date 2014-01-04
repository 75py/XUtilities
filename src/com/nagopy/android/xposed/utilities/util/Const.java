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

package com.nagopy.android.xposed.utilities.util;

import android.widget.Toast;

/**
 * 定数クラス.
 */
public class Const {

    /** コンストラクタ */
    private Const() {
    }

    /** ロックスクリーンの時計設定変更アクション */
    public static final String ACTION_LOCKSCREEN_CLOCK_SETTING_CHANGED = "com.nagopy.android.xposed.utilities.ACTION_LOCKSCREEN_CLOCK_SETTING_CHANGED";
    /** ステータスバーの時計設定変更アクション */
    public static final String ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED = "com.nagopy.android.xposed.utilities.ACTION_STATUS_BAR_CLOCK_SETTING_CHANGED";

    /** モジュールのパッケージ名 */
    public static final String PACKAGE_NAME = "com.nagopy.android.xposed.utilities";

    /** トースト表示アクション */
    public static final String ACTION_SHOW_TOAST = "com.nagopy.android.xposed.utilities.ACTION_SHOW_TOAST";
    /** {@link Toast}のメッセージ本文 */
    public static final String EXTRA_TOAST_MESSAGE = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_MESSAGE";
    /** {@link Toast#getDuration()} */
    public static final String EXTRA_TOAST_DURATION = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_DURATION";
    /** {@link Toast#getGravity()} */
    public static final String EXTRA_TOAST_GRAVITY = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_GRAVITY";
    /** {@link Toast#getGravity()} */
    public static final String EXTRA_TOAST_HORIZONTAL_MARGIN = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_HORIZONTAL_MARGIN";
    /** {@link Toast#getGravity()} */
    public static final String EXTRA_TOAST_VERTICAL_MARGIN = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_VERTICAL_MARGIN";
    /** {@link Toast#getXOffset()} */
    public static final String EXTRA_TOAST_X_OFFSET = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_X_OFFSET";
    /** {@link Toast#getYOffset()} */
    public static final String EXTRA_TOAST_Y_OFFSET = "com.nagopy.android.xposed.utilities.ModToast.EXTRA_TOAST_Y_OFFSET";

}
