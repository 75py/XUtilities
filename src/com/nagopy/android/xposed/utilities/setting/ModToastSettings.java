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

import com.nagopy.android.xposed.annotation.XSettings;
import com.nagopy.android.xposed.utilities.util.Const;

@XSettings(modulePackageName = Const.PACKAGE_NAME)
class ModToastSettings {

    public Boolean masterModToastDaoEnable;

    public boolean setToastAboveLockscreen;

    /** アプリアイコンを表示するかどうか */
    public Boolean toastShowAppIcon;

    /**
     * 表示時間（長め）<br>
     * <b>この値に100をかけるとミリ秒になります。例えば、1秒に設定した場合は10って値が入る。</b><br>
     * TODO わかりづらいわｗ
     */
    public Integer toastLongDelay;

    /**
     * 表示時間（短め）<br>
     * <b>この値に100をかけるとミリ秒になります</b> TODO わかりづらいわｗ
     */
    public Integer toastShortDelay;

}
