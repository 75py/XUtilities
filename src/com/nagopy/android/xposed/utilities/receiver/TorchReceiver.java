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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nagopy.android.xposed.utilities.service.TorchService;

public class TorchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // ライトを制御するサービスを開始
        Intent service = new Intent(context.getApplicationContext(), TorchService.class);
        service.setAction(intent.getAction());
        context.startService(service);
    }

}
