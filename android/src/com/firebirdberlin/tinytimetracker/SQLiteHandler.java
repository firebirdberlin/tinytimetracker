package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHandler extends SQLiteOpenHelper {

  public static final String TABLE_TRACKERS = "trackers";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_METHOD = "method";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_VERBOSE = "verbose_name";

  public static final String TABLE_LOGS = "logs";
  public static final String COLUMN_TIMESTAMP_END = "timestamp_end";
  public static final String COLUMN_TIMESTAMP_START = "timestamp_start";
  public static final String COLUMN_TRACKER_ID = "tracker_id";

  private static final String DATABASE_NAME = "trackers.db";
  private static final int DATABASE_VERSION = 2;

  // Database creation sql statement
  private static final String DATABASE_CREATE_TRACKERS =
        "CREATE TABLE " + TABLE_TRACKERS + "("
          + COLUMN_ID + " integer primary key autoincrement, "
          + COLUMN_METHOD + " text not null, "
          + COLUMN_NAME + " text not null, "
          + COLUMN_VERBOSE + " text not null);";
  private static final String DATABASE_CREATE_LOGS =
        "CREATE TABLE " + TABLE_LOGS + "("
          + COLUMN_ID + " integer primary key autoincrement, "
          + COLUMN_TRACKER_ID + " integer REFERENCES " + TABLE_TRACKERS +"(" + COLUMN_ID + "),"
          + COLUMN_TIMESTAMP_START + " integer not null, "
          + COLUMN_TIMESTAMP_END + " integer not null);";

  public SQLiteHandler(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
      database.execSQL(DATABASE_CREATE_TRACKERS);
      database.execSQL(DATABASE_CREATE_LOGS);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(SQLiteHandler.class.getName(),
              "Upgrading database from version " + oldVersion + " to "
              + newVersion + ", which will destroy all old data");

      if (oldVersion == 1 && newVersion == 2) {
          db.execSQL("ALTER TABLE " + TABLE_TRACKERS + " ADD COLUMN " + COLUMN_VERBOSE + " TEXT DEFAULT '' NOT NULL");
          db.execSQL("UPDATE " + TABLE_TRACKERS + " SET " + COLUMN_VERBOSE + " = " + COLUMN_NAME);
          return;
      }
      // otherwise drop and re-create
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKERS);
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
      onCreate(db);
  }

}
