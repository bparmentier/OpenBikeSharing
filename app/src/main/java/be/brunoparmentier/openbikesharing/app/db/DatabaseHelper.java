/*
 * Copyright (c) 2015 Bruno Parmentier. This file is part of OpenBikeSharing.
 *
 * OpenBikeSharing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenBikeSharing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenBikeSharing.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.brunoparmentier.openbikesharing.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper instance;

    private static final String DB_NAME = "openbikesharing.sqlite";
    private static final int DB_VERSION = 1;

    public static final String STATIONS_TABLE_NAME = "stations";
    public static final String STATIONS_COLUMN_ID = "id";
    public static final String STATIONS_COLUMN_NAME = "name";
    public static final String STATIONS_COLUMN_LAST_UPDATE = "last_update";
    public static final String STATIONS_COLUMN_LATITUDE = "latitude";
    public static final String STATIONS_COLUMN_LONGITUDE = "longitude";
    public static final String STATIONS_COLUMN_FREE_BIKES = "free_bikes";
    public static final String STATIONS_COLUMN_EMPTY_SLOTS = "empty_slots";
    public static final String STATIONS_COLUMN_ADDRESS = "address";
    public static final String STATIONS_COLUMN_BANKING = "banking";
    public static final String STATIONS_COLUMN_BONUS = "bonus";
    public static final String STATIONS_COLUMN_STATUS = "status";

    public static final String FAV_STATIONS_TABLE_NAME = "fav_stations";
    public static final String FAV_STATIONS_COLUMN_ID = "id";

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + STATIONS_TABLE_NAME
                + "(id TEXT PRIMARY KEY, name TEXT NOT NULL, last_update TEXT NOT NULL, "
                + "latitude NUMERIC NOT NULL, longitude NUMERIC NOT NULL, "
                + "free_bikes INTEGER NOT NULL, empty_slots INTEGER NOT NULL, "
                + "address TEXT, banking INTEGER, bonus INTEGER, status TEXT)"
        );
        db.execSQL("CREATE TABLE " + FAV_STATIONS_TABLE_NAME
                        + "(id TEXT PRIMARY KEY)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
