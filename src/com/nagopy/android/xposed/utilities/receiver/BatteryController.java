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

// このクラスは、Android 4.1.2内にある以下のソースコードをもとにしています。
// com.android.systemui.statusbar.policy.BatteryController
/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.xposed.utilities.R;

public class BatteryController extends BroadcastReceiver {
    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();
    private Resources mResources;

    public BatteryController(Context context) {
        this(context, context.getResources());
    }

    public BatteryController(Context context, Resources resources) {
        mContext = context;
        mResources = resources;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            final boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            final int icon = plugged ? R.drawable.stat_sys_battery_circle_charge
                    : R.drawable.stat_sys_battery_circle;
            int N = mIconViews.size();
            for (int i = 0; i < N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageDrawable(mResources.getDrawableForDensity(icon,
                        (int) DimenUtil.getDensity(mContext)));
                v.setImageLevel(level);
            }
            N = mLabelViews.size();
            for (int i = 0; i < N; i++) {
                TextView v = mLabelViews.get(i);
                v.setText(level + "%");
            }
        }
    }
}
