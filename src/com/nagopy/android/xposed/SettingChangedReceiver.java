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

package com.nagopy.android.xposed;

import java.lang.ref.WeakReference;

import org.apache.commons.lang3.StringUtils;

import com.nagopy.android.xposed.util.XLog;

import de.robv.android.xposed.XposedBridge;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * 設定変更をリアルタイムで反映するためのレシーバー.
 */
public abstract class SettingChangedReceiver extends BroadcastReceiver {

    /** 設定変更アクション */
    public final String action;

    /** 設定保存オブジェクト */
    public final WeakReference<Object> dataObject;

    protected SettingChangedReceiver(Object dataObject, String action) {
        this.dataObject = new WeakReference<Object>(dataObject);
        this.action = action;
    }

    /**
     * インテントを受け取り、データを更新した後に実行されるメソッド.
     */
    protected abstract void onDataChanged();

    @Override
    public void onReceive(Context context, Intent intent) {
        Object obj = dataObject.get();
        if (!StringUtils.equals(intent.getAction(), action) || obj == null) {
            context.unregisterReceiver(this);
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null || extras.size() != 2) {
            return;
        }

        try {
            // target、valueを取り出し、設定保存オブジェクトに反映する
            String target = extras.getString("target");
            Object value = extras.get("value");
            obj.getClass().getField(target).set(obj, value);

            onDataChanged();
        } catch (Throwable t) {
            XLog.e(getClass().getSimpleName(), t);
            XposedBridge.log(t);
        }
    }

    protected boolean isNotNull(Object... objects) {
        if (objects == null) {
            return false;
        }
        for (Object obj : objects) {
            if (obj == null) {
                return false;
            }
        }
        return true;
    }
}
