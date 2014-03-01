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

import de.robv.android.xposed.XposedBridge;
import android.util.Log;

public class XLog {

    private static final String TAG = "XUtilities";

    private static final String LOG_FORMAT_DEFAULT = "%s ::: %s";
    private static final String LOG_FORMAT_MODULE = "%s_%s ::: %s";

    public static void e(Throwable t) {
        XposedBridge.log(String.format(LOG_FORMAT_DEFAULT, TAG, Log.getStackTraceString(t)));
    }

    public static void e(String tag, Throwable t) {
        XposedBridge.log(String.format(LOG_FORMAT_MODULE, TAG, tag, Log.getStackTraceString(t)));
    }

    /**
     * デバッグログ出力を行う.
     * 
     * @param obj 出力値
     */
    public static void d(Object obj) {
        XposedBridge.log(String.format(LOG_FORMAT_DEFAULT, TAG,
                obj == null ? "null" : obj.toString()));
    }

    /**
     * デバッグログ出力を行う.
     * 
     * @param tag "XUtilities_" + tag
     * @param obj 出力値
     */
    public static void d(String tag, Object obj) {
        XposedBridge.log(String.format(LOG_FORMAT_MODULE, TAG, tag,
                obj == null ? "null" : obj.toString()));
    }

}
