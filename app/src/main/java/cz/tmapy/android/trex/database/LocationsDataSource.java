package cz.tmapy.android.trex.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cz.tmapy.android.trex.database.dobs.LocationDob;

/**
 * Created by kasvo on 8.9.2015.
 */
public class LocationsDataSource {

    private static final String TABLE_NAME = "locations";
    private static final String COL_ID = "_id"; //The database tables should use the identifier _id for the primary key of the table. Several Android functions rely on this standard.
    private static final String COL_GPS_TIME = "gpsTime";
    private static final String COL_LAT = "lat";
    private static final String COL_LON = "lon";
    private static final String COL_ALT = "alt";
    private static final String COL_SPEED = "speed";
    private static final String COL_BEARING = "bearing";
    private static final String COL_SERVER_RESP = "server_resp";
    private static final String COL_UPDATE_TIME = "update_time";
    private static final String IDX_ID = "id_idx";

    public static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

    public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            COL_GPS_TIME + " INTEGER, " +
            COL_LAT + " REAL, " +
            COL_LON + " REAL, " +
            COL_ALT + " REAL, " +
            COL_SPEED + " REAL, " +
            COL_BEARING + " REAL, " +
            COL_SERVER_RESP + " TEXT, " +
            COL_UPDATE_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";

    public static final String CREATE_INDEX = "CREATE INDEX " + IDX_ID + " ON " + TABLE_NAME + " (" + COL_ID + ");";

    // Database fields
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String[] allColumns = {COL_ID, COL_GPS_TIME, COL_LAT, COL_LON, COL_ALT, COL_SPEED, COL_BEARING, COL_SERVER_RESP, COL_UPDATE_TIME};

    /**
     * Static method to create table
     * @param db
     */
    public static void CreateTable (SQLiteDatabase db)
    {
        db.execSQL(DROP_TABLE_SQL);
        db.execSQL(CREATE_TABLE_SQL);
        db.execSQL(CREATE_INDEX);
    }

    public LocationsDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Long createLocation(LocationDob loc) {
        ContentValues values = new ContentValues();
        values.put(COL_GPS_TIME, loc.getGpsTime());
        values.put(COL_LAT, loc.getLat());
        values.put(COL_LON, loc.getLon());
        values.put(COL_ALT, loc.getAlt());
        values.put(COL_SPEED, loc.getSpeed());
        values.put(COL_BEARING, loc.getBearing());
        values.put(COL_SERVER_RESP, loc.getServerResponse());

        long insertId = database.insert(TABLE_NAME, null, values);
        return insertId;
    }

    public LocationDob getLast()
    {
        Cursor cursor = database.query(TABLE_NAME, allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        LocationDob loc = cursorToLocation(cursor);

        // make sure to close the cursor
        cursor.close();
        return loc;
    }

    public List<LocationDob> getAllLocations() {
        List<LocationDob> locations = new ArrayList<LocationDob>();

        Cursor cursor = database.query(TABLE_NAME, allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LocationDob loc = cursorToLocation(cursor);
            locations.add(loc);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return locations;
    }

    private LocationDob cursorToLocation(Cursor cursor) {
        LocationDob loc = new LocationDob();
        loc.setId(cursor.getLong(0));
        loc.setGpsTime(cursor.getLong(1));
        loc.setLat(cursor.getDouble(2));
        loc.setLon(cursor.getDouble(3));
        loc.setAlt(cursor.getDouble(4));
        loc.setSpeed(cursor.getFloat(5));
        loc.setBearing(cursor.getFloat(6));
        loc.setServerResponse(cursor.getString(7));
        loc.setUpdateTime(cursor.getLong(8));
        return loc;
    }
}
