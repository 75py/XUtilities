
package com.nagopy.android.xposed.utilities.util;

import android.content.Context;

import com.google.tagmanager.DataLayer;
import com.google.tagmanager.TagManager;

/**
 * Google Analyticsのユーティリティクラス.
 */
public class AnalyticsUtil {

    private AnalyticsUtil() {
    }

    /**
     * Push an "openScreen" event with the given screen name. Tags that match
     * that event will fire.
     */
    public static void pushOpenScreenEvent(Context context, String screenName) {
        DataLayer dataLayer = TagManager.getInstance(context).getDataLayer();
        dataLayer.push(DataLayer.mapOf("screenName", screenName, "event", "openScreen"));
    }

    /**
     * Push a "closeScreen" event with the given screen name. Tags that match
     * that event will fire.
     */
    public static void pushCloseScreenEvent(Context context, String screenName) {
        DataLayer dataLayer = TagManager.getInstance(context).getDataLayer();
        dataLayer.push(DataLayer.mapOf("screenName", screenName, "event", "closeScreen"));
    }

    /**
     * 設定変更をpushする.
     * 
     * @param context {@link Context}
     * @param key キー
     * @param newValue 値
     */
    public static void pushSettingChengedEvent(Context context, String key, Object newValue) {
        DataLayer dataLayer = TagManager.getInstance(context).getDataLayer();
        dataLayer.push(DataLayer.mapOf("prefKey", key, "newValue", newValue));
    }

}
