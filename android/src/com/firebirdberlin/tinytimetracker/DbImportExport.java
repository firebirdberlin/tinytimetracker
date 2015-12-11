package com.firebirdberlin.tinytimetracker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

public class DbImportExport {

    public static final String TAG = DbImportExport.class.getName();

    /** Directory that files are to be read from and written to **/
    protected static final File DATABASE_DIRECTORY =
        new File(Environment.getExternalStorageDirectory(), "TinyTimeTracker");

    /** File path of Db to be imported **/
    protected static final File IMPORT_FILE = new File(DATABASE_DIRECTORY, "trackers.db");

    protected static final String PACKAGE_NAME = "com.firebirdberlin.tinytimetracker";
    protected static final String DATABASE_NAME = "trackers";
    protected static final String DATABASE_NAME_EXT = ".db";

    private static final int MIN_NUMBER_OF_BACKUPS_TO_KEEP = 10;
    private static final long MAX_FILE_AGE = 31 * 24 * 60 * 60 * 1000;


    /** Contains: /data/data/com.example.app/databases/example.db **/
    private static final File DATA_DIRECTORY_DATABASE = new File(Environment.getDataDirectory() +
            "/data/" + PACKAGE_NAME +
            "/databases/" + DATABASE_NAME +
            DATABASE_NAME_EXT );

    /** Saves the application database to the
     * export directory under MyDb.db **/
    protected static  boolean exportDb(){
        Log.d(TAG, "exportDb()");
        if( ! SdIsPresent() ) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());

        File dbFile = DATA_DIRECTORY_DATABASE;
        String filename = DATABASE_NAME + "_" + currentDateAndTime + DATABASE_NAME_EXT;

        File exportDir = DATABASE_DIRECTORY;
        File file = new File(exportDir, filename);

        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        try {
            file.createNewFile();
            copyFile(dbFile, file);
            Log.d(TAG, "exported " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        deleteOldBackupFiles();
        return true;
    }

    protected static void shareFile(Context context, String filename) {
        Uri uri = Uri.parse(filename);
        Intent email = new Intent(android.content.Intent.ACTION_SEND);
        email.setType("application/octet-stream");
        email.putExtra(Intent.EXTRA_STREAM, uri);
        email.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file:" + filename));

        context.startActivity(Intent.createChooser(email,
                              context.getResources().getString(R.string.dialog_title_share_database)));
    }

    /** Replaces current database with the IMPORT_FILE if
     * import database is valid and of the correct type **/
    protected static boolean restoreDb(String filename){
        Log.d(TAG, "restoreDb()");
        if( ! SdIsPresent() ) return false;

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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    private static void deleteOldBackupFiles() {
        File[] files = listFiles();
        if (files.length < MIN_NUMBER_OF_BACKUPS_TO_KEEP) return;

        for (File file: files) {
            if (file.lastModified() + MAX_FILE_AGE < System.currentTimeMillis()) {
                file.delete();
            }
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

        return DbImportExport.DATABASE_DIRECTORY.listFiles(filter);
    }


    /** Returns whether an SD card is present and writable **/
    public static boolean SdIsPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
