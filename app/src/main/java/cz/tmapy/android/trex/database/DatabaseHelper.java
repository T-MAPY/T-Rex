package cz.tmapy.android.trex.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.database.dobs.TrackDob;

/**
 * Called by system to initialize database
 * Created by kasvo on 7.9.2015.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getName();

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "trex";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        CreateDatabase(db);
    }

    public void CreateDatabase(SQLiteDatabase db) {
        TrackDataSource.InitTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (Const.LOG_ENHANCED) Log.w(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        CreateDatabase(db);
    }
}
