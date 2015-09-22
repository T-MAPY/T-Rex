package cz.tmapy.android.trex.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.database.dobs.TrackDob;

/**
 * Created by kasvo on 15.9.2015.
 */
public class TrackDataSource {
    public static final String TABLE_NAME = "tracks";
    public static final String COL_ID = "_id"; //The database tables should use the identifier _id for the primary key of the table. Several Android functions rely on this standard.
    public static final String COL_START_TIME = "start_time";
    public static final String COL_FIRST_LAT = "first_lat";
    public static final String COL_FIRST_LON = "first_lon";
    public static final String COL_FIRST_ADDRESS = "first_address";
    public static final String COL_FINISH_TIME = "finish_time";
    public static final String COL_LAST_LAT = "last_lat";
    public static final String COL_LAST_LON = "last_lon";
    public static final String COL_LAST_ADDRESS = "last_address";
    public static final String COL_DISTANCE = "distance";
    public static final String COL_MAX_SPEED = "max_speed";
    public static final String COL_AVE_SPEED = "ave_speed";
    public static final String COL_MIN_ALT = "min_alt";
    public static final String COL_MAX_ALT = "max_alt";
    public static final String COL_ELEV_DIFF_UP = "elev_diff_up";
    public static final String COL_ELEV_DIFF_DOWN = "elev_diff_down";
    public static final String COL_NOTE = "note";
    public static final String COL_UPDATE_TIME = "update_time";
    public static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
            COL_START_TIME + " INTEGER," +
            COL_FIRST_LAT + " REAL," +
            COL_FIRST_LON + " REAL," +
            COL_FIRST_ADDRESS + " TEXT," +
            COL_FINISH_TIME + " INTEGER," +
            COL_LAST_LAT + " REAL," +
            COL_LAST_LON + " REAL," +
            COL_LAST_ADDRESS + " TEXT," +
            COL_DISTANCE + " COL_DISTANCE," +
            COL_MAX_SPEED + " REAL," +
            COL_AVE_SPEED + " REAL," +
            COL_MIN_ALT + " REAL," +
            COL_MAX_ALT + " REAL," +
            COL_ELEV_DIFF_UP + " REAL," +
            COL_ELEV_DIFF_DOWN + " REAL," +
            COL_NOTE + " TEXT," +
            COL_UPDATE_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
    private static final String TAG = LocationsDataSource.class.getName();
    private static final String IDX_ID = "id_idx";
    public static final String CREATE_INDEX = "CREATE INDEX " + IDX_ID + " ON " + TABLE_NAME + " (" + COL_ID + ");";

    // Database fields
    private DatabaseManager dbMan;
    private String[] allColumns = {COL_ID, COL_START_TIME, COL_FIRST_LAT, COL_FIRST_LON, COL_FIRST_ADDRESS, COL_FINISH_TIME, COL_LAST_LAT, COL_LAST_LON, COL_LAST_ADDRESS, COL_DISTANCE, COL_MAX_SPEED, COL_AVE_SPEED, COL_MAX_ALT, COL_MIN_ALT, COL_ELEV_DIFF_UP, COL_ELEV_DIFF_DOWN, COL_NOTE, COL_UPDATE_TIME};

    /**
     * Static method to create table
     *
     * @param db
     */
    public static void init(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE_SQL);
        db.execSQL(CREATE_TABLE_SQL);
        db.execSQL(CREATE_INDEX);
    }

    /**
     * Complete table erase
     *
     * @throws SQLException
     */
    public void EraseTable() {
        init(DatabaseManager.getDb()); //sqlite doesn't have TRUNCATE - drop is recomended
    }

    public Long saveTrack(TrackDob trackDob) {
        long insertId = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COL_START_TIME, trackDob.getStartTime());
            values.put(COL_FIRST_LAT, trackDob.getFirstLat());
            values.put(COL_FIRST_LON, trackDob.getFirstLon());
            values.put(COL_FIRST_ADDRESS, trackDob.getFirstAddress());
            values.put(COL_FINISH_TIME, trackDob.getFinishTime());
            values.put(COL_LAST_LAT, trackDob.getLastLat());
            values.put(COL_LAST_LON, trackDob.getLastLon());
            values.put(COL_LAST_ADDRESS, trackDob.getLastAddress());
            values.put(COL_DISTANCE, trackDob.getDistance());
            values.put(COL_MAX_SPEED, trackDob.getMaxSpeed());
            values.put(COL_AVE_SPEED, trackDob.getAveSpeed());
            values.put(COL_MAX_ALT, trackDob.getMaxAlt());
            values.put(COL_MIN_ALT, trackDob.getMinAlt());
            values.put(COL_ELEV_DIFF_UP, trackDob.getElevDiffUp());
            values.put(COL_ELEV_DIFF_DOWN, trackDob.getElevDiffDown());
            values.put(COL_NOTE, trackDob.getNote());
            insertId = DatabaseManager.getDb().insert(TABLE_NAME, null, values);
            if (Const.LOG_ENHANCED) Log.i(TAG, "Track successfully saved with id = " + insertId);
        } catch (Exception e) {
            Log.e(TAG, "Cannot save track", e);
        }

        return insertId;
    }

    public void deleteTrack(Long value) {
        try {
            DatabaseManager.getDb().delete(TABLE_NAME, COL_ID + "=" + value, null);
        } catch (Exception e) {
            Log.e(TAG, "Can't delete row", e);
        }

    }

    public Cursor getAllTracksCursor() {
        return DatabaseManager.getDb().query(TABLE_NAME, allColumns, null, null, null, null, COL_ID + " DESC");
    }

    public ArrayList<TrackDob> getAllTracks() {
        ArrayList<TrackDob> locations = new ArrayList<TrackDob>();
        try {
            Cursor cursor = DatabaseManager.getDb().query(TABLE_NAME, allColumns, null, null, null, null, COL_ID + " DESC");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TrackDob loc = cursorToLocation(cursor);
                locations.add(loc);
                cursor.moveToNext();
            }
            // make sure to close the cursor
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Cannot read all tracks", e);
        }
        return locations;
    }

    /**
     * Delete track, keep only last N number
     *
     * @param keepOnlyTracks
     */
    public Boolean keepOnlyLastTracks(Integer keepOnlyTracks) {
        return DatabaseManager.getDb().delete(TABLE_NAME, COL_ID +
                " NOT IN (SELECT " + COL_ID + " FROM " + TABLE_NAME + " ORDER BY " + COL_ID + " DESC LIMIT ?)", new String[]{String.valueOf(keepOnlyTracks)}) > 0;
    }

    private TrackDob cursorToLocation(Cursor cursor) {
        TrackDob trackDob = new TrackDob();
        trackDob.setId(cursor.getLong(0));
        trackDob.setStartTime(cursor.getLong(1));
        trackDob.setFirstLat(cursor.getDouble(2));
        trackDob.setFirstLon(cursor.getDouble(3));
        trackDob.setFirstAddress(cursor.getString(4));
        trackDob.setFinishTime(cursor.getLong(5));
        trackDob.setLastLat(cursor.getDouble(6));
        trackDob.setLastLon(cursor.getDouble(7));
        trackDob.setLastAddress(cursor.getString(8));
        trackDob.setDistance(cursor.getFloat(9));
        trackDob.setMaxSpeed(cursor.getFloat(10));
        trackDob.setAveSpeed(cursor.getFloat(11));
        trackDob.setMinAlt(cursor.getDouble(12));
        trackDob.setMaxAlt(cursor.getDouble(13));
        trackDob.setElevDiffUp(cursor.getDouble(14));
        trackDob.setElevDiffDown(cursor.getDouble(15));
        trackDob.setNote(cursor.getString(16));
        trackDob.setUpdateTime(cursor.getLong(17));
        return trackDob;
    }

}
