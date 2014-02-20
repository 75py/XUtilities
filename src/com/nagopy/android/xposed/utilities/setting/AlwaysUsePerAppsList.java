
package com.nagopy.android.xposed.utilities.setting;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class AlwaysUsePerAppsList {

    public AlwaysUsePerAppsList() {
        list = new ArrayList<AlwaysUsePerAppsList.PerAppsSetting>();
    }

    public List<PerAppsSetting> list;

    public PerAppsSetting findByAction(String launchedFrom, String targetAction) {
        for (PerAppsSetting app : list) {
            if (TextUtils.equals(app.launchedFromPackageName, launchedFrom)
                    && TextUtils.equals(app.targetAction, targetAction)) {
                return app;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "AlwaysUsePerAppsList [list=" + list + "]";
    }

    public static class PerAppsSetting {

        public String launchedFromPackageName;

        public String targetPackageName;

        public String targetActivityName;

        public String targetAction;

        @Override
        public String toString() {
            return "PerAppsSetting [From=" + launchedFromPackageName
                    + ", target=" + targetPackageName + ", "
                    + targetActivityName + ", " + targetAction + "]";
        }

    }

}
