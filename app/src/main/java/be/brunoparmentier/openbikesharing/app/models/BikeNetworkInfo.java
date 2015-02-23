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
 * Information on a bike network.
 */
public class BikeNetworkInfo implements Serializable, Comparable<BikeNetworkInfo> {
    private String id;
    private String name;
    private String company;
    private BikeNetworkLocation location;

    public BikeNetworkInfo(String id, String name, String company, BikeNetworkLocation location) {
        this.id = id;
        this.name = name;
        this.company = company;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCompany() {
        return company;
    }

    public BikeNetworkLocation getLocation() {
        return location;
    }

    @Override
    public int compareTo(BikeNetworkInfo another) {
        return location.getCity().compareToIgnoreCase(another.getLocation().getCity()) > 0 ? 1 :
                (location.getCity().compareToIgnoreCase(another.getLocation().getCity()) < 0 ? -1 : 0);
    }
}
