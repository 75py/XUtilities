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

import java.util.Set;

import com.nagopy.android.xposed.annotation.XSettings;
import com.nagopy.android.xposed.utilities.util.Const;

@XSettings(modulePackageName = Const.PACKAGE_NAME)
public class ModImmersiveFullScreenModeSettings {

    /** モジュールを有効にするかどうか */
    public Boolean masterModImmersiveFullScreenModeEnabled;

    /** 動作モード */
    public String immersiveMode;

    /** Immersive full screen modeを有効にするパッケージ名のセット */
    public Set<String> immersiveModePackages;

    /** ナビバーだけimmersiveにするパッケージ名 */
    public Set<String> immersiveNaviBarPackages;
}
