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

package com.nagopy.android.xposed.utilities.receiver;

import org.apache.commons.lang3.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;

import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.common.util.ImageUtil;
import com.nagopy.android.common.util.ViewUtil;
import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.utilities.util.Const;

/**
 * {@link Toast}の表示をこのアプリに移譲するためのレシーバー.
 * 
 * @author 75py
 */
public class ToastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!StringUtils.equals(intent.getAction(), Const.ACTION_SHOW_TOAST)) {
            return;
        }

        // intentからトースト情報を取得し、セットする
        String message = intent.getStringExtra(Const.EXTRA_TOAST_MESSAGE);
        int duration = intent.getIntExtra(Const.EXTRA_TOAST_DURATION, Toast.LENGTH_LONG);
        Toast toast = Toast.makeText(context, message, duration);
        toast.setGravity(intent.getIntExtra(Const.EXTRA_TOAST_GRAVITY, toast.getGravity()),
                intent.getIntExtra(Const.EXTRA_TOAST_X_OFFSET, toast.getXOffset()),
                intent.getIntExtra(Const.EXTRA_TOAST_Y_OFFSET, toast.getYOffset()));
        toast.setMargin(
                intent.getFloatExtra(Const.EXTRA_TOAST_HORIZONTAL_MARGIN,
                        toast.getHorizontalMargin()),
                intent.getFloatExtra(Const.EXTRA_TOAST_VERTICAL_MARGIN,
                        toast.getVerticalMargin()));

        // アプリアイコン表示
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean showAppIcon = sp.getBoolean("toast_show_app_icon", false);
        if (showAppIcon) {
            // 呼び出し元のパッケージ名を取得
            String originalPackageName = intent
                    .getStringExtra(Const.EXTRA_TOAST_ORIGINAL_PACKAGE_NAME);
            // アイコン取得
            Drawable icon = ImageUtil.getApplicationIcon(context, originalPackageName);
            if (icon != null) {
                // アイコンサイズをセット
                int iconSize = ImageUtil.getIconSize(context);
                icon.setBounds(0, 0, iconSize, iconSize);
                // アイコンをセット
                TextView messageTextView = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                ViewUtil.setCompoundDrawablesRelative(messageTextView, icon, null, null, null);
                // 余白設定
                messageTextView.setCompoundDrawablePadding(DimenUtil.getPixelFromDp(context, 8));
            }
        }

        // 表示
        XLog.d(getClass().getSimpleName(), "show()");
        toast.show();
    }

}
