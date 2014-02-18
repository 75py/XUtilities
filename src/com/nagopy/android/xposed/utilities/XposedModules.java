
package com.nagopy.android.xposed.utilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.os.Build;

import com.nagopy.android.xposed.util.XLog;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 後悔はしていない
 */
public class XposedModules implements IXposedHookZygoteInit, IXposedHookLoadPackage,
        IXposedHookInitPackageResources {

    protected Class<?>[] mXposedModules = {
            ModActionBar.class,
            ModAppPicker.class,
            ModBatteryIcon.class,
            ModBrightness.class,
            ModImmersiveFullScreenMode.class,
            ModLockscreenClock.class,
            ModLockscreenTorch.class,
            ModNotification.class,
            ModNotificationExpandedClock.class,
            ModOtherUtilities.class,
            ModStatusBarClock.class,
            ModToast.class,
    };

    List<XModuleInfo> initZygote = new ArrayList<XposedModules.XModuleInfo>();
    List<XModuleInfo> handleLoadPackage = new ArrayList<XposedModules.XModuleInfo>();
    List<XModuleInfo> handleInitPackageResources = new ArrayList<XposedModules.XModuleInfo>();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        for (Class<?> module : mXposedModules) {
            Object moduleInstance = module.newInstance();
            XMinSdkVersion classMinSdkVersion = module.getAnnotation(XMinSdkVersion.class);
            if (isImplemented(module, IXposedHookZygoteInit.class)) {
                XModuleInfo info = new XModuleInfo();
                info.moduleInstance = moduleInstance;
                info.method = module.getMethod("initZygote", StartupParam.class);
                XTargetPackage targetPackage = info.method.getAnnotation(XTargetPackage.class);
                if (targetPackage != null) {
                    info.targetPackageName = Arrays.asList(targetPackage.value());
                }
                if (classMinSdkVersion != null) {
                    info.minSdkVersion = classMinSdkVersion.value();
                } else {
                    XMinSdkVersion minSdkVersion = info.method.getAnnotation(XMinSdkVersion.class);
                    if (minSdkVersion != null) {
                        info.minSdkVersion = minSdkVersion.value();
                    }
                }
                initZygote.add(info);
            }
            if (isImplemented(module, IXposedHookLoadPackage.class)) {
                XModuleInfo info = new XModuleInfo();
                info.moduleInstance = moduleInstance;
                info.method = module.getMethod("handleLoadPackage", LoadPackageParam.class);
                XTargetPackage targetPackage = info.method.getAnnotation(XTargetPackage.class);
                if (targetPackage != null) {
                    info.targetPackageName = Arrays.asList(targetPackage.value());
                }
                if (classMinSdkVersion != null) {
                    info.minSdkVersion = classMinSdkVersion.value();
                } else {
                    XMinSdkVersion minSdkVersion = info.method.getAnnotation(XMinSdkVersion.class);
                    if (minSdkVersion != null) {
                        info.minSdkVersion = minSdkVersion.value();
                    }
                }
                handleLoadPackage.add(info);
            }
            if (isImplemented(module, IXposedHookInitPackageResources.class)) {
                XModuleInfo info = new XModuleInfo();
                info.moduleInstance = moduleInstance;
                info.method = module.getMethod("handleInitPackageResources",
                        InitPackageResourcesParam.class);
                XTargetPackage targetPackage = info.method.getAnnotation(XTargetPackage.class);
                if (targetPackage != null) {
                    info.targetPackageName = Arrays.asList(targetPackage.value());
                }
                if (classMinSdkVersion != null) {
                    info.minSdkVersion = classMinSdkVersion.value();
                } else {
                    XMinSdkVersion minSdkVersion = info.method.getAnnotation(XMinSdkVersion.class);
                    if (minSdkVersion != null) {
                        info.minSdkVersion = minSdkVersion.value();
                    }
                }
                handleInitPackageResources.add(info);
            }
        }

        // 実行
        for (XModuleInfo info : initZygote) {
            if (Build.VERSION.SDK_INT >= info.minSdkVersion) {
                try {
                    info.d("initZygote START");
                    info.method.invoke(info.moduleInstance, startupParam);
                    info.d("initZygote COMPLETED!");
                } catch (Throwable t) {
                    info.e("ERROR: " + t.getMessage());
                    XposedBridge.log(t);
                }
            }
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 実行
        for (XModuleInfo info : handleLoadPackage) {
            if (info.targetPackageName.contains(lpparam.packageName)
                    && Build.VERSION.SDK_INT >= info.minSdkVersion) {
                try {
                    info.d("handleLoadPackage START");
                    info.method.invoke(info.moduleInstance, lpparam);
                    info.d("handleLoadPackage COMPLETED!");
                } catch (Throwable t) {
                    info.e("ERROR: " + t.getMessage());
                    XposedBridge.log(t);
                }
            }
        }
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        // 実行
        for (XModuleInfo info : handleInitPackageResources) {
            if (info.targetPackageName.contains(resparam.packageName)
                    && Build.VERSION.SDK_INT >= info.minSdkVersion) {
                try {
                    info.d("handleInitPackageResources START");
                    info.method.invoke(info.moduleInstance, resparam);
                    info.d("handleInitPackageResources COMPLETED!");
                } catch (Throwable t) {
                    info.e("ERROR: " + t.getMessage());
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
        Object moduleInstance;
        Method method;
        int minSdkVersion = Build.VERSION_CODES.JELLY_BEAN;
        List<String> targetPackageName = Collections.emptyList();

        public void d(Object obj) {
            XLog.d(moduleInstance.getClass().getSimpleName(), obj);
        }

        public void e(Object obj) {
            XLog.e(moduleInstance.getClass().getSimpleName(), obj);
        }
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
