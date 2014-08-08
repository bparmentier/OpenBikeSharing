/*
 * Copyright (c) 2014 Bruno Parmentier. This file is part of OpenBikeSharing.
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

package be.brunoparmentier.openbikesharing.app;

import java.io.Serializable;

/**
 * Represents a bike station.
 */
public class Station implements Serializable, Comparable<Station> {
    private String id;
    private String name;
    //private Date timestamp;
    private double latitude;
    private double longitude;
    private int freeBikes;
    private int emptySlots;

    public Station(String id, String name, /*Date timestamp, */double latitude, double longitude, int freeBikes, int emptySlots) {
        this.id = id;
        this.name = name;
        //this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.freeBikes = freeBikes;
        this.emptySlots = emptySlots;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    //public Date getTimestamp() { return timestamp; }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getFreeBikes() {
        return freeBikes;
    }

    public int getEmptySlots() {
        return emptySlots;
    }

    @Override
    public int compareTo(Station another) {
        return this.getName().compareToIgnoreCase(another.getName()) > 0 ? 1 :
                (this.getName().compareToIgnoreCase(another.getName()) < 0 ? -1 : 0);
    }
}
