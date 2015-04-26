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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.models.BikeNetwork;
import be.brunoparmentier.openbikesharing.app.models.BikeNetworkLocation;
import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.models.StationStatus;

/**
 * Parse information on a bike network.
 */
public class BikeNetworkParser {
    private BikeNetwork bikeNetwork;

    public BikeNetworkParser(String toParse, boolean stripIdFromStationName) throws ParseException {
        ArrayList<Station> stations = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(toParse);
            JSONObject rawNetwork = jsonObject.getJSONObject("network");

            /* network name & id */
            String networkId = rawNetwork.getString("id");
            String networkName = rawNetwork.getString("name");
            String networkCompany = rawNetwork.getString("company");

            /* network location */
            BikeNetworkLocation networkLocation;
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
                    if (stripIdFromStationName) name = name.replaceAll("^[0-9 ]*- *", "");
                    String lastUpdate = rawStation.getString("timestamp");
                    double latitude = rawStation.getDouble("latitude");
                    double longitude = rawStation.getDouble("longitude");
                    int freeBikes = rawStation.getInt("free_bikes");
                    int emptySlots;
                    if (!rawStation.isNull("empty_slots")) {
                        emptySlots = rawStation.getInt("empty_slots");
                    } else {
                        emptySlots = -1;
                    }

                    Station station = new Station(id, name, lastUpdate, latitude, longitude,
                            freeBikes, emptySlots);

                    /* extra info */
                    if (rawStation.has("extra")) {
                        JSONObject rawExtra = rawStation.getJSONObject("extra");

                        /* address */
                        if (rawExtra.has("address")) {
                            station.setAddress(rawExtra.getString("address"));
                        } else if (rawExtra.has("description")) {
                            station.setAddress(rawExtra.getString("description"));
                        }

                        /* banking */
                        if (rawExtra.has("banking")) { // JCDecaux
                            station.setBanking(rawExtra.getBoolean("banking"));
                        } else if (rawExtra.has("payment")) {
                            if (rawExtra.getString("payment").equals("AVEC_TPE")) { // vlille
                                station.setBanking(true);
                            } else {
                                station.setBanking(false);
                            }
                        } else if (rawExtra.has("ticket")) { // dublinbikes, citycycle
                            station.setBanking(rawExtra.getBoolean("ticket"));
                        }

                        /* bonus */
                        if (rawExtra.has("bonus")) {
                            station.setBonus(rawExtra.getBoolean("bonus"));
                        }

                        /* status */
                        if (rawExtra.has("status")) {
                            String status = rawExtra.getString("status");
                            if (status.equals("CLOSED") // villo
                                    || status.equals("CLS") // ClearChannel
                                    || status.equals("1") // vlille
                                    || status.equals("offline")) { // idecycle
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("statusValue")) {
                            if (rawExtra.getString("statusValue").equals("Not In Service")) { // Bike Share
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("locked")) {
                            if (rawExtra.getBoolean("locked")) { // bixi
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("open")) {
                            if (!rawExtra.getBoolean("open")) { // dublinbikes, citycycle
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        }
                    }
                    stations.add(station);
                }
            }

            bikeNetwork = new BikeNetwork(networkId, networkName, networkCompany, networkLocation, stations);
        } catch (JSONException e) {
            throw new ParseException("Error parsing JSON", 0);
        }
    }

    public BikeNetwork getNetwork() {
        return bikeNetwork;
    }

}
