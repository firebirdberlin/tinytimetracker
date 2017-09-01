package com.firebirdberlin.tinytimetracker;

import java.io.File;
import java.util.ArrayList;

import com.firebirdberlin.tinytimetracker.events.OnDatabaseImported;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import org.greenrobot.eventbus.EventBus;

// The callback interface
interface FileChooserListener {
    public void onClick(String absoluteFilePath);
}

public class SettingsFragment extends PreferenceFragment {
    public static final String TAG = TinyTimeTracker.TAG + ".SettingsFragment";
    private String result = null;
    private final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        toggleEnabledState();
        Preference pref_data_export = (Preference) findPreference("pref_key_data_export");
        pref_data_export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @SuppressLint("NewApi")
            public boolean onPreferenceClick(Preference preference) {

                if ( ! hasPermissionWriteExternalStorage() ) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                       PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    DbImportExport.exportDb();
                    toggleEnabledState();
                }

                return true;
            }
        });
        Preference pref_data_import = (Preference) findPreference("pref_key_data_import");
        pref_data_import.setSummary(DbImportExport.DATABASE_DIRECTORY.getAbsolutePath());
        pref_data_import.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                chooseFile( new FileChooserListener() {
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
                chooseFile( new FileChooserListener() {
                    public void onClick(String absoluteFilePath) {
                        DbImportExport.shareFile(getActivity(), absoluteFilePath);
                    }
                } );
                return true;
            }
        });

        Preference pref_key_recommendation = (Preference) findPreference("pref_key_recommendation");
        pref_key_recommendation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                String body = "https://play.google.com/store/apps/details?id=com.firebirdberlin.tinytimetracker";
                String subject = getResources().getString(R.string.recommend_app_subject);
                String description = getResources().getString(R.string.recommend_app_desc);
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
                getActivity().startActivity(Intent.createChooser(sharingIntent, description));
                return true;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private boolean hasPermissionWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= 23 ) {
            return ( getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED );
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission WRITE_EXTERNAL_STORAGE granted");
                    DbImportExport.exportDb();
                    toggleEnabledState();
                } else {
                    Log.e(TAG, "permission WRITE_EXTERNAL_STORAGE denied");
                }
                return;
            }
        }
    }

    private void toggleEnabledState() {
        boolean enabled = false;
        Preference pref_data_import = (Preference) findPreference("pref_key_data_import");
        Preference pref_data_share = (Preference) findPreference("pref_key_data_share");
        if (hasPermissionWriteExternalStorage() ) {
            File[] files = DbImportExport.listFiles();
            enabled = (files != null && files.length > 0);
        }

        pref_data_share.setEnabled(enabled);
        pref_data_import.setEnabled(enabled);
    }

    public void chooseFile(final FileChooserListener fileChooserListener) {
        final File file_list[] = DbImportExport.listFiles();

        if (file_list == null || file_list.length == 0) {
            return;
        }

        ArrayList<String> files = new ArrayList<String>();

        for (File file : file_list) {
            files.add(file.getName());
        }

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
