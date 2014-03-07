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
import android.text.TextUtils;
import android.widget.TextView;

import com.nagopy.android.xposed.utilities.util.Logger;

public class AutoBrightnessController extends BroadcastReceiver {

    public static final String ACTION_AUTO_BRIGHTNESS_CHANGED = "com.nagopy.android.xposed.utilities.ACTION_AUTO_BRIGHTNESS_CHANGED";
    public static final String EXTRA_BRIGHTNESS = "com.nagopy.android.xposed.utilities.EXTRA_BRIGHTNESS";
    public static final String EXTRA_LUX = "com.nagopy.android.xposed.utilities.EXTRA_LUX";

    private final TextView mStateTextView;

    private String mTextFormat = "l:%.1f\nb:%d";

    public AutoBrightnessController(TextView textView) {
        mStateTextView = textView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.equals(action, Intent.ACTION_SCREEN_OFF)) {
            // スクリーンオフの場合、いったんテキストを消す
            mStateTextView.setText("");
        } else if (TextUtils.equals(action, ACTION_AUTO_BRIGHTNESS_CHANGED)) {
            // Extraを取得
            int brightness = intent.getIntExtra(EXTRA_BRIGHTNESS, -1);
            float lux = intent.getFloatExtra(EXTRA_LUX, -1);

            // extraの存在チェック
            if (!intent.hasExtra(EXTRA_BRIGHTNESS) || !intent.hasExtra(EXTRA_LUX)) {
                Logger.e(this.getClass().getSimpleName(), "invalid " + "lux:" + lux
                        + " brightness:" + brightness);
                return;
            }

            // 表示を更新
            mStateTextView.setText(String.format(mTextFormat, lux, brightness));
        }
    }
}
