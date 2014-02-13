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
class ModStatusBarClockSettings {

    @XSettingsHint(ignore = true)
    public XModuleResources moduleResources;

    /** モジュールを有効にするかどうか */
    public Boolean masterModStatusBarEnable;

    /** 時計の表示位置 */
    @XStringDefaultValue(Const.SB_CLOCK_POSITION_DEFAULT)
    public String statusBarClockPosition;

    /** 文字サイズ */
    @XIntDefaultValue(100)
    public int statusBarClockTextSize;

    /** 文字色 */
    @XIntDefaultValue(Color.WHITE)
    public int statusBarClockTextColor;

    /** フォーマット */
    @XStringDefaultValue("MM/dd(E) HH:mm")
    public String statusBarClockFormat;

    /** Localeを英語にするか */
    public boolean statusBarClockForceEnglish;

    public boolean statusBarClockGravityBottom;

    public boolean statusBarClockGravityRight;

    // ----------------------------------------------
    // ----------------------------------------------
    // ----------------------------------------------
    /** フォントファミリー */
    @XStringDefaultValue("DEFAULT")
    public String statusBarClockTypefaceKbn;

    public String statusBarClockTypefaceName;

    @XIntDefaultValue(Typeface.NORMAL)
    public Integer statusBarClockTypefaceStyle;
    // ----------------------------------------------
    // ----------------------------------------------
    // ----------------------------------------------

    /** デフォルトの文字サイズ */
    @XSettingsHint(ignore = true)
    public float defaultStatusBarClockTextSize;

    /** デフォルトの文字色 */
    @XSettingsHint(ignore = true)
    public int defaultStatusBarClockTextColor;

    /** デフォルトの位置 */
    @XSettingsHint(ignore = true)
    public int defaultGravity;

    /** デフォルトのフォント */
    @XSettingsHint(ignore = true)
    public Typeface defaultTypeface;

}
