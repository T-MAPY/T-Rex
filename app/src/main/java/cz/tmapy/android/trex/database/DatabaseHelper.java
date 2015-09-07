package cz.tmapy.android.trex.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by kasvo on 7.9.2015.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
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
    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p/>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
