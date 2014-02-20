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

package com.nagopy.android.xposed.utilities.setting;

import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.Typeface;

import com.nagopy.android.xposed.annotation.XIntDefaultValue;
import com.nagopy.android.xposed.annotation.XSettings;
import com.nagopy.android.xposed.annotation.XSettingsHint;
import com.nagopy.android.xposed.annotation.XStringDefaultValue;
import com.nagopy.android.xposed.utilities.util.Const;

@XSettings(modulePackageName = Const.PACKAGE_NAME)
class ModNotificationExpandedClockSettings {
    @XSettingsHint(ignore = true)
    public XModuleResources moduleResources;

    /** モジュールを有効にするかどうか */
    public Boolean masterModNotificationExpandedClockEnable;

    /** 文字サイズ */
    @XIntDefaultValue(100)
    public int notificationExpandedClockTimeTextSize;

    /** 文字色 */
    @XIntDefaultValue(Color.WHITE)
    public int notificationExpandedClockTimeTextColor;

    /** フォーマット */
    @XStringDefaultValue("HH:mm:ss")
    public String notificationExpandedClockTimeFormat;

    /** 英語表記にするかどうか */
    public Boolean notificationExpandedClockTimeForceEnglish;

    // ----------------------------------------------
    // ----------------------------------------------
    // ----------------------------------------------
    /** フォントファミリー */
    @XStringDefaultValue("DEFAULT")
    public String notificationExpandedClockTimeTypefaceKbn;

    public String notificationExpandedClockTimeTypefaceName;

    @XIntDefaultValue(Typeface.NORMAL)
    public Integer notificationExpandedClockTimeTypefaceStyle;
    // ----------------------------------------------
    // ----------------------------------------------
    // ----------------------------------------------

    /** デフォルトの文字サイズ */
    @XSettingsHint(ignore = true)
    public float defaultTimeTextSize;

    /** デフォルトの文字色 */
    @XSettingsHint(ignore = true)
    public int defaultTimeTextColor;

    /** デフォルトのフォント */
    @XSettingsHint(ignore = true)
    public Typeface defaultTimeTypeface;

    /** 文字サイズ */
    @XIntDefaultValue(100)
    public int notificationExpandedClockDateTextSize;

    /** 文字色 */
    @XIntDefaultValue(Color.WHITE)
    public int notificationExpandedClockDateTextColor;

    /** フォーマット */
    @XStringDefaultValue("yyyy/MM/dd(E)")
    public String notificationExpandedClockDateFormat;

    /** 英語表記にするかどうか */
    public Boolean notificationExpandedClockDateForceEnglish;

    // ----------------------------------------------
    // ----------------------------------------------
    // ----------------------------------------------
    /** フォントファミリー */
    @XStringDefaultValue("DEFAULT")
    public String notificationExpandedClockDateTypefaceKbn;

    public String notificationExpandedClockDateTypefaceName;

    @XIntDefaultValue(Typeface.NORMAL)
    public Integer notificationExpandedClockDateTypefaceStyle;
    // ----------------------------------------------
    // ----------------------------------------------
    // ----------------------------------------------

    /** デフォルトの文字サイズ */
    @XSettingsHint(ignore = true)
    public float defaultDateTextSize;

    /** デフォルトの文字色 */
    @XSettingsHint(ignore = true)
    public int defaultDateTextColor;

    /** デフォルトのフォント */
    @XSettingsHint(ignore = true)
    public Typeface defaultDateTypeface;
}
