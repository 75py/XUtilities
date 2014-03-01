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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Build;

import com.nagopy.android.xposed.util.XLog;
import com.nagopy.android.xposed.utilities.util.Const;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 後悔はしていない
 */
public class XposedModules implements IXposedHookZygoteInit, IXposedHookLoadPackage,
        IXposedHookInitPackageResources {

    List<XModuleInfo> mXModuleInfoList = new ArrayList<XposedModules.XModuleInfo>();
    private String modulePath;

    private static final String LOG_FORMAT_INVOKE = "invoke: %s(%s)";
    private static final String LOG_FORMAT_SUCCESS = "success: %s(%s)";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // モジュールパスを保存
        modulePath = startupParam.modulePath;

        XSharedPreferences xSharedPreferences = new XSharedPreferences(Const.PACKAGE_NAME);
        xSharedPreferences.reload();

        /** 適用するモジュールと、マスタ設定のキーのマップ */
        Map<Class<?>, String> xposedModules = new HashMap<Class<?>, String>();
        xposedModules.put(ModActionBar.class, "master_mod_action_bar_enable");
        xposedModules.put(ModAppPicker.class, "master_mod_app_picker_enable");
        xposedModules.put(ModBatteryIcon.class, "master_mod_battery_icon_enable");
        xposedModules.put(ModBrightness.class, "master_mod_brightness_enable");
        xposedModules.put(ModImmersiveFullScreenMode.class,
                "master_mod_immersive_full_screen_mode_enabled");
        xposedModules.put(ModLockscreenClock.class, "master_mod_lockscreen_clock_enable");
        xposedModules.put(ModLockscreenTorch.class, "master_mod_lockscreen_torch_enable");
        xposedModules.put(ModNotification.class, "master_mod_notification_enable");
        xposedModules.put(ModNotificationExpandedClock.class,
                "master_mod_notification_expanded_clock_enable");
        xposedModules.put(ModOtherUtilities.class, "master_mod_other_utilities_enable");
        xposedModules.put(ModStatusBarClock.class, "master_mod_status_bar_enable");
        xposedModules.put(ModToast.class, "master_mod_toast_dao_enable");

        for (Entry<Class<?>, String> entry : xposedModules.entrySet()) {
            Class<?> module = entry.getKey();

            if (!module.isAnnotationPresent(XposedModule.class)) {
                // モジュールじゃない場合は次へ
                continue;
            }
            XposedModule xposedModuleAnnotation = module.getAnnotation(XposedModule.class);

            XModuleInfo info = new XModuleInfo();
            info.clsName = module.getSimpleName();

            // モジュール設定のインスタンスを生成
            info.settings = xposedModuleAnnotation.setting().newInstance();

            // クラスについてるXMinSdkVersionを取得
            XMinSdkVersion classMinSdkVersion = module.getAnnotation(XMinSdkVersion.class);

            Method[] methods = module.getMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(InitZygote.class)) {
                    XModuleMethod initZygoteMethod = new XModuleMethod();

                    // メソッドをセットしとく
                    initZygoteMethod.method = method;

                    // 対象SDKバージョンを取得
                    initZygoteMethod.minSdkVersion = getMinSdkVersion(method, classMinSdkVersion);

                    // サマリー取得
                    InitZygote annotation = method.getAnnotation(InitZygote.class);
                    initZygoteMethod.summary = annotation.summary();

                    info.initZygote.add(initZygoteMethod);
                } else if (method.isAnnotationPresent(HandleLoadPackage.class)) {
                    XModuleMethod handleLoadPackage = new XModuleMethod();
                    HandleLoadPackage annotation = method.getAnnotation(HandleLoadPackage.class);

                    // メソッドをセットしとく
                    handleLoadPackage.method = method;

                    // 対象パッケージを取得
                    handleLoadPackage.targetPackageName = Arrays.asList(annotation.targetPackage());

                    // 対象SDKバージョンを取得
                    handleLoadPackage.minSdkVersion = getMinSdkVersion(method, classMinSdkVersion);

                    // サマリー取得
                    handleLoadPackage.summary = annotation.summary();

                    info.handleLoadPackage.add(handleLoadPackage);
                } else if (method.isAnnotationPresent(HandleInitPackageResources.class)) {
                    XModuleMethod handleInitPackageResources = new XModuleMethod();
                    HandleInitPackageResources annotation = method
                            .getAnnotation(HandleInitPackageResources.class);

                    // メソッドをセットしとく
                    handleInitPackageResources.method = method;

                    // 対象パッケージを取得
                    handleInitPackageResources.targetPackageName = Arrays.asList(annotation
                            .targetPackage());

                    // 対象SDKバージョンを取得
                    handleInitPackageResources.minSdkVersion = getMinSdkVersion(method,
                            classMinSdkVersion);

                    // サマリー取得
                    handleInitPackageResources.summary = annotation.summary();

                    info.handleInitPackageResources.add(handleInitPackageResources);
                }

            }

            // モジュールの有効・無効を判定
            info.enabled = xSharedPreferences.getBoolean(entry.getValue(), false);

            // 追加
            mXModuleInfoList.add(info);

            XLog.d(info);
            XposedBridge.log(info.toString());

        }

        // 実行
        for (XModuleInfo moduleInfo : mXModuleInfoList) {
            if (!moduleInfo.enabled) {
                // モジュールが無効の場合は次へ
                continue;
            }

            if (moduleInfo.initZygote.isEmpty()) {
                continue;
            }

            moduleInfo.d("initZygote START");
            for (XModuleMethod methodInfo : moduleInfo.initZygote) {
                if (Build.VERSION.SDK_INT >= methodInfo.minSdkVersion) {
                    try {
                        // ログ出力
                        moduleInfo.d(String.format(LOG_FORMAT_INVOKE, methodInfo.method.getName(),
                                methodInfo.summary));
                        // メソッド実行
                        methodInfo.method.invoke(null, startupParam, moduleInfo.settings);
                        // 成功ログ出力
                        moduleInfo.d(String.format(LOG_FORMAT_SUCCESS, methodInfo.method.getName(),
                                methodInfo.summary));
                    } catch (Throwable t) {
                        // エラーログ出力
                        XLog.e(t);
                    }
                }
            }
            moduleInfo.d("initZygote FINISH");
        }
    }

    /**
     * MinSdkVersionを取得
     * 
     * @param method
     * @param classMinSdkVersion
     * @return
     */
    private int getMinSdkVersion(Method method, XMinSdkVersion classMinSdkVersion) {
        if (classMinSdkVersion != null) {
            return classMinSdkVersion.value();
        } else if (method.isAnnotationPresent(XMinSdkVersion.class)) {
            XMinSdkVersion minSdkVersion = method.getAnnotation(XMinSdkVersion.class);
            if (minSdkVersion != null) {
                return minSdkVersion.value();
            }
        }

        return Build.VERSION_CODES.JELLY_BEAN;
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 実行
        for (XModuleInfo moduleInfo : mXModuleInfoList) {
            if (!moduleInfo.enabled) {
                // モジュールが無効の場合は次へ
                continue;
            }

            if (moduleInfo.handleLoadPackage.isEmpty()) {
                continue;
            }

            for (XModuleMethod methodInfo : moduleInfo.handleLoadPackage) {
                if (!methodInfo.targetPackageName.contains(lpparam.packageName)) {
                    // 対象じゃない場合はスキップ
                    continue;
                }

                if (Build.VERSION.SDK_INT >= methodInfo.minSdkVersion) {
                    try {
                        // ログ出力
                        moduleInfo.d(String.format(LOG_FORMAT_INVOKE, methodInfo.method.getName(),
                                methodInfo.summary));
                        // メソッド実行
                        methodInfo.method.invoke(null, modulePath, lpparam, moduleInfo.settings);
                        // 成功ログ出力
                        moduleInfo.d(String.format(LOG_FORMAT_SUCCESS, methodInfo.method.getName(),
                                methodInfo.summary));
                    } catch (Throwable t) {
                        // エラーログ出力
                        XLog.e(t);
                    }
                }
            }
        }
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        // 実行
        for (XModuleInfo moduleInfo : mXModuleInfoList) {
            if (!moduleInfo.enabled) {
                // モジュールが無効の場合は次へ
                continue;
            }

            if (moduleInfo.handleInitPackageResources.isEmpty()) {
                continue;
            }

            for (XModuleMethod methodInfo : moduleInfo.handleInitPackageResources) {
                if (!methodInfo.targetPackageName.contains(resparam.packageName)) {
                    // 対象じゃない場合はスキップ
                    continue;
                }

                if (Build.VERSION.SDK_INT >= methodInfo.minSdkVersion) {
                    try {
                        // ログ出力
                        moduleInfo.d(String.format(LOG_FORMAT_INVOKE, methodInfo.method.getName(),
                                methodInfo.summary));
                        // メソッド実行
                        methodInfo.method.invoke(null, modulePath, resparam, moduleInfo.settings);
                        // 成功ログ出力
                        moduleInfo.d(String.format(LOG_FORMAT_SUCCESS, methodInfo.method.getName(),
                                methodInfo.summary));
                    } catch (Throwable t) {
                        // エラーログ出力
                        XLog.e(t);
                    }
                }
            }
        }
    }

    private static class XModuleInfo {
        @Override
        public String toString() {
            return "XModuleInfo [clsName=" + clsName + ", enabled=" + enabled + ", initZygote="
                    + initZygote + ", handleLoadPackage=" + handleLoadPackage
                    + ", handleInitPackageResources=" + handleInitPackageResources + "]";
        }

        String clsName;

        /** 設定のインスタンス */
        Object settings;

        /** モジュールの有効判定 */
        boolean enabled;

        // それぞれの実行メソッド
        List<XModuleMethod> initZygote = new ArrayList<XposedModules.XModuleMethod>();
        List<XModuleMethod> handleLoadPackage = new ArrayList<XposedModules.XModuleMethod>();
        List<XModuleMethod> handleInitPackageResources = new ArrayList<XposedModules.XModuleMethod>();

        public void d(Object obj) {
            XLog.d(clsName, obj);
        }
    }

    private static class XModuleMethod {
        @Override
        public String toString() {
            return "XModuleMethod [method=" + method + ", minSdkVersion=" + minSdkVersion
                    + ", targetPackageName=" + targetPackageName + "]";
        }

        /** サマリー（ログ表示用） */
        String summary;

        /** 実行メソッド */
        Method method;
        /** 最低バージョン */
        int minSdkVersion = Build.VERSION_CODES.JELLY_BEAN;
        /** 対象パッケージ名 */
        List<String> targetPackageName = Collections.emptyList();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XModuleSettings {
    }

    @Target({
            ElementType.METHOD, ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XMinSdkVersion {
        int value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InitZygote {
        String summary() default "";
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HandleLoadPackage {
        String[] targetPackage();

        String summary() default "";
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HandleInitPackageResources {
        String[] targetPackage();

        String summary() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XposedModule {
        Class<?> setting();
    }

}
