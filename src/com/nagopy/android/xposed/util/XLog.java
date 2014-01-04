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

import android.util.Log;

public class XLog {

    private static final String TAG = "XUtilities";

    /**
     * デバッグログ出力を行う.
     * 
     * @param obj 出力値
     */
    public static void d(Object obj) {
        Log.d(TAG, obj == null ? "null" : obj.toString());
    }

    /**
     * デバッグログ出力を行う.
     * 
     * @param tag "75pyXposedMod_" + tag
     * @param obj 出力値
     */
    public static void d(String tag, Object obj) {
        Log.d(TAG + "_" + tag, obj == null ? "null" : obj.toString());
    }

    /**
     * ログ出力を行う.
     * 
     * @param obj 出力値
     */
    public static void i(Object obj) {
        Log.i(TAG, obj == null ? "null" : obj.toString());
    }

    /**
     * ログ出力を行う.
     * 
     * @param tag "75pyXposedMod_" + tag
     * @param obj 出力値
     */
    public static void i(String tag, Object obj) {
        Log.i(TAG + "_" + tag, obj == null ? "null" : obj.toString());
    }

    /**
     * ログ出力を行う.
     * 
     * @param obj 出力値
     */
    public static void e(Object obj) {
        Log.e(TAG, obj == null ? "null" : obj.toString());
    }

    /**
     * ログ出力を行う.
     * 
     * @param tag "75pyXposedMod_" + tag
     * @param obj 出力値
     */
    public static void e(String tag, Object obj) {
        Log.e(TAG + "_" + tag, obj == null ? "null" : obj.toString());
    }

}
