package cz.tmapy.android.trex.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import cz.tmapy.android.trex.Const;

/**
 * Database manager
 * Created by kasvo on 7.9.2015.
 */
public class DatabaseManager extends SQLiteOpenHelper {

    private static final String TAG = DatabaseManager.class.getName();

    /**
     * Version of data model
     * 4 - renamed some columns
     * 3 - initial version
     */
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "trex.db3";

    static DatabaseManager instance = null;
    static SQLiteDatabase db = null;

    DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static void init(Context context) {
        if (null == instance) {
            instance = new DatabaseManager(context);
        }
    }

    public static SQLiteDatabase getDb() {
        if (null == db) {
            db = instance.getWritableDatabase();
        }
        return db;
    }

    public static void deactivate() {
        if (null != db && db.isOpen()) {
            db.close();
        }
        db = null;
        instance = null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        CreateDatabase(db);
    }

    public void CreateDatabase(SQLiteDatabase db) {
        TrackDataSource.init(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (Const.LOG_ENHANCED) Log.w(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        CreateDatabase(db);
    }
}
