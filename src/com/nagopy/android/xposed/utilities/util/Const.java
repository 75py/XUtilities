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

    /** 初回起動のフラグ用キー */
    public static final String KEY_FIRST_FLAG = "KEY_FIRST_FLAG";

    public static final String KEY_GA_OPTOUT = "KEY_GA_OPTOUT";

    /** ステータスバー時計の表示位置（左） */
    public static final String SB_CLOCK_POSITION_LEFT = "left";
    /** ステータスバー時計の表示位置（中央） */
    public static final String SB_CLOCK_POSITION_CENTER = "center";
    /** ステータスバー時計の表示位置（デフォルト） */
    public static final String SB_CLOCK_POSITION_DEFAULT = "default";

    public static final String ACTION_ALWAYS_USE_PER_APPS = "com.nagopy.android.xposed.utilities.ACTION_ALWAYS_USE_PER_APPS";
    public static final String EXTRA_LAUNCHED_FROM_PKG = "com.nagopy.android.xposed.utilities.EXTRA_LAUNCHED_FROM_PKG";
    public static final String EXTRA_TARGET_PACKAGE_NAME = "com.nagopy.android.xposed.utilities.EXTRA_TARGET_PACKAGE_NAME";
    public static final String EXTRA_TARGET_ACTIVITY_NAME = "com.nagopy.android.xposed.utilities.EXTRA_TARGET_ACTIVITY_NAME";
    public static final String EXTRA_TARGET_ACTION = "com.nagopy.android.xposed.utilities.EXTRA_TARGET_ACTION";
    public static final String ACTION_APP_PICKER_SETTING_CHANGED = "com.nagopy.android.xposed.utilities.ACTION_APP_PICKER_SETTING_CHANGED";
    public static final String ACTION_NOTIFICATION_EXPANDED_CLOCK_SETTING_CHANGED = "com.nagopy.android.xposed.utilities.ACTION_NOTIFICATION_EXPANDED_CLOCK_SETTING_CHANGED";

    public static final String ADDITIONAL_DATE_FORMAT = "ADDITIONAL_DATE_FORMAT";

}
