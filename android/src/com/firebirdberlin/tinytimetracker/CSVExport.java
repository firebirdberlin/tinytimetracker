package com.firebirdberlin.tinytimetracker;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v4.app.ShareCompat;

public class CSVExport {
    private static String TAG = TinyTimeTracker.TAG + ".CSVExport";
    private Context context = null;
    private String filename = "myfile.txt";

    public CSVExport(Context context, String filename) {
        this.context = context;
        this.filename = filename;
    }

    public void save(String string) {
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void share() {
        final File file = new File(context.getFilesDir(), filename);

        Log.i(TAG, file.getAbsolutePath());
        if (! file.exists()) {
            Log.i(TAG, "does not exist");
            return;
        }
        final Uri uri = FileProvider.getUriForFile(context, "com.firebirdberlin.tinytimetracker.fileprovider", file);
        final Intent intent = ShareCompat.IntentBuilder.from((Activity) context)
            .setType("text/csv")
            .setSubject("Share")
            .setStream(uri)
            .setChooserTitle("Share title")
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(intent);
    }
}
