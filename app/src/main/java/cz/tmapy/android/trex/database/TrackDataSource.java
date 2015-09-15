package cz.tmapy.android.trex.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.database.dobs.TrackDob;

/**
 * Created by kasvo on 15.9.2015.
 */
public class TrackDataSource {
    private static final String TAG = LocationsDataSource.class.getName();

    private static final String TABLE_NAME = "tracks";
    private static final String COL_ID = "_id"; //The database tables should use the identifier _id for the primary key of the table. Several Android functions rely on this standard.
    private static final String START_TIME = "start_time";
    private static final String START_LAT = "start_lat";
    private static final String START_LON = "start_lon";
    private static final String START_ADDRESS = "start_address";
    private static final String FINISH_TIME = "finish_time";
    private static final String FINISH_LAT = "finish_lat";
    private static final String FINISH_LON = "finish_lon";
    private static final String FINISH_ADDRESS = "finish_address";
    private static final String DISTANCE = "distance";
    private static final String MAX_SPEED = "max_speed";
    private static final String AVE_SPEED = "ave_speed";
    private static final String MIN_ALT = "min_alt";
    private static final String MAX_ALT = "max_alt";
    private static final String ELEV_DIFF_UP = "elev_diff_up";
    private static final String ELEV_DIFF_DOWN = "elev_diff_down";
    private static final String NOTE = "note";
    private static final String COL_UPDATE_TIME = "update_time";
    private static final String IDX_ID = "id_idx";

    public static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

    public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
            START_TIME + " INTEGER," +
            START_LAT + " REAL," +
            START_LON + " REAL," +
            START_ADDRESS + " TEXT," +
            FINISH_TIME + " INTEGER," +
            FINISH_LAT + " REAL," +
            FINISH_LON + " REAL," +
            FINISH_ADDRESS + " TEXT," +
            DISTANCE + " DISTANCE," +
            MAX_SPEED + " REAL," +
            AVE_SPEED + " REAL," +
            MIN_ALT + " REAL," +
            MAX_ALT + " REAL," +
            ELEV_DIFF_UP + " REAL," +
            ELEV_DIFF_DOWN + " REAL," +
            NOTE + " TEXT," +
            COL_UPDATE_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";

    public static final String CREATE_INDEX = "CREATE INDEX " + IDX_ID + " ON " + TABLE_NAME + " (" + COL_ID + ");";

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = {COL_ID, START_TIME, START_LAT, START_LON, START_ADDRESS, FINISH_TIME, FINISH_LAT, FINISH_LON, FINISH_ADDRESS, DISTANCE, MAX_SPEED, AVE_SPEED, MIN_ALT, MAX_ALT, ELEV_DIFF_UP, ELEV_DIFF_DOWN, NOTE, COL_UPDATE_TIME};

    /**
     * Static method to create table
     *
     * @param db
     */
    public static void InitTable(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE_SQL);
        db.execSQL(CREATE_TABLE_SQL);
        db.execSQL(CREATE_INDEX);
    }

    public TrackDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * Complete table erase
     *
     * @throws SQLException
     */
    public void EraseTable() {
        try {
            open();
            InitTable(database); //sqlite doesn't have TRUNCATE - drop is recomended
            close();
        } catch (Exception e) {
            Log.e(TAG, "Cannot erase table", e);
        }
    }

    public Long saveTrack(TrackDob trackDob) {
        long insertId = -1;
        try {
            open();
            ContentValues values = new ContentValues();
            values.put(START_TIME, trackDob.getStartTime());
            values.put(START_LAT, trackDob.getStartLat());
            values.put(START_LON, trackDob.getStartLon());
            values.put(START_ADDRESS, trackDob.getStartAddress());
            values.put(FINISH_TIME, trackDob.getFinishTime());
            values.put(FINISH_LAT, trackDob.getFinishLat());
            values.put(FINISH_LON, trackDob.getFinishLon());
            values.put(FINISH_ADDRESS, trackDob.getFinishAddress());
            values.put(DISTANCE, trackDob.getDistance());
            values.put(MAX_SPEED, trackDob.getMaxSpeed());
            values.put(AVE_SPEED, trackDob.getAveSpeed());
            values.put(MIN_ALT, trackDob.getMinAlt());
            values.put(MAX_ALT, trackDob.getMaxAlt());
            values.put(ELEV_DIFF_UP, trackDob.getElevDiffUp());
            values.put(ELEV_DIFF_DOWN, trackDob.getElevDiffDown());
            values.put(NOTE, trackDob.getNote());
            insertId = database.insert(TABLE_NAME, null, values);
            if (Const.LOG_ENHANCED) Log.i(TAG,"Track successfully saved with id = " + insertId);
            close();
        } catch (Exception e) {
            Log.e(TAG, "Cannot save track", e);
        }

        return insertId;
    }

    public List<TrackDob> getAllLocations() {
        List<TrackDob> locations = new ArrayList<TrackDob>();
        try {
            open();
            Cursor cursor = database.query(TABLE_NAME, allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TrackDob loc = cursorToLocation(cursor);
                locations.add(loc);
                cursor.moveToNext();
            }
            // make sure to close the cursor
            cursor.close();
            close();
        } catch (Exception e) {
            Log.e(TAG, "Cannot read all tracks", e);
        }
        return locations;
    }

    private TrackDob cursorToLocation(Cursor cursor) {
        TrackDob trackDob = new TrackDob();
        trackDob.setId(cursor.getLong(0));
        trackDob.setStartTime(cursor.getLong(1));
        trackDob.setStartLat(cursor.getDouble(2));
        trackDob.setStartLon(cursor.getDouble(3));
        trackDob.setStartAddress(cursor.getString(4));
        trackDob.setFinishTime(cursor.getLong(5));
        trackDob.setFinishLat(cursor.getDouble(6));
        trackDob.setFinishLon(cursor.getDouble(7));
        trackDob.setFinishAddress(cursor.getString(8));
        trackDob.setDistance(cursor.getFloat(9));
        trackDob.setMaxSpeed(cursor.getFloat(10));
        trackDob.setAveSpeed(cursor.getFloat(11));
        trackDob.setMinAlt(cursor.getDouble(12));
        trackDob.setMaxAlt(cursor.getDouble(13));
        trackDob.setElevDiffUp(cursor.getFloat(14));
        trackDob.setElevDiffDown(cursor.getFloat(15));
        trackDob.setNote(cursor.getString(16));
        trackDob.setUpdateTime(cursor.getLong(17));
        return trackDob;
    }
}
