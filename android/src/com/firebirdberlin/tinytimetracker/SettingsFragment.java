package com.firebirdberlin.tinytimetracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import de.greenrobot.event.EventBus;
import java.io.File;
import java.util.ArrayList;

// The callback interface
interface FileChooserListener {
    public void onClick(String absoluteFilePath);
}

public class SettingsFragment extends PreferenceFragment {
    public static final String TAG = TinyTimeTracker.TAG + ".SettingsFragment";
    private String result = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);

        toggleEnabledState();

        Preference pref_data_export = (Preference) findPreference("pref_key_data_export");
        pref_data_export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                DbImportExport.exportDb();
                toggleEnabledState();
                return true;
            }
        });

        Preference pref_data_import = (Preference) findPreference("pref_key_data_import");

        pref_data_import.setSummary(DbImportExport.DATABASE_DIRECTORY.getAbsolutePath());

        pref_data_import.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                chooseFile( new FileChooserListener(){
                    public void onClick(String absoluteFilePath) {
                        DbImportExport.restoreDb(absoluteFilePath);
                        EventBus bus = EventBus.getDefault();
                        bus.post(new OnDatabaseImported());
                    }
                } );
                return true;
            }
        });

        Preference pref_data_share = (Preference) findPreference("pref_key_data_share");
        pref_data_share.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                chooseFile( new FileChooserListener(){
                    public void onClick(String absoluteFilePath) {
                        DbImportExport.shareFile(getActivity(), absoluteFilePath);
                    }
                } );
                return true;
            }
        });
    }

    private void toggleEnabledState() {
        Preference pref_data_import = (Preference) findPreference("pref_key_data_import");
        Preference pref_data_share = (Preference) findPreference("pref_key_data_share");

        File[] files = DbImportExport.listFiles();
        boolean enabled = (files.length > 0);
        pref_data_share.setEnabled(enabled);
        pref_data_import.setEnabled(enabled);
    }

    public void chooseFile(final FileChooserListener fileChooserListener) {
        final File file_list[] = DbImportExport.listFiles();

        if (file_list.length == 0) return;

        ArrayList<String> files = new ArrayList<String>();
        for (File file : file_list) files.add(file.getName());

        final CharSequence[] files_seq = files.toArray(new CharSequence[files.size()]);

        new AlertDialog.Builder(getActivity())
            .setTitle(getActivity().getResources().getString(R.string.dialog_title_database_backup))
            .setSingleChoiceItems(files_seq, 0, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    result = file_list[item].getAbsolutePath();
                    Log.i(TAG, result);
                    if (fileChooserListener != null) {
                        fileChooserListener.onClick(result);
                    }
                    dialog.dismiss();
                }
            })
        .setNegativeButton(android.R.string.no, null).show();
    }
}
