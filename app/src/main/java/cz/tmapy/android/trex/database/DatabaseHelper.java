package cz.tmapy.android.trex.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by kasvo on 7.9.2015.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getName();

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "trex";
    private static final String LOCATIONS_TABLE_SQL = "DROP TABLE IF EXISTS locations; CREATE TABLE locations("+
                                                        "id BIGINT PRIMARY KEY NOT NULL, "+
                                                        "time DATETIME, "+
                                                        "lat DOUBLE, "+
                                                        "lon DOUBLE, "+
                                                        "alt DOUBLE, "+
                                                        "speed FLOAT, "+
                                                        "bearing FLOAT, "+
                                                        "server_resp TEXT, "+
                                                        "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);" +
                                                        "DROP INDEX IF EXISTS id_idx; CREATE INDEX id_idx ON WORD (id);";

    private SQLiteDatabase mDb;

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        mDb = db;
    }

    public void CreateDatabase()
    {
        mDb.execSQL(LOCATIONS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS locations;");
        CreateDatabase();
    }
}
