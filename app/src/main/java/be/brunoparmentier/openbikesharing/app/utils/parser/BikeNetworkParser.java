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

package be.brunoparmentier.openbikesharing.app.utils.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.BikeNetworkLocation;
import be.brunoparmentier.openbikesharing.app.Station;
import be.brunoparmentier.openbikesharing.app.utils.OBSException;

/**
 * Parse information on a bike network.
 */
public class BikeNetworkParser {
    private String networkId;
    private String networkName;
    private String networkCompany;
    private BikeNetworkLocation networkLocation;
    private ArrayList<Station> stations;

    private BikeNetwork bikeNetwork;

    public BikeNetworkParser(JSONObject jsonObject) throws OBSException {

        stations = new ArrayList<Station>();

        try {
            JSONObject rawNetwork = jsonObject.getJSONObject("network");

            /* network name & id */
            networkId = rawNetwork.getString("id");
            networkName = rawNetwork.getString("name");
            networkCompany = rawNetwork.getString("company");

            /* network location */
            {
                JSONObject rawLocation = rawNetwork.getJSONObject("location");

                double latitude = rawLocation.getDouble("latitude");
                double longitude = rawLocation.getDouble("longitude");
                String city = rawLocation.getString("city");
                String country = rawLocation.getString("country");

                networkLocation = new BikeNetworkLocation(latitude, longitude, city, country);
            }

            /* stations list */
            {
                JSONArray rawStations = rawNetwork.getJSONArray("stations");

                for (int i = 0; i < rawStations.length(); i++) {
                    JSONObject rawStation = rawStations.getJSONObject(i);

                    String id = rawStation.getString("id");
                    String name = rawStation.getString("name");
                    //Date timestamp = new Date(rawStation.getString("timestamp"));
                    double latitude = rawStation.getDouble("latitude");
                    double longitude = rawStation.getDouble("longitude");
                    int freeBikes = rawStation.getInt("free_bikes");
                    int emptySlots = rawStation.getInt("empty_slots");

                    stations.add(new Station(id, name, /*timestamp,*/ latitude, longitude, freeBikes, emptySlots));
                }
            }

            bikeNetwork = new BikeNetwork(networkId, networkName, networkCompany, networkLocation, stations);
        } catch (JSONException e) {
            throw new OBSException("Invalid JSON object");
        }
    }

    public BikeNetwork getNetwork() {
        return bikeNetwork;
    }

}
