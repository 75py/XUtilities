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

import org.apache.commons.lang3.StringUtils;

import android.content.res.XResources;

import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.utilities.setting.ModBrightnessSettingsGen;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ModBrightness extends AbstractXposedModule implements IXposedHookZygoteInit {

    @XResource
    private ModBrightnessSettingsGen mBrightnessSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!mBrightnessSettings.masterModBrightnessEnable) {
            XLog.d(getClass().getSimpleName() + " do nothing.");
            return;
        }

        // 最低輝度
        XResources.setSystemWideReplacement("android", "integer", "config_screenBrightnessDim",
                mBrightnessSettings.minBrightness);
        if (VersionUtil.isJBmr1OrLater()) {
            XResources.setSystemWideReplacement("android", "integer",
                    "config_screenBrightnessSettingMinimum", mBrightnessSettings.minBrightness);
        }

        // 自動輝度調整
        if (StringUtils.isNotEmpty(mBrightnessSettings.configAutoBrightnessLevels)
                && StringUtils
                        .isNotEmpty(mBrightnessSettings.configAutoBrightnessLcdBacklightValues)) {
            int[] autoBrightnessLevels = makeIntArray(mBrightnessSettings.configAutoBrightnessLevels);
            int[] autoBrightnessLcdBacklightValues = makeIntArray(mBrightnessSettings.configAutoBrightnessLcdBacklightValues);
            if ((autoBrightnessLevels.length + 1) != autoBrightnessLcdBacklightValues.length) {
                throw new IllegalArgumentException("パラメータ数エラー");
            }
            XResources.setSystemWideReplacement("android", "array",
                    "config_autoBrightnessLevels",
                    autoBrightnessLevels);
            XResources.setSystemWideReplacement("android", "array",
                    "config_autoBrightnessLcdBacklightValues",
                    autoBrightnessLcdBacklightValues);

            XResources.setSystemWideReplacement("android", "bool",
                    "config_automatic_brightness_available", true);

            final Class<?> displayPowerContoroll = XposedHelpers.findClass(
                    "com.android.server.power.DisplayPowerController", null);
            XposedHelpers.findAndHookMethod(displayPowerContoroll, "updateAutoBrightness",
                    boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object mScreenAutoBrightness = XposedHelpers.getObjectField(
                                    param.thisObject, "mScreenAutoBrightness");
                            Object mAmbientLux = XposedHelpers.getObjectField(
                                    param.thisObject, "mAmbientLux");
                            XLog.d("updateAutoBrightness", "lux:" + mAmbientLux + " / brightness:"
                                    + mScreenAutoBrightness);
                        }
                    });
        }

        XLog.d(getClass().getSimpleName() + " mission complete!");
    }

    /**
     * カンマ区切り数値の文字列をint配列に変換する.
     * 
     * @param value カンマ区切り数値
     * @return int配列
     */
    private int[] makeIntArray(String value) {
        String[] split = value.split(",");
        int length = split.length;
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            String trim = split[i].replace(" ", "");
            array[i] = Integer.parseInt(trim);
        }
        return array;
    }
}
