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

package com.nagopy.android.xposed.utilities.setting;

import java.util.Set;

import com.nagopy.android.xposed.annotation.XObject;
import com.nagopy.android.xposed.annotation.XSettings;
import com.nagopy.android.xposed.annotation.XSettingsHint;
import com.nagopy.android.xposed.utilities.util.Const;

@XSettings(modulePackageName = Const.PACKAGE_NAME)
class ModAppPickerSettings {

    /** 「Use by default for this action.」を表示するか */
    public Boolean showAlwaysUse;

    /** アプリ選択で表示させないアプリのパッケージ名リスト */
    public Set<String> appPickerBlackList;

    /** 「常時」をアプリごとに設定する */
    public Boolean settingAlwaysPerApps;

    @XObject
    public AlwaysUsePerAppsList alwaysUsePerApps;

    /** 「常に」をリソースからとっとく */
    @XSettingsHint(ignore = true)
    public CharSequence alwaysUse;

}
