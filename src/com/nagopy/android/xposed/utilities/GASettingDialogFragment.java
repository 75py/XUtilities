
package com.nagopy.android.xposed.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.nagopy.android.common.util.DimenUtil;
import com.nagopy.android.xposed.utilities.util.Const;

public class GASettingDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    private CheckBox mCheckBox;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.app_name);

        Context applicationContext = getActivity().getApplicationContext();
        LinearLayout ll = new LinearLayout(applicationContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        int padding = DimenUtil.getPixelFromDp(applicationContext, 8);
        ll.setPadding(padding, padding, padding, padding);

        TextView textView = new TextView(applicationContext);
        textView.setTextAppearance(applicationContext, android.R.style.TextAppearance_Medium);
        textView.setText(R.string.ga_explain);

        mCheckBox = new CheckBox(applicationContext);
        mCheckBox.setTextAppearance(applicationContext, android.R.style.TextAppearance_Small);
        mCheckBox.setText(R.string.ga_dllow_anonymous_usage_reports);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        boolean isOptOut = sp.getBoolean(Const.KEY_GA_OPTOUT, false);
        mCheckBox.setChecked(isOptOut);

        ll.addView(textView, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ll.addView(mCheckBox, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        ScrollView scrollView = new ScrollView(applicationContext);
        scrollView.addView(ll);

        builder.setView(scrollView);

        builder.setPositiveButton(android.R.string.ok, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // OKが押されたら、設定を保存
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity()
                        .getApplicationContext());
                boolean isOptOut = mCheckBox.isChecked();
                sp.edit().putBoolean(Const.KEY_GA_OPTOUT, isOptOut).apply();

                GoogleAnalytics.getInstance(getActivity().getApplicationContext()).setAppOptOut(
                        isOptOut);
                break;
            default:
                break;
        }
    }

}
