package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class CSVExport {
    private static String TAG = TinyTimeTracker.TAG + ".CSVExport";
    private Context context = null;
    private String filename = "";
    private File file;

    public CSVExport(Context context, String filename) {
        this.context = context;
        this.filename = filename;
    }

    public void save(String string) {
        file = new File(context.getCacheDir(), filename);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void share(String subject) {
        if (file == null || ! file.exists()) {
            Log.i(TAG, "does not exist");
            return;
        }
        String chooserTitle = context.getResources().getString(R.string.dialog_title_share_database);
        final Uri uri = FileProvider.getUriForFile(
                context, "com.firebirdberlin.tinytimetracker.fileprovider", file
        );
        final Intent intent = ShareCompat.IntentBuilder.from((AppCompatActivity) context)
            .setType("text/csv")
            .setSubject(subject)
            .setStream(uri)
            .setChooserTitle(chooserTitle)
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(intent);
    }
}
