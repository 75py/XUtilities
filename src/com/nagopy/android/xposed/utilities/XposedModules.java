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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
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

            XModuleInfo info = new XModuleInfo();
            info.moduleInstance = module.newInstance();

            // モジュール設定を検索、インスタンス生成
            Field[] fields = module.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(XModuleSettings.class)) {
                    Object settings = field.getType().getConstructor().newInstance();
                    field.setAccessible(true);
                    field.set(info.moduleInstance, settings);
                }
            }

            XMinSdkVersion classMinSdkVersion = module.getAnnotation(XMinSdkVersion.class);

            if (isImplemented(module, IXposedHookZygoteInit.class)) {
                XModuleMethod methodInfo = new XModuleMethod();
                methodInfo.method = module.getMethod("initZygote", StartupParam.class);
                if (methodInfo.method.isAnnotationPresent(XTargetPackage.class)) {
                    XTargetPackage targetPackage = methodInfo.method
                            .getAnnotation(XTargetPackage.class);
                    methodInfo.targetPackageName = Arrays.asList(targetPackage.value());
                }
                if (classMinSdkVersion != null) {
                    methodInfo.minSdkVersion = classMinSdkVersion.value();
                } else if (methodInfo.method.isAnnotationPresent(XMinSdkVersion.class)) {
                    XMinSdkVersion minSdkVersion = methodInfo.method
                            .getAnnotation(XMinSdkVersion.class);
                    if (minSdkVersion != null) {
                        methodInfo.minSdkVersion = minSdkVersion.value();
                    }
                }
                info.initZygote = methodInfo;
            }

            if (isImplemented(module, IXposedHookLoadPackage.class)) {
                XModuleMethod methodInfo = new XModuleMethod();
                methodInfo.method = module.getMethod("handleLoadPackage", LoadPackageParam.class);
                if (methodInfo.method.isAnnotationPresent(XTargetPackage.class)) {
                    XTargetPackage targetPackage = methodInfo.method
                            .getAnnotation(XTargetPackage.class);
                    methodInfo.targetPackageName = Arrays.asList(targetPackage.value());
                }
                if (classMinSdkVersion != null) {
                    methodInfo.minSdkVersion = classMinSdkVersion.value();
                } else if (methodInfo.method.isAnnotationPresent(XMinSdkVersion.class)) {
                    XMinSdkVersion minSdkVersion = methodInfo.method
                            .getAnnotation(XMinSdkVersion.class);
                    if (minSdkVersion != null) {
                        methodInfo.minSdkVersion = minSdkVersion.value();
                    }
                }
                info.handleLoadPackage = methodInfo;
            }

            if (isImplemented(module, IXposedHookInitPackageResources.class)) {
                XModuleMethod methodInfo = new XModuleMethod();
                methodInfo.method = module.getMethod("handleInitPackageResources",
                        InitPackageResourcesParam.class);
                if (methodInfo.method.isAnnotationPresent(XTargetPackage.class)) {
                    XTargetPackage targetPackage = methodInfo.method
                            .getAnnotation(XTargetPackage.class);
                    methodInfo.targetPackageName = Arrays.asList(targetPackage.value());
                }
                if (classMinSdkVersion != null) {
                    methodInfo.minSdkVersion = classMinSdkVersion.value();
                } else if (methodInfo.method.isAnnotationPresent(XMinSdkVersion.class)) {
                    XMinSdkVersion minSdkVersion = methodInfo.method
                            .getAnnotation(XMinSdkVersion.class);
                    if (minSdkVersion != null) {
                        methodInfo.minSdkVersion = minSdkVersion.value();
                    }
                }
                info.handleInitPackageResources = methodInfo;
            }

            // モジュールの有効・無効を判定
            info.enabled = xSharedPreferences.getBoolean(entry.getValue(), false);

            // 追加
            mXModuleInfoList.add(info);

            XLog.d(info);
        }

        // 実行
        for (XModuleInfo moduleInfo : mXModuleInfoList) {
            if (!moduleInfo.enabled) {
                // モジュールが無効の場合は次へ
                continue;
            }

            XModuleMethod methodInfo = moduleInfo.initZygote;

            if (methodInfo == null) {
                continue;
            }

            if (Build.VERSION.SDK_INT >= methodInfo.minSdkVersion) {
                try {
                    moduleInfo.d("initZygote START");
                    methodInfo.method.invoke(moduleInfo.moduleInstance, startupParam);
                    moduleInfo.d("initZygote COMPLETED!");
                } catch (Throwable t) {
                    moduleInfo.e("ERROR: " + t.getMessage());
                    XposedBridge.log(t);
                }
            }
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 実行
        for (XModuleInfo moduleInfo : mXModuleInfoList) {
            if (!moduleInfo.enabled) {
                // モジュールが無効の場合は次へ
                continue;
            }

            XModuleMethod methodInfo = moduleInfo.handleLoadPackage;

            if (methodInfo == null) {
                continue;
            }

            boolean checkPkg = methodInfo.targetPackageName.isEmpty()
                    || methodInfo.targetPackageName.contains(lpparam.packageName);
            if (checkPkg && Build.VERSION.SDK_INT >= methodInfo.minSdkVersion) {
                try {
                    moduleInfo.d("handleLoadPackage START");
                    methodInfo.method.invoke(moduleInfo.moduleInstance, lpparam);
                    moduleInfo.d("handleLoadPackage COMPLETED!");
                } catch (Throwable t) {
                    moduleInfo.e("ERROR: " + t.getMessage());
                    XposedBridge.log(t);
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

            XModuleMethod methodInfo = moduleInfo.handleInitPackageResources;

            if (methodInfo == null) {
                continue;
            }

            boolean checkPkg = methodInfo.targetPackageName.isEmpty()
                    || methodInfo.targetPackageName.contains(resparam.packageName);
            if (checkPkg && Build.VERSION.SDK_INT >= methodInfo.minSdkVersion) {
                try {
                    moduleInfo.d("handleInitPackageResources START");
                    methodInfo.method.invoke(moduleInfo.moduleInstance, resparam);
                    moduleInfo.d("handleInitPackageResources COMPLETED!");
                } catch (Throwable t) {
                    moduleInfo.e("ERROR: " + t.getMessage());
                    XposedBridge.log(t);
                }
            }
        }
    }

    public static boolean isImplemented(Class<?> clazz, Class<?> intrfc) {
        if (clazz == null || intrfc == null) {
            return false;
        }
        // インターフェースを実装したクラスであるかどうかをチェック
        if (!clazz.isInterface() && intrfc.isAssignableFrom(clazz)
                && !Modifier.isAbstract(clazz.getModifiers())) {
            return true;
        }
        return false;
    }

    private static class XModuleInfo {
        @Override
        public String toString() {
            return "XModuleInfo [moduleInstance=" + moduleInstance + ", enabled=" + enabled
                    + ", initZygote=" + initZygote + ", handleLoadPackage=" + handleLoadPackage
                    + ", handleInitPackageResources=" + handleInitPackageResources + "]";
        }

        /** モジュールのインスタンス */
        Object moduleInstance;
        /** モジュールの有効判定 */
        boolean enabled;
        XModuleMethod initZygote;
        XModuleMethod handleLoadPackage;
        XModuleMethod handleInitPackageResources;

        public void d(Object obj) {
            XLog.d(moduleInstance.getClass().getSimpleName(), obj);
        }

        public void e(Object obj) {
            XLog.e(moduleInstance.getClass().getSimpleName(), obj);
        }
    }

    private static class XModuleMethod {
        @Override
        public String toString() {
            return "XModuleMethod [method=" + method + ", minSdkVersion=" + minSdkVersion
                    + ", targetPackageName=" + targetPackageName + "]";
        }

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

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XTargetPackage {
        /** 処理対象のパッケージ名（任意） */
        String[] value();
    }

    @Target({
            ElementType.METHOD, ElementType.TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XMinSdkVersion {
        int value();
    }
}
