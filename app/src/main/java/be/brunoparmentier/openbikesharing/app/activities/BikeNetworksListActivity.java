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

package be.brunoparmentier.openbikesharing.app.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.adapters.BikeNetworksListAdapter;
import be.brunoparmentier.openbikesharing.app.models.BikeNetworkInfo;
import be.brunoparmentier.openbikesharing.app.parsers.BikeNetworksListParser;

public class BikeNetworksListActivity extends Activity {
    private static final String TAG = BikeNetworksListActivity.class.getSimpleName();

    private static final String DEFAULT_API_URL = "http://api.citybik.es/v2/";
    private static final String PREF_KEY_API_URL = "pref_api_url";
    private static final String PREF_KEY_NETWORK_ID = "network-id";
    private static final String PREF_KEY_NETWORK_NAME = "network-name";
    private static final String PREF_KEY_NETWORK_CITY = "network-city";
    private static final String PREF_KEY_NETWORK_LATITUDE = "network-latitude";
    private static final String PREF_KEY_NETWORK_LONGITUDE = "network-longitude";
    private static final String KEY_NETWORK_ID = "network-id";

    private ListView listView;
    private ArrayList<BikeNetworkInfo> bikeNetworks;
    private ArrayList<BikeNetworkInfo> searchedBikeNetworks;
    private BikeNetworksListAdapter bikeNetworksListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_networks_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        listView = (ListView) findViewById(R.id.networksListView);
        String apiUrl = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(PREF_KEY_API_URL, DEFAULT_API_URL) + "networks";
        new JSONDownloadTask().execute(apiUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bike_networks_list, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            // TODO: avoid redundancy
            public boolean onQueryTextChange(String s) {
                if (bikeNetworks == null) {
                    return false;
                }
                searchedBikeNetworks = new ArrayList<>();
                for (BikeNetworkInfo network : bikeNetworks) {
                    if (network.getName().toLowerCase().contains(s.toLowerCase())
                            || network.getLocation().getCity().toLowerCase().contains(s.toLowerCase())) {
                        searchedBikeNetworks.add(network);
                    }
                }
                bikeNetworksListAdapter = new BikeNetworksListAdapter(BikeNetworksListActivity.this,
                        android.R.layout.simple_expandable_list_item_2,
                        android.R.id.text1,
                        searchedBikeNetworks);
                listView.setAdapter(bikeNetworksListAdapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(BikeNetworksListActivity.this);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_KEY_NETWORK_ID, searchedBikeNetworks.get(position).getId());
                        editor.putString(PREF_KEY_NETWORK_NAME, searchedBikeNetworks.get(position).getName());
                        editor.putString(PREF_KEY_NETWORK_CITY, searchedBikeNetworks.get(position).getLocation().getCity());
                        editor.putLong(PREF_KEY_NETWORK_LATITUDE, Double.doubleToRawLongBits(
                                        searchedBikeNetworks.get(position).getLocation().getLatitude())
                        );
                        editor.putLong(PREF_KEY_NETWORK_LONGITUDE, Double.doubleToRawLongBits(
                                        searchedBikeNetworks.get(position).getLocation().getLongitude())
                        );
                        editor.apply();
                        Toast.makeText(BikeNetworksListActivity.this,
                                searchedBikeNetworks.get(position).getName()
                                        + " ("
                                        + searchedBikeNetworks.get(position).getLocation().getCity()
                                        + ") " + getString(R.string.network_selected),
                                Toast.LENGTH_SHORT).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(KEY_NETWORK_ID, searchedBikeNetworks.get(position).getId());
                        if (getParent() == null) {
                            setResult(Activity.RESULT_OK, resultIntent);
                        } else {
                            getParent().setResult(Activity.RESULT_OK, resultIntent);
                        }
                        finish();
                    }
                });
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class JSONDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                StringBuilder response = new StringBuilder();

                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String strLine;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }
                return response.toString();
            } catch (IOException e) {
                return getString(R.string.connection_error);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            try {
                /* parse result */
                BikeNetworksListParser bikeNetworksListParser = new BikeNetworksListParser(result);
                bikeNetworks = bikeNetworksListParser.getNetworks();
                Collections.sort(bikeNetworks);

                bikeNetworksListAdapter = new BikeNetworksListAdapter(BikeNetworksListActivity.this,
                        android.R.layout.simple_expandable_list_item_2,
                        android.R.id.text1,
                        bikeNetworks);

                listView.setAdapter(bikeNetworksListAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(BikeNetworksListActivity.this);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_KEY_NETWORK_ID, bikeNetworks.get(position).getId());
                        editor.putString(PREF_KEY_NETWORK_NAME, bikeNetworks.get(position).getName());
                        editor.putString(PREF_KEY_NETWORK_CITY, bikeNetworks.get(position).getLocation().getCity());
                        editor.putLong(PREF_KEY_NETWORK_LATITUDE, Double.doubleToRawLongBits(
                                        bikeNetworks.get(position).getLocation().getLatitude())
                        );
                        editor.putLong(PREF_KEY_NETWORK_LONGITUDE, Double.doubleToRawLongBits(
                                        bikeNetworks.get(position).getLocation().getLongitude())
                        );
                        editor.apply();
                        Toast.makeText(BikeNetworksListActivity.this,
                                bikeNetworks.get(position).getName()
                                        + " ("
                                        + bikeNetworks.get(position).getLocation().getCity()
                                        + ") " + getString(R.string.network_selected),
                                Toast.LENGTH_SHORT).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(KEY_NETWORK_ID, bikeNetworks.get(position).getId());
                        if (getParent() == null) {
                            setResult(Activity.RESULT_OK, resultIntent);
                        } else {
                            getParent().setResult(Activity.RESULT_OK, resultIntent);
                        }
                        finish();
                    }
                });
            } catch (ParseException e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(BikeNetworksListActivity.this,
                        R.string.json_error, Toast.LENGTH_LONG).show();
            }
        }
    }
}
