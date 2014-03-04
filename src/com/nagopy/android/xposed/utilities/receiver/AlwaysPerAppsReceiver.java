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

package com.nagopy.android.xposed.utilities.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nagopy.android.common.helper.Preferences;
import com.nagopy.android.xposed.utilities.setting.AlwaysUsePerAppsList;
import com.nagopy.android.xposed.utilities.setting.AlwaysUsePerAppsList.PerAppsSetting;
import com.nagopy.android.xposed.utilities.util.Const;
import com.nagopy.android.xposed.utilities.util.Logger;

public class AlwaysPerAppsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        log("receive");
        log(intent);

        String launchedFrom = intent.getStringExtra(Const.EXTRA_LAUNCHED_FROM_PKG);
        String targetPackageName = intent.getStringExtra(Const.EXTRA_TARGET_PACKAGE_NAME);
        String targetActivityName = intent.getStringExtra(Const.EXTRA_TARGET_ACTIVITY_NAME);
        String targetAction = intent.getStringExtra(Const.EXTRA_TARGET_ACTION);
        PerAppsSetting alwaysUsePerApps = new PerAppsSetting();
        alwaysUsePerApps.launchedFromPackageName = launchedFrom;
        alwaysUsePerApps.targetPackageName = targetPackageName;
        alwaysUsePerApps.targetActivityName = targetActivityName;
        alwaysUsePerApps.targetAction = targetAction;
        log(alwaysUsePerApps);

        Preferences preferences = new Preferences(context);
        AlwaysUsePerAppsList alwaysUsePerAppsList = preferences.getObject("always_use_per_apps",
                AlwaysUsePerAppsList.class);
        alwaysUsePerAppsList.list.add(alwaysUsePerApps);
        log(alwaysUsePerAppsList);
        preferences.putObject("always_use_per_apps", alwaysUsePerAppsList);
        preferences.apply();
    }

    /**
     * ログ出力を行う.
     * 
     * @param obj
     */
    private static void log(Object obj) {
        Logger.d(AlwaysPerAppsReceiver.class.getSimpleName(), obj);
    }

}
