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

// このクラスの実装にあたり、GravityBoxのソースコードの一部を参考にしています。
/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.nagopy.android.xposed.utilities.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;

import com.nagopy.android.common.R;
import com.nagopy.android.common.helper.TorchHelper;

/**
 * ライト点灯を行うサービス.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class TorchService extends Service {

    /** ライトのオン・オフを切り替え */
    public static final String ACTION_TORCH_TOGGLE = "com.nagopy.android.xposed.utilities.service.TorchService.ACTION_TORCH_TOGGLE";

    /** ライトを点灯する */
    public static final String ACTION_TORCH_ON = "com.nagopy.android.xposed.utilities.service.TorchService.ACTION_TORCH_ON";

    /** ライトを消灯する */
    public static final String ACTION_TORCH_OFF = "com.nagopy.android.xposed.utilities.service.TorchService.ACTION_TORCH_OFF";

    /** ライトの状態が変化したときに送信するブロードキャストのアクション */
    public static final String ACTION_TORCH_STATE_CHANGED = "com.nagopy.android.xposed.utilities.service.TorchService.ACTION_TORCH_STATE_CHANGED";
    public static final String EXTRA_TORCH_IS_ON = "com.nagopy.android.xposed.utilities.service.TorchService.EXTRA_TORCH_IS_ON";

    /** ライト */
    private TorchHelper mTorch;

    /** 点灯中の通知 */
    private Notification mNotification;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTorch = TorchHelper.getInstance();

        // 点灯中の通知アイコン等を作成
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.torch_service_notification_content_title));
        builder.setContentText(getString(R.string.torch_service_notification_content_text));
        builder.setSmallIcon(R.drawable.ic_flashlight);
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.ic_flashlight);
        builder.setLargeIcon(b);
        // 通知をタップされたときは、ブロードキャストを送信して止める
        Intent intent = new Intent(ACTION_TORCH_OFF);
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        builder.setContentIntent(mPendingIntent);
        mNotification = builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TORCH_TOGGLE.equals(intent.getAction())) {
            mTorch.toggle();
        } else if (intent != null && ACTION_TORCH_ON.equals(intent.getAction())) {
            mTorch.on();
        } else if (intent != null && ACTION_TORCH_OFF.equals(intent.getAction())) {
            mTorch.off();
        }

        Boolean isON = mTorch.isON();

        // 状態変更のブロードキャスト送信
        Intent broadcast = new Intent(ACTION_TORCH_STATE_CHANGED);
        broadcast.putExtra(EXTRA_TORCH_IS_ON, isON);
        sendBroadcast(broadcast);

        if (isON) {
            // 通知アイコンを表示し、フォアグラウンドに
            startForeground(R.drawable.ic_flashlight, mNotification);
            // TODO タイマーで消灯する処理
            return START_REDELIVER_INTENT;
        } else {
            // サービスを終了させる
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTorch.off();
    }
}
