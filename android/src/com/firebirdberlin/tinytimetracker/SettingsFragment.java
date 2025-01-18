package com.firebirdberlin.tinytimetracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.firebirdberlin.tinytimetracker.events.OnDatabaseImported;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;

import de.firebirdberlin.preference.SeekBarPreference;
import de.firebirdberlin.preference.SeekBarPreferenceDialogFragment;

// The callback interface
interface FileChooserListener {
    void onClick(String absoluteFilePath);
}

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "SettingsFragment";
    private String result = null;
    private DbImportExport dbImportExport;
    private static final int PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 3;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        dbImportExport = new DbImportExport(requireActivity());
        addPreferencesFromResource(R.xml.preferences);
        toggleEnabledState();
        setOnPreferenceClickListener("pref_key_data_export", new Preference.OnPreferenceClickListener() {
            @SuppressLint("NewApi")
            public boolean onPreferenceClick(@NonNull Preference preference) {
                dbImportExport.exportDb();
                toggleEnabledState();
                return true;
            }
        });
        setSummary("pref_key_data_import", dbImportExport.DATABASE_DIRECTORY.getAbsolutePath());
        setOnPreferenceClickListener("pref_key_data_import", new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(@NonNull Preference preference) {
                chooseFile(absoluteFilePath -> {
                    dbImportExport.restoreDb(absoluteFilePath);
                    EventBus bus = EventBus.getDefault();
                    bus.post(new OnDatabaseImported());
                });
                return true;
            }
        });

        setOnPreferenceClickListener("pref_key_data_share",
                preference -> {
                    chooseFile(absoluteFilePath -> {
                        Activity activity = getActivity();
                        if (activity != null) {
                            dbImportExport.shareFile(activity, absoluteFilePath);
                        }
                    });
                    return true;
                }
        );

        setOnPreferenceClickListener("pref_key_recommendation", new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(@NonNull Preference preference) {
                String body = "https://play.google.com/store/apps/details?id=com.firebirdberlin.tinytimetracker";
                String subject = getResources().getString(R.string.recommend_app_subject);
                String description = getResources().getString(R.string.recommend_app_desc);
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
                Activity activity = getActivity();
                if (activity != null) {
                    activity.startActivity(Intent.createChooser(sharingIntent, description));
                }
                return true;
            }
        });

        setOnPreferenceClickListener("pref_key_buy", new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Activity activity = getActivity();
                if (activity == null) {
                    return false;
                }
                ((Settings) activity).launchBillingFlow(BillingHelperActivity.ITEM_PRO);
                return true;
            }
        });

        setOnPreferenceChangeListener("pref_key_theme", new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String theme = (String) newValue;
                int mode = Settings.getDayNightTheme(theme);
                AppCompatDelegate.setDefaultNightMode(mode);
                return true;
            }
        });
        EditTextPreference referenceTimePreference = findPreference("pref_key_reference_time_months");
        if (referenceTimePreference != null) {
            referenceTimePreference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setHint("3");
                editText.setSelection(editText.getText().length());
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = getPreferenceScreen().getSharedPreferences();
        if (settings != null) {
            settings.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences settings = getPreferenceScreen().getSharedPreferences();
        if (settings != null) {
            settings.unregisterOnSharedPreferenceChangeListener(this);
        }
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

    void toggleEnabledState() {
        if (!isAdded()) {
            return;
        }
        setVisible("pref_key_buy", !isPurchased());
        setEnabled("pref_key_data_backup", isPurchased());
        setEnabled("pref_key_notification_new_access_points", isPurchased());

        File[] files = dbImportExport.listFiles();
        boolean enabled = (files != null && files.length > 0);

        setEnabled("pref_key_data_share", enabled);
        setEnabled("pref_key_data_import", enabled);
    }

    boolean isPurchased() {
        Activity activity = getActivity();
        if (activity instanceof Settings) {
            return ((Settings) activity).isPurchased(BillingHelperActivity.ITEM_PRO);
        }
        return false;
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
        final File file_list[] = dbImportExport.listFiles();

        if (file_list == null || file_list.length == 0) {
            return;
        }

        ArrayList<String> files = new ArrayList<>();

        for (File file : file_list) {
            files.add(file.getName());
        }

        final CharSequence[] files_seq = files.toArray(new CharSequence[0]);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        new AlertDialog.Builder(activity)
        .setTitle(activity.getResources().getString(R.string.dialog_title_database_backup))
        .setSingleChoiceItems(files_seq, 0, (dialog, item) -> {
            result = file_list[item].getAbsolutePath();
            Log.i(TAG, result);

            if (fileChooserListener != null) {
                fileChooserListener.onClick(result);
            }

            dialog.dismiss();
        })
        .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        Log.i(TAG, preference.getKey());
        if (preference instanceof SeekBarPreference) {
            DialogFragment dialogFragment = SeekBarPreferenceDialogFragment.newInstance(
                    (SeekBarPreference) preference
            );
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        } else super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_key_show_notifications")) {
            boolean showNotifications = sharedPreferences.getBoolean(key, false);
            if (showNotifications) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted");
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission denied");
                // Optionally, disable the preference if permission is denied
                SwitchPreferenceCompat switchPreference = findPreference("pref_key_show_notifications");
                if (switchPreference != null) {
                    switchPreference.setChecked(false);
                }
            }
        }
    }
}
