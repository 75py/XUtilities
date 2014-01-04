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

package com.nagopy.android.xposed.util;

import org.apache.commons.lang3.StringUtils;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

public class XUtil {

    private XUtil() {
    }

    /**
     * オリジナルのメソッドを実行する.
     * 
     * @param param {@link MethodHookParam}
     * @return オリジナルのメソッドの戻り値
     */
    public static Object invokeOriginalMethod(MethodHookParam param) throws Exception {
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }

    /**
     * システムUIとパッケージ名を比較する.
     * 
     * @param packageName パッケージ名
     * @return システムUIならtrue、それ以外ならfalseを返す。
     */
    public static Boolean isSystemUi(String packageName) {
        return StringUtils.equals(packageName, XConst.PKG_SYSTEM_UI);
    }

    public static Boolean isSystemUi(final InitPackageResourcesParam resparam) {
        return isSystemUi(resparam.packageName);
    }

    public static Boolean isSystemUi(final LoadPackageParam lpparam) {
        return isSystemUi(lpparam.packageName);
    }

}
