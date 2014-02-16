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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import com.nagopy.android.common.util.VersionUtil;
import com.nagopy.android.xposed.util.XLog;
import de.robv.android.xposed.XposedHelpers;

/**
 * Xposedモジュールのベースクラス.<br>
 * とりあえず{@link XResource}をつけたフィールドにデフォルトコンストラクタで生成したインスタンスをセットする。
 */
public abstract class AbstractXposedModule {

    {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            XResource xResource = field.getAnnotation(XResource.class);
            if (xResource != null) {
                XLog.d("xResource:" + field);
                try {
                    Class<?> cls = field.getType();
                    Object instance = cls.newInstance();
                    field.setAccessible(true);
                    field.set(this, instance);
                } catch (Exception e) {
                    XLog.e("xResource error:" + field + "," + e);
                }
            }
        }
    }

    /**
     * デフォルトコンストラクタで自動生成を行うマーカー.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XResource {
    }

    protected static int findId(String packageName, String idName, ClassLoader classLoader)
            throws IllegalAccessException, IllegalArgumentException {
        Class<?> idCls = XposedHelpers.findClass(packageName + ".R$id", classLoader);
        Field messageId = XposedHelpers.findField(idCls, idName);
        int id = messageId.getInt(null);
        return id;
    }

    /**
     * ログ出力を行う.
     * 
     * @param obj
     */
    protected void log(Object obj) {
        XLog.d(this.getClass().getSimpleName(), obj);
    }

    protected Object getObjectField(Object obj, String fieldName) {
        return XposedHelpers.getObjectField(obj, fieldName);
    }

    /**
     * @param context
     * @param intent
     */
    @SuppressLint("NewApi")
    protected void sendBroadcast(Context context, Intent intent) {
        if (VersionUtil.isJBmr1OrLater()) {
            UserHandle userAll = (UserHandle) XposedHelpers.getStaticObjectField(
                    UserHandle.class, "ALL");
            context.sendBroadcastAsUser(intent, userAll);
        } else {
            context.sendBroadcast(intent);
        }
    }

}
