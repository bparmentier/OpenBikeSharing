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

package be.brunoparmentier.openbikesharing.app.parsers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.models.BikeNetworkInfo;
import be.brunoparmentier.openbikesharing.app.models.BikeNetworkLocation;

/**
 * Parse the list of bike networks.
 */
public class BikeNetworksListParser {
    private ArrayList<BikeNetworkInfo> bikeNetworks;

    public BikeNetworksListParser(String toParse) throws ParseException {
        bikeNetworks = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(toParse);
            JSONArray rawNetworks = jsonObject.getJSONArray("networks");
            for (int i = 0; i < rawNetworks.length(); i++) {
                JSONObject rawNetwork = rawNetworks.getJSONObject(i);

                String id = rawNetwork.getString("id");
                String name = rawNetwork.getString("name");
                String company = rawNetwork.getString("company");
                BikeNetworkLocation location;

                /* network location */
                {
                    JSONObject rawLocation = rawNetwork.getJSONObject("location");

                    double latitude = rawLocation.getDouble("latitude");
                    double longitude = rawLocation.getDouble("longitude");
                    String city = rawLocation.getString("city");
                    String country = rawLocation.getString("country");

                    location = new BikeNetworkLocation(latitude, longitude, city, country);

                }
                bikeNetworks.add(new BikeNetworkInfo(id, name, company, location));
            }
        } catch (JSONException e) {
            throw new ParseException("Error parsing JSON", 0);
        }
    }

    public ArrayList<BikeNetworkInfo> getNetworks() {
        return bikeNetworks;
    }
}
