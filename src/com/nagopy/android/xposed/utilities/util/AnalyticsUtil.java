
package com.nagopy.android.xposed.utilities.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import android.content.Context;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

/**
 * Google Analyticsのユーティリティクラス.
 */
public class AnalyticsUtil {

    private AnalyticsUtil() {
    }

    /**
     * 設定カテゴリをpushする.
     * 
     * @param context {@link Context}
     * @param value 送信文字列
     */
    public static void pushPreferenceCategory(Context context, String value) {
        Tracker tracker = EasyTracker.getInstance(context);
        MapBuilder builder = MapBuilder.createAppView().set(Fields.SCREEN_NAME, value);
        tracker.send(builder.build());
    }

    /**
     * 設定変更をpushする.
     * 
     * @param context {@link Context}
     * @param key キー
     * @param newValue 値
     */
    public static void pushSettingChengedEvent(Context context, String key, Object newValue) {
        Tracker tracker = EasyTracker.getInstance(context);
        if (newValue != null && newValue instanceof Set) {
            pushSettingChengedEvent(tracker, key, (Set<?>) newValue);
        } else {
            String newValueStr = newValue == null ? "nullpo" : String.valueOf(newValue);
            tracker.send(MapBuilder.createEvent("settingChenged", key, urlEncode(newValueStr),
                    null)
                    .build());
        }
    }

    /**
     * 設定変更を送信する.<br>
     * {@link Set}＝アプリ選択の場合。パッケージ名を送信しても仕方ないため、選択したアプリの数を送信する。
     * 
     * @param tracker
     * @param key
     * @param newValue
     */
    private static void pushSettingChengedEvent(Tracker tracker, String key, Set<?> newValue) {
        tracker.send(MapBuilder
                .createEvent("settingChengedSet", key, String.valueOf(newValue.size()), null)
                .build());
    }

    /**
     * URLエンコードして返す.
     * 
     * @param source 変換する文字列
     * @return UTF-8でエンコードした文字列
     */
    private static String urlEncode(String source) {
        try {
            return URLEncoder.encode(source.toString(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            return source;
        }
    }
}
