package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.firebirdberlin.tinytimetracker.events.OnDatabaseImported;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;

import de.firebirdberlin.preference.SeekBarPreference;
import de.firebirdberlin.preference.SeekBarPreferenceDialogFragment;

// The callback interface
interface FileChooserListener {
    public void onClick(String absoluteFilePath);
}

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String TAG = "SettingsFragment";
    private String result = null;
    private final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        toggleEnabledState();
        setOnPreferenceClickListener("pref_key_data_export", new Preference.OnPreferenceClickListener() {
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
        setSummary("pref_key_data_import", DbImportExport.DATABASE_DIRECTORY.getAbsolutePath());
        setOnPreferenceClickListener("pref_key_data_import", new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                chooseFile(new FileChooserListener() {
                    public void onClick(String absoluteFilePath) {
                        DbImportExport.restoreDb(absoluteFilePath);
                        EventBus bus = EventBus.getDefault();
                        bus.post(new OnDatabaseImported());
                    }
                } );
                return true;
            }
        });

        setOnPreferenceClickListener("pref_key_data_share",
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        chooseFile(new FileChooserListener() {
                            public void onClick(String absoluteFilePath) {
                                DbImportExport.shareFile(getActivity(), absoluteFilePath);
                            }
                        } );
                        return true;
                    }
                }
        );

        setOnPreferenceClickListener("pref_key_recommendation", new Preference.OnPreferenceClickListener() {
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

        setOnPreferenceClickListener("pref_key_buy", new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((Settings) getActivity()).launchBillingFlow(BillingHelperActivity.ITEM_PRO);
                return true;
            }
        });

        setOnPreferenceChangeListener("pref_key_theme", new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String theme = (String) newValue;
                int mode = Settings.getDayNightTheme(theme);
                AppCompatDelegate.setDefaultNightMode(mode);
                return true;
            }
        });
    }

    private void setOnPreferenceClickListener(String key, Preference.OnPreferenceClickListener listener) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceClickListener(listener);
        }
    }

    private void setOnPreferenceChangeListener(String key, Preference.OnPreferenceChangeListener listener) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(listener);
        }
    }

    void setSummary(String key, String summary) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(summary);
        }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission WRITE_EXTERNAL_STORAGE granted");
                DbImportExport.exportDb();
                toggleEnabledState();
            } else {
                Log.e(TAG, "permission WRITE_EXTERNAL_STORAGE denied");
            }
        }
    }

    void toggleEnabledState() {
        if (!isAdded()) {
            return;
        }
        setVisible("pref_key_buy", !isPurchased());
        setEnabled("pref_key_data_backup", isPurchased());
        setEnabled("pref_key_notification_new_access_points", isPurchased());
        boolean enabled = false;
        Preference pref_data_import = findPreference("pref_key_data_import");
        Preference pref_data_share = findPreference("pref_key_data_share");
        if (hasPermissionWriteExternalStorage() ) {
            File[] files = DbImportExport.listFiles();
            enabled = (files != null && files.length > 0);
        }

        pref_data_share.setEnabled(enabled);
        pref_data_import.setEnabled(enabled);
    }

    boolean isPurchased() {
        return ((Settings) getActivity()).isPurchased(BillingHelperActivity.ITEM_PRO);
    }

    void setVisible(String key, boolean on) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setVisible(on);
        }
    }

    void setEnabled(String key, boolean on) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setEnabled(on);
        }
    }

    private void chooseFile(final FileChooserListener fileChooserListener) {
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

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        Log.i(TAG, preference.getKey());
        if (preference instanceof SeekBarPreference) {
            DialogFragment dialogFragment = SeekBarPreferenceDialogFragment.newInstance(
                    (SeekBarPreference) preference
            );
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        } else super.onDisplayPreferenceDialog(preference);
    }
}
