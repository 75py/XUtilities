
package com.nagopy.android.xposed.utilities;

import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;

import com.nagopy.android.xposed.AbstractXposedModule;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ModAppPicker extends AbstractXposedModule implements IXposedHookZygoteInit {

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        final XModuleResources modRes = XModuleResources.createInstance(
                startupParam.modulePath, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            XResources.setSystemWideReplacement("android", "layout", "resolver_list",
                    modRes.fwd(R.layout.resolver_list_alt));
        } else {
            XResources.setSystemWideReplacement("android", "layout", "resolver_grid",
                    modRes.fwd(R.layout.resolver_grid_alt));
        }

        Class<?> clsResolverActivity = XposedHelpers.findClass(
                "com.android.internal.app.ResolverActivity", null);
        XposedHelpers.findAndHookMethod(clsResolverActivity, "onCreate", Bundle.class,
                Intent.class, CharSequence.class, Intent[].class, java.util.List.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param)
                            throws Throwable {
                        View mAlwaysButton = (View) XposedHelpers.getObjectField(
                                param.thisObject, "mAlwaysButton");
                        ViewGroup buttonLayout = (ViewGroup) mAlwaysButton.getParent();
                        final CheckBox mAlwaysCheckBox = (CheckBox) buttonLayout.getChildAt(0);
                        final AbsListView mAbsListView = getAbsListView(param.thisObject);
                        mAbsListView.setOnItemClickListener(new OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                                if (XposedHelpers.getBooleanField(param.thisObject,
                                        "mAlwaysUseOption")) {
                                    final int checkedPos = mAbsListView.getCheckedItemPosition();
                                    final boolean enabled = checkedPos != GridView.INVALID_POSITION;
                                    if (enabled) {
                                        XposedHelpers.callMethod(param.thisObject,
                                                "startSelected", position,
                                                mAlwaysCheckBox.isChecked());
                                    }
                                } else {
                                    XposedHelpers.callMethod(param.thisObject, "startSelected",
                                            position, false);
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
}
