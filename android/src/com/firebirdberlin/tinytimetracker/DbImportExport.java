package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class DbImportExport {

    public static final String TAG = DbImportExport.class.getName();

    /** Directory that files are to be read from and written to **/
    protected static final File DATABASE_DIRECTORY =
        new File(
                Environment.getExternalStorageDirectory(),
                "TinyTimeTracker"
        );

    /** File path of Db to be imported **/
    protected static final File IMPORT_FILE = new File(DATABASE_DIRECTORY, "trackers.db");

    protected static final String PACKAGE_NAME = "com.firebirdberlin.tinytimetracker";
    protected static final String DATABASE_NAME = "trackers";
    protected static final String DATABASE_NAME_EXT = ".db";

    private static final int MIN_NUMBER_OF_BACKUPS_TO_KEEP = 10;


    /** Contains: /data/data/com.example.app/databases/example.db **/
    private static final File DATA_DIRECTORY_DATABASE =
            new File(
                    Environment.getDataDirectory() +
                            "/data/" + PACKAGE_NAME +
                            "/databases/" + DATABASE_NAME +
                            DATABASE_NAME_EXT
            );

    /** Saves the application database to the
     * export directory under MyDb.db **/
    protected static  boolean exportDb() {
        Log.d(TAG, "exportDb()");

        if( ! SdIsPresent() ) {
            Log.d(TAG, "Failure: Storage device is not writable.");
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());
        File dbFile = DATA_DIRECTORY_DATABASE;
        String filename = DATABASE_NAME + "_" + currentDateAndTime + DATABASE_NAME_EXT;
        createDatabaseDirectory();
        File file = new File(DATABASE_DIRECTORY, filename);

        try {
            file.createNewFile();
            copyFile(dbFile, file);
            Log.d(TAG, "exported " + file.getAbsolutePath());
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        deleteOldBackupFiles();
        return true;
    }

    private static void createDatabaseDirectory() {
        if (!DATABASE_DIRECTORY.exists()) {
            DATABASE_DIRECTORY.mkdirs();
        }
    }

    protected static void shareFile(Context context, String filename) {
        File file = new File(filename);
        String chooserTitle = context.getResources().getString(R.string.dialog_title_share_database);
        final Uri uri = FileProvider.getUriForFile(
                context, "com.firebirdberlin.tinytimetracker.fileprovider", file
        );
        final Intent intent = ShareCompat.IntentBuilder.from((AppCompatActivity) context)
            .setType("text/csv")
            .setSubject(file.getName())
            .setStream(uri)
            .setChooserTitle(chooserTitle)
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(intent);
    }

    /** Replaces current database with the IMPORT_FILE if
     * import database is valid and of the correct type **/
    protected static boolean restoreDb(String filename) {
        Log.d(TAG, "restoreDb()");

        if( ! SdIsPresent() ) {
            return false;
        }

        File exportFile = DATA_DIRECTORY_DATABASE;
        File importFile = new File(filename);

        // if( ! checkDbIsValid(importFile) ) return false;

        if (!importFile.exists()) {
            Log.d(TAG, "File does not exist");
            return false;
        }

        try {
            exportFile.createNewFile();
            copyFile(importFile, exportFile);
            Log.d(TAG, "restored from " + filename);
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally {
            if (inChannel != null) {
                inChannel.close();
            }

            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    private static void deleteOldBackupFiles() {
        File[] files = listFiles();

        if (files.length < MIN_NUMBER_OF_BACKUPS_TO_KEEP) {
            return;
        }

        // drop the first elements from the list
        files = Arrays.copyOfRange(files, MIN_NUMBER_OF_BACKUPS_TO_KEEP, files.length);
        // delete the remaining files
        for (File file: files) {
            file.delete();
        }
    }

    public static File[] listFiles() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                return (filename.startsWith("trackers") &&
                        filename.endsWith(".db")) || sel.isDirectory();
            }
        };
        createDatabaseDirectory();

        File[] files = DbImportExport.DATABASE_DIRECTORY.listFiles(filter);
        // sort by descending modified time
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                }
            });
        }
        return files;
    }


    /** Returns whether an SD card is present and writable **/
    public static boolean SdIsPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
