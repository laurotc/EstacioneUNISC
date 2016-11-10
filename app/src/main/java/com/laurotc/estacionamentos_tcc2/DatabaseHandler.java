package com.laurotc.estacionamentos_tcc2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by laurotc on 9/25/16.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    // For tagging messages in Logs.
    private static final String TAG = "DatabaseHandler";

    private static final int DATABASE_VERSION = 1;
    //private static final String DATABASE_NAME = "estacioneUniscDatabase";
    private static final String DATABASE_NAME = "parkAroundDatabase";

    /* Table Device Data information*/
    private static final String TABLE_DATA = "deviceData";
    public static final String COLUMN_DATA_ID = "dataId";
    public static final String COLUMN_DATA_LATITUDE = "latitude";
    public static final String COLUMN_DATA_LONGITUDE = "longitude";
    public static final String COLUMN_DATA_STATUS = "status";

    /**
     * Default constructor.
     * @param context
     */
    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //**********
    // Creating tables in this app's database.
    //**********
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        Log.v(TAG, "###############################");
        Log.v(TAG, "## onCreate: creating database.");
        Log.v(TAG, "###############################");

        // The Device Data table.
        String CREATE_DATA_TABLE = "CREATE TABLE " +
                TABLE_DATA + "("
                + COLUMN_DATA_ID + " INT DEFAULT 1,"
                + COLUMN_DATA_LATITUDE + " FLOAT(10,6) DEFAULT null,"
                + COLUMN_DATA_LONGITUDE + " FLOAT(10,6) DEFAULT null,"
                + COLUMN_DATA_STATUS + " INTEGER DEFAULT -1)";

        // All tables are created when database is initialized.
        db.execSQL(CREATE_DATA_TABLE);

        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA_ID, "1");
        values.put(COLUMN_DATA_LATITUDE, "");
        values.put(COLUMN_DATA_LONGITUDE, "");
        values.put(COLUMN_DATA_STATUS, -1);
        db.insert(TABLE_DATA, null, values);
    }

    //*************************************************************************
    // The onUpgrade() method is called when the handler is invoked with a
    // greater database version number from the one previously used.
    // It simply removes the old database and create a new one.
    //*************************************************************************
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Delete all tables.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);

        // Create fresh tables.
        onCreate(db);
    }


    /**
     * Updates the information in the table Data.
     *
     * @param deviceInfo Info about the device (location and status)
     * @return true if update was OK
     *
     * Colocar os dados de posição em um array e enviar ele para salvar os dados
     */
    public boolean updateTableData(DeviceInfo deviceInfo)
    {
        // Return value.
        boolean retVal = false;

        if (deviceInfo != null) {
            Log.v(TAG, "### Update ###");
            // Get a reference of our database.
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            // Device data type and content.
            values.put(COLUMN_DATA_LATITUDE, deviceInfo.getLatitude());
            values.put(COLUMN_DATA_LONGITUDE, deviceInfo.getLongitude());
            values.put(COLUMN_DATA_STATUS, deviceInfo.getStatus());


            // Update the Device Data table. If update fails, return false.
            int ret = db.update(TABLE_DATA, values, COLUMN_DATA_ID + "=?", new String[] {"1"});
            db.close();

            Log.v(TAG, "Estacionado: " + Integer.toString(deviceInfo.getStatus()));
            retVal = ret >= 0; //If < 0, returns false, else true
        }

        return retVal;
    }


    /**
     * Get the information in the table Data.
     *
     * @return ArrayList with the data from the table
     */
    public DeviceInfo getTableData()
    {
        // Get a reference of our database.
        SQLiteDatabase db = this.getWritableDatabase();
        DeviceInfo deviceInfo = new DeviceInfo();

        // Query to select the data
        String query = "SELECT * FROM " + TABLE_DATA;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            deviceInfo.setLatitude(cursor.getDouble(1));
            deviceInfo.setLongitude(cursor.getDouble(2));
            deviceInfo.setStatus(cursor.getInt(3));
        }

        // Close out the database and cursor.
        cursor.close();
        db.close();

        return deviceInfo;
    }
}
