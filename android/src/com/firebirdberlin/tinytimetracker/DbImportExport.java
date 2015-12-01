package com.firebirdberlin.tinytimetracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
 
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
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
 
	public static final String PACKAGE_NAME = "com.firebirdberlin.tinytimetracker";
	public static final String DATABASE_NAME = "trackers.db";
 
	/** Contains: /data/data/com.example.app/databases/example.db **/
	private static final File DATA_DIRECTORY_DATABASE = new File(Environment.getDataDirectory() +
                                                                 "/data/" + PACKAGE_NAME +
                                                                 "/databases/" + DATABASE_NAME );
 
	/** Saves the application database to the
	 * export directory under MyDb.db **/
	protected static  boolean exportDb(){
        Log.d(TAG, "exportDb()");
		if( ! SdIsPresent() ) return false;
 
		File dbFile = DATA_DIRECTORY_DATABASE;
		String filename = DATABASE_NAME;
 
		File exportDir = DATABASE_DIRECTORY;
		File file = new File(exportDir, filename);
//		File file = IMPORT_FILE;
 
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
 
		try {
			file.createNewFile();
			copyFile(dbFile, file);
            Log.d(TAG, "exported " + file.getAbsolutePath());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

    protected static void shareFile(Context context) {
        String path = IMPORT_FILE.getAbsolutePath();
        Uri uri = Uri.parse(path);
        Intent email = new Intent(android.content.Intent.ACTION_SEND);
        email.setType("application/octet-stream");
        email.putExtra(Intent.EXTRA_STREAM, uri);
        email.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file:" + path));

        context.startActivity(Intent.createChooser(email, "Send mail..."));
    }
 
	/** Replaces current database with the IMPORT_FILE if
	 * import database is valid and of the correct type **/
	protected static boolean restoreDb(){
        Log.d(TAG, "restoreDb()");
		if( ! SdIsPresent() ) return false;
 
		File exportFile = DATA_DIRECTORY_DATABASE;
		File importFile = IMPORT_FILE;
 
		// if( ! checkDbIsValid(importFile) ) return false;
 
		if (!importFile.exists()) {
			Log.d(TAG, "File does not exist");
			return false;
		}
 
		try {
			exportFile.createNewFile();
			copyFile(importFile, exportFile);
            Log.d(TAG, "restored from " + importFile.getAbsolutePath());
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
 
	/** Returns whether an SD card is present and writable **/
	public static boolean SdIsPresent() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
}
