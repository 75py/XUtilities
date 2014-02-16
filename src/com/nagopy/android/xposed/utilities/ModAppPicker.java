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

package com.nagopy.android.xposed.utilities;

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.xposed.AbstractXposedModule;
import com.nagopy.android.xposed.utilities.setting.AlwaysUsePerAppsList.PerAppsSetting;
import com.nagopy.android.xposed.utilities.setting.ModAppPickerSettingsGen;
import com.nagopy.android.xposed.utilities.util.Const;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

/**
 * アプリ選択をカスタマイズするモジュール.<br>
 * 常時のチェックボックス部分はAlternateAppPickerをほぼコピー(Apache License v2.0)。
 */
public class ModAppPicker extends AbstractXposedModule implements IXposedHookZygoteInit {

    /** アクション名保存用のキー */
    private static final String KEY_TARGET_ACTION = "targetAction";

    @XResource
    private ModAppPickerSettingsGen mSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (!mSettings.masterModAppPickerEnable) {
            // モジュールが無効になっている場合はここで終了
            return;
        }

        if (mSettings.showAlwaysUse) {
            // 常時のチェックボックス表示
            showAlwaysCheckBox(startupParam);
        }

        if (mSettings.appPickerBlackList != null && mSettings.appPickerBlackList.size() > 0) {
            // ブラックリストのアプリを除外
            hideBlackListApps();
        }

        if (mSettings.settingAlwaysPerApps) {
            // アプリごとに常時記憶を分離
            registerAlwaysCheckPerApps();
        }
    }

    private void showAlwaysCheckBox(final StartupParam startupParam) {
        final String resourceName;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            resourceName = "resolver_list";
        } else {
            resourceName = "resolver_grid";
        }
        XResources.hookSystemWideLayout("android", "layout", resourceName,
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam)
                            throws Throwable {
                        int id_button_bar = liparam.res.getIdentifier("button_bar", "id",
                                "android");
                        int id_button_always = liparam.res.getIdentifier("button_bar", "id",
                                "android");
                        int id_button_once = liparam.res.getIdentifier("button_bar", "id",
                                "android");
                        int id_resolver = liparam.res.getIdentifier(resourceName, "id",
                                "android");
                        Context context = liparam.view.getContext();

                        // 常時、一回のみボタンを非表示にする
                        View buttonAlways = liparam.view.findViewById(id_button_always);
                        View buttonOnce = liparam.view.findViewById(id_button_once);
                        buttonAlways.setVisibility(View.GONE);
                        buttonOnce.setVisibility(View.GONE);

                        // ボタンを入れてるLLを取得
                        LinearLayout buttonBar = (LinearLayout) liparam.view
                                .findViewById(id_button_bar);

                        // チェックボックスを作成
                        CheckBox checkBox = new CheckBox(context);
                        checkBox.setChecked(false);
                        XModuleResources moduleResources = XModuleResources.createInstance(
                                startupParam.modulePath, null);
                        checkBox.setText(moduleResources.getText(R.string.alwaysUse));

                        // LayoutParamsを作成
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        int fiveDp = DimenUtil.getPixelFromDp(context, 5);
                        layoutParams.setMargins(fiveDp, fiveDp, fiveDp, fiveDp);
                        layoutParams.gravity = Gravity.CENTER_VERTICAL;

                        // チェックボックスを追加
                        buttonBar.addView(checkBox, 0, layoutParams);

                        // チェックボックス、ボタン部分のLLを表示
                        checkBox.setVisibility(View.VISIBLE);
                        buttonBar.setVisibility(View.VISIBLE);

                        // ListViewにViewHolderを保存
                        ViewHolder viewHolder = new ViewHolder();
                        viewHolder.mAlwaysButton = buttonAlways;
                        viewHolder.mOnceButton = buttonOnce;
                        viewHolder.mAlwaysCheckBox = checkBox;
                        viewHolder.mButtonLayout = buttonBar;
                        View mAbsListView = liparam.view.findViewById(id_resolver);
                        mAbsListView.setTag(R.id.tag_app_picker_view_holder, viewHolder);
                    }
                });

        Class<?> clsResolverActivity = XposedHelpers.findClass(
                "com.android.internal.app.ResolverActivity", null);

        // onIntentSelected
        XposedHelpers.findAndHookMethod(clsResolverActivity, "onIntentSelected",
                ResolveInfo.class, Intent.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[2] = false;
                    }
                });
        XposedHelpers.findAndHookMethod(clsResolverActivity, "onCreate", Bundle.class,
                Intent.class, CharSequence.class, Intent[].class, java.util.List.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param)
                            throws Throwable {
                        final AbsListView mAbsListView = getAbsListView(param.thisObject);
                        ViewHolder viewHolder = (ViewHolder) mAbsListView
                                .getTag(R.id.tag_app_picker_view_holder);
                        final CheckBox mAlwaysCheckBox = viewHolder.mAlwaysCheckBox;
                        mAbsListView.setOnItemClickListener(new OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                                final int checkedPos = mAbsListView.getCheckedItemPosition();
                                final boolean enabled = checkedPos != GridView.INVALID_POSITION;
                                if (enabled) {
                                    XposedHelpers.callMethod(param.thisObject,
                                            "startSelected", position,
                                            mAlwaysCheckBox.isChecked());
                                }
                            }
                        });
                    }
                });
    }

    private AbsListView getAbsListView(Object thisObject) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return (AbsListView) XposedHelpers.getObjectField(thisObject, "mListView");
        } else {
            return (AbsListView) XposedHelpers.getObjectField(thisObject, "mGrid");
        }
    }

    /**
     * ブラックリストのアプリを非表示にする.
     */
    private void hideBlackListApps() {
        // ResolveListAdapter
        Class<?> clsResolveListAdapter = XposedHelpers.findClass(
                "com.android.internal.app.ResolverActivity$ResolveListAdapter", null);
        XposedHelpers.findAndHookMethod(clsResolveListAdapter, "processGroup",
                List.class, int.class, int.class, ResolveInfo.class, CharSequence.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        List<?> mList = (List<?>) XposedHelpers.getObjectField(param.thisObject,
                                "mList");

                        Iterator<?> i = mList.iterator();
                        while (i.hasNext()) {
                            Object obj = i.next();
                            ResolveInfo ri = (ResolveInfo) XposedHelpers.getObjectField(obj, "ri");
                            String packageName = ri.activityInfo.packageName;
                            if (mSettings.appPickerBlackList.contains(packageName)) {
                                // ブラックリストに含まれている場合は除外
                                i.remove();
                            }
                        }
                    }
                });
    }

    /**
     * アプリごとに「常に」状態を保存する.
     */
    private void registerAlwaysCheckPerApps() {
        Class<?> clsResolverActivity = XposedHelpers.findClass(
                "com.android.internal.app.ResolverActivity", null);
        XposedHelpers.findAndHookMethod(clsResolverActivity, "onCreate", Bundle.class,
                Intent.class, CharSequence.class, Intent[].class, java.util.List.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        int mLaunchedFromUid = XposedHelpers.getIntField(param.thisObject,
                                "mLaunchedFromUid");
                        String launchedFromPkg = activity.getPackageManager().getNameForUid(
                                mLaunchedFromUid);
                        log("launched from : " + launchedFromPkg);
                        log("current setting : " + mSettings.alwaysUsePerApps);

                        Intent paramIntent = (Intent) param.args[1];

                        // 呼び出し元のアクションを取得
                        String targetAction = paramIntent.getAction();
                        XposedHelpers.setAdditionalInstanceField(activity, KEY_TARGET_ACTION,
                                targetAction);

                        PerAppsSetting launchApp = mSettings.alwaysUsePerApps.findByAction(
                                launchedFromPkg, paramIntent.getAction());
                        if (launchApp != null) {
                            ListView mListView = (ListView) getObjectField(param.thisObject,
                                    "mListView");
                            ListAdapter adapter = mListView.getAdapter();
                            int childCount = adapter.getCount();
                            log("childCount:" + childCount);
                            for (int i = 0; i < childCount; i++) {
                                Object item = adapter.getItem(i);
                                ResolveInfo ri = (ResolveInfo) getObjectField(item, "ri");
                                if (TextUtils.equals(ri.activityInfo.packageName,
                                        launchApp.targetPackageName)
                                        && TextUtils.equals(ri.activityInfo.name,
                                                launchApp.targetActivityName)) {
                                    log("launch!! :" + ri);
                                    mListView.setItemChecked(i, true);
                                    XposedHelpers.callMethod(param.thisObject, "startSelected", i,
                                            false);
                                    break;
                                }
                            }
                        }
                    }
                });

        XposedHelpers.findAndHookMethod(clsResolverActivity, "startSelected", int.class,
                boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        int which = (Integer) param.args[0];
                        boolean always = (Boolean) param.args[1];

                        if (always) {
                            Activity activity = (Activity) param.thisObject;

                            // 常時の場合、選択したパッケージ名を保存する

                            // 呼び出し元を取得
                            int mLaunchedFromUid = XposedHelpers.getIntField(param.thisObject,
                                    "mLaunchedFromUid");
                            String launchedFromPkg = activity.getPackageManager().getNameForUid(
                                    mLaunchedFromUid);
                            log("from: " + launchedFromPkg);

                            // 選択したアプリを取得
                            Object mAdapter = XposedHelpers.getObjectField(param.thisObject,
                                    "mAdapter");
                            ResolveInfo ri = (ResolveInfo) XposedHelpers.callMethod(mAdapter,
                                    "resolveInfoForPosition", which);
                            String targetPackageName = ri.activityInfo.packageName;
                            String targetActivityName = ri.activityInfo.name;

                            // Actionを取得
                            Object additionalInstanceField = XposedHelpers
                                    .getAdditionalInstanceField(activity, KEY_TARGET_ACTION);
                            String targetAction = additionalInstanceField == null ? ""
                                    : (String) additionalInstanceField;

                            log("target: " + targetPackageName + ", " + targetActivityName + ", "
                                    + targetAction);

                            // 設定変更のブロードキャストを送信
                            Intent intent = new Intent(Const.ACTION_ALWAYS_USE_PER_APPS);
                            intent.putExtra(Const.EXTRA_LAUNCHED_FROM_PKG, launchedFromPkg);
                            intent.putExtra(Const.EXTRA_TARGET_PACKAGE_NAME, targetPackageName);
                            intent.putExtra(Const.EXTRA_TARGET_ACTIVITY_NAME, targetActivityName);
                            intent.putExtra(Const.EXTRA_TARGET_ACTION, targetAction);
                            sendBroadcast(activity, intent);

                            // 設定に追加
                            PerAppsSetting alwaysUsePerApps = new PerAppsSetting();
                            alwaysUsePerApps.launchedFromPackageName = launchedFromPkg;
                            alwaysUsePerApps.targetPackageName = targetPackageName;
                            alwaysUsePerApps.targetActivityName = targetActivityName;
                            alwaysUsePerApps.targetAction = targetAction;
                            mSettings.alwaysUsePerApps.list.add(alwaysUsePerApps);
                        }

                        // オリジナルを実行
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject,
                                new Object[] {
                                        param.args[0], false
                                });
                    }
                });
    }

    private static class ViewHolder {
        @SuppressWarnings("unused")
        LinearLayout mButtonLayout;
        @SuppressWarnings("unused")
        View mAlwaysButton;
        @SuppressWarnings("unused")
        View mOnceButton;
        CheckBox mAlwaysCheckBox;
    }
}
