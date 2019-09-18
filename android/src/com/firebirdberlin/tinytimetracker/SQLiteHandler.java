package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class SQLiteHandler extends SQLiteOpenHelper {

    public static final String TABLE_TRACKERS = "trackers";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_METHOD = "method";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_WORKING_HOURS = "working_hours";
    public static final String COLUMN_VERBOSE = "verbose_name";
    public static final String COLUMN_OPERATION_STATE = "operation_state";

    public static final String TABLE_LOGS = "logs";
    public static final String COLUMN_TIMESTAMP_END = "timestamp_end";
    public static final String COLUMN_TIMESTAMP_START = "timestamp_start";
    public static final String COLUMN_TRACKER_ID = "tracker_id";

    public static final String TABLE_ACCESS_POINTS = "access_points";
    public static final String TABLE_IGNORED_ACCESS_POINTS = "ignored_access_points";
    public static final String COLUMN_SSID = "ssid";
    public static final String COLUMN_BSSID = "bssid";

    public static final String TABLE_TIME_BALANCE = "time_balance";
    public static final String COLUMN_INSERT_TIME = "insert_time";
    public static final String COLUMN_MINUTES = "minutes";
    public static final String COLUMN_NOTE = "note";

    private static final String DATABASE_NAME = "trackers.db";
    private static final int DATABASE_VERSION = 7;
    private Context context;

    // Database creation sql statement
    private static final String DATABASE_CREATE_TRACKERS =
        "CREATE TABLE " + TABLE_TRACKERS + "("
        + COLUMN_ID + " INTEGER primary key autoincrement, "
        + COLUMN_METHOD + " TEXT not null, "
        + COLUMN_NAME + " TEXT not null, "
        + COLUMN_WORKING_HOURS + " REAL not null, "
        + COLUMN_VERBOSE + " TEXT not null, "
        + COLUMN_OPERATION_STATE + " INTEGER not null);";

    private static final String DATABASE_INSERT_DEFAULT_TRACKER =
            "INSERT INTO " + TABLE_TRACKERS + " ("
                    + COLUMN_METHOD + ", "
                    + COLUMN_NAME + ", "
                    + COLUMN_WORKING_HOURS + ", "
                    + COLUMN_VERBOSE + ", "
                    + COLUMN_OPERATION_STATE + ") "
                    + "VALUES ('WLAN', 'Account 1', 8, 'Account 1', 0);";

    private static final String DATABASE_CREATE_LOGS =
        "CREATE TABLE " + TABLE_LOGS + "("
        + COLUMN_ID + " INTEGER primary key autoincrement, "
        + COLUMN_TRACKER_ID + " INTEGER REFERENCES " + TABLE_TRACKERS + "(" + COLUMN_ID + "),"
        + COLUMN_TIMESTAMP_START + " INTEGER not null, "
        + COLUMN_TIMESTAMP_END + " INTEGER not null);";

    private static final String DATABASE_CREATE_ACCESS_POINTS =
        "CREATE TABLE " + TABLE_ACCESS_POINTS + "("
        + COLUMN_ID + " INTEGER primary key autoincrement, "
        + COLUMN_TRACKER_ID + " INTEGER REFERENCES " + TABLE_TRACKERS + "(" + COLUMN_ID + "),"
        + COLUMN_SSID + " TEXT not null, "
        + COLUMN_BSSID + " TEXT not null);";

    private static final String DATABASE_CREATE_IGNORED_ACCESS_POINTS =
        "CREATE TABLE " + TABLE_IGNORED_ACCESS_POINTS + "("
        + COLUMN_TRACKER_ID + " INTEGER REFERENCES " + TABLE_TRACKERS + "(" + COLUMN_ID + "),"
        + COLUMN_SSID + " TEXT not null, "
        + COLUMN_BSSID + " TEXT not null, "
        + "UNIQUE(" + COLUMN_TRACKER_ID + ", " + COLUMN_SSID + ", " + COLUMN_BSSID +") ON CONFLICT REPLACE);";

    private static final String DATABASE_CREATE_TIME_BALANCE =
            "CREATE TABLE " + TABLE_TIME_BALANCE + "("
                    + COLUMN_ID + " INTEGER primary key autoincrement, "
                    + COLUMN_TRACKER_ID + " INTEGER REFERENCES " + TABLE_TRACKERS + "(" + COLUMN_ID + "),"
                    + COLUMN_INSERT_TIME + " INTEGER not null, "
                    + COLUMN_MINUTES + " INTEGER not null, "
                    + COLUMN_NOTE + " TEXT);";


    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_TRACKERS);
        database.execSQL(DATABASE_CREATE_LOGS);
        database.execSQL(DATABASE_CREATE_ACCESS_POINTS);
        database.execSQL(DATABASE_CREATE_IGNORED_ACCESS_POINTS);
        database.execSQL(DATABASE_CREATE_TIME_BALANCE);

        database.execSQL(DATABASE_INSERT_DEFAULT_TRACKER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SQLiteHandler.class.getName(),
              "Upgrading database from version " + oldVersion + " to "
              + newVersion + ", which will destroy all old data");

        if (oldVersion == 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE " + TABLE_TRACKERS + " ADD COLUMN " + COLUMN_VERBOSE + " TEXT DEFAULT '' NOT NULL");
            db.execSQL("UPDATE " + TABLE_TRACKERS + " SET " + COLUMN_VERBOSE + " = " + COLUMN_NAME);
        }

        if (oldVersion <= 2 && newVersion >= 3) {
            db.execSQL(DATABASE_CREATE_ACCESS_POINTS);
        }

        if (oldVersion < 4 && newVersion >= 4) {
            db.execSQL("ALTER TABLE " + TABLE_TRACKERS + " ADD COLUMN " + COLUMN_WORKING_HOURS + " REAL DEFAULT 8 NOT NULL");
        }

        if (oldVersion < 5 && newVersion >= 5) {
            db.execSQL("ALTER TABLE " + TABLE_TRACKERS + " ADD COLUMN " + COLUMN_OPERATION_STATE + " INTEGER DEFAULT 0 NOT NULL");
        }

        if (oldVersion < 6 && newVersion >= 6) {
            db.execSQL(DATABASE_CREATE_IGNORED_ACCESS_POINTS);
        }

        if (oldVersion < 7 && newVersion >= 7) {
            db.execSQL(DATABASE_CREATE_TIME_BALANCE);
            return;
        }

        // otherwise drop and re-create
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCESS_POINTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IGNORED_ACCESS_POINTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_BALANCE);
        onCreate(db);
    }

}
