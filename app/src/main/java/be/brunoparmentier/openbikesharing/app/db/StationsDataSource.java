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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;

import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.models.StationStatus;

public class StationsDataSource {
    private DatabaseHelper dbHelper;

    public StationsDataSource(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    public void storeStations(ArrayList<Station> stations) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            clearStations();
            for (Station station : stations) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.STATIONS_COLUMN_ID, station.getId());
                values.put(DatabaseHelper.STATIONS_COLUMN_NAME, station.getName());
                values.put(DatabaseHelper.STATIONS_COLUMN_LAST_UPDATE, station.getLastUpdate());
                values.put(DatabaseHelper.STATIONS_COLUMN_LATITUDE, String.valueOf(station.getLatitude()));
                values.put(DatabaseHelper.STATIONS_COLUMN_LONGITUDE, String.valueOf(station.getLongitude()));
                values.put(DatabaseHelper.STATIONS_COLUMN_FREE_BIKES, String.valueOf(station.getFreeBikes()));
                values.put(DatabaseHelper.STATIONS_COLUMN_EMPTY_SLOTS, String.valueOf(station.getEmptySlots()));
                if (station.getAddress() != null)
                    values.put(DatabaseHelper.STATIONS_COLUMN_ADDRESS, station.getAddress());
                if (station.isBanking() != null)
                    values.put(DatabaseHelper.STATIONS_COLUMN_BANKING, station.isBanking() ? 1 : 0);
                if (station.isBonus() != null)
                    values.put(DatabaseHelper.STATIONS_COLUMN_BONUS, station.isBonus() ? 1 : 0);
                if (station.getStatus() != null)
                    values.put(DatabaseHelper.STATIONS_COLUMN_STATUS, station.getStatus().name());

                db.insert(DatabaseHelper.STATIONS_TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearStations() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.STATIONS_TABLE_NAME, null, null);
    }

    public ArrayList<Station> getStations() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Station> stations = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id as _id, name, last_update, latitude, longitude, "
                + "free_bikes, empty_slots, address, banking, bonus, status "
                + "FROM " + DatabaseHelper.STATIONS_TABLE_NAME, null);

        try {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    Station station = toStation(cursor);
                    stations.add(station);
                    cursor.moveToNext();
                }
            }
            Collections.sort(stations);
            return stations;
        } finally {
            cursor.close();
        }
    }

    public Station getStation(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT id as _id, name, last_update, latitude, longitude, "
                + "free_bikes, empty_slots, address, banking, bonus, status "
                + "FROM " + DatabaseHelper.STATIONS_TABLE_NAME + " "
                + "WHERE id = ?", new String[] { id });
        try {
            if (cursor.moveToFirst()) {
                return toStation(cursor);
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public void addFavoriteStation(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.FAV_STATIONS_COLUMN_ID, id);
        db.insert(DatabaseHelper.FAV_STATIONS_TABLE_NAME, null, values);
    }

    public void removeFavoriteStation(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.FAV_STATIONS_TABLE_NAME, "id = ?", new String[] { id });
    }

    public ArrayList<Station> getFavoriteStations() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Station> favStations = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT sta.id as _id, name, last_update, latitude, longitude, "
                + "free_bikes, empty_slots, address, banking, bonus, status "
                + "FROM " + DatabaseHelper.FAV_STATIONS_TABLE_NAME + " sta "
                + "INNER JOIN " + DatabaseHelper.STATIONS_TABLE_NAME + " fav "
                + "ON sta.id = fav.id", null);
        try {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    Station station = toStation(cursor);
                    favStations.add(station);
                    cursor.moveToNext();
                }
            }
            Collections.sort(favStations);
            return favStations;
        } finally {
            cursor.close();
        }
    }

    public boolean isFavoriteStation(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id "
                + "FROM " + DatabaseHelper.FAV_STATIONS_TABLE_NAME + " "
                + "WHERE id = ?", new String[] { id });

        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }

    private Station toStation(Cursor cursor) {
        Station station = new Station(
                cursor.getString(0), // id
                cursor.getString(1), // name
                cursor.getString(2), // last_update
                cursor.getDouble(3), // latitude
                cursor.getDouble(4), // longitude
                cursor.getInt(5), // free_bikes
                cursor.getInt(6) // empty_slots
        );
        if (!cursor.isNull(7)) {
            station.setAddress(cursor.getString(7)); // address
        }
        if (!cursor.isNull(8)) {
            station.setBanking(cursor.getInt(8) != 0); // banking
        }
        if (!cursor.isNull(9)) {
            station.setBonus(cursor.getInt(9) != 0); // bonus
        }
        if (!cursor.isNull(10)) {
            station.setStatus(StationStatus.valueOf(cursor.getString(10))); // status
        }

        return station;
    }
}
