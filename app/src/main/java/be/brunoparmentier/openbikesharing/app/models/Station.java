/*
 * Copyright (c) 2014-2015 Bruno Parmentier. This file is part of OpenBikeSharing.
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

package be.brunoparmentier.openbikesharing.app.models;

import java.io.Serializable;

/**
 * Represents a bike station.
 */
public class Station implements Serializable, Comparable<Station> {
    private String id;
    private String name;
    private String lastUpdate;
    private double latitude;
    private double longitude;
    private int freeBikes;
    private int emptySlots;

    private String address;
    private Boolean banking;
    private Boolean bonus;
    private StationStatus status;

    public Station(String id, String name, String lastUpdate, double latitude, double longitude, int freeBikes, int emptySlots) {
        this.id = id;
        this.name = name;
        this.lastUpdate = lastUpdate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.freeBikes = freeBikes;
        this.emptySlots = emptySlots;

        this.address = null;
        this.banking = null;
        this.bonus = null;
        this.status = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean isBanking() {
        return banking;
    }

    public Boolean isBonus() {
        return bonus;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public void setBanking(boolean banking) {
        this.banking = banking;
    }

    public void setBonus(boolean bonus) {
        this.bonus = bonus;
    }

    @Override
    public int compareTo(Station another) {
        return name.compareToIgnoreCase(another.getName()) > 0 ? 1 :
                (name.compareToIgnoreCase(another.getName()) < 0 ? -1 : 0);
    }
}
