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

package be.brunoparmentier.openbikesharing.app.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import be.brunoparmentier.openbikesharing.app.BikeNetworkInfo;
import be.brunoparmentier.openbikesharing.app.BikeNetworksListAdapter;
import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.utils.OBSException;
import be.brunoparmentier.openbikesharing.app.utils.parser.BikeNetworksListParser;

public class BikeNetworksListActivity extends Activity {

    private final String BASE_URL = "http://api.citybik.es/v2/networks";
    private final String NETWORK_ID_LABEL = "network-id";
    private ListView listView;
    private ArrayList<BikeNetworkInfo> bikeNetworks;
    private ArrayList<BikeNetworkInfo> searchedBikeNetworks;
    private BikeNetworksListAdapter bikeNetworksListAdapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_networks_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        listView = (ListView) findViewById(R.id.networksListView);
        new JSONDownloadTask().execute(BASE_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bike_networks_list, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
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
                        editor.putString(NETWORK_ID_LABEL, searchedBikeNetworks.get(position).getId());
                        editor.putBoolean("first-run", false);
                        editor.commit();
                        Toast.makeText(BikeNetworksListActivity.this,
                                searchedBikeNetworks.get(position).getId() + " " + getString(R.string.network_selected),
                                Toast.LENGTH_SHORT).show();

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
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);

                /* parse result */
                BikeNetworksListParser bikeNetworksListParser = new BikeNetworksListParser(jsonObject);
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
                        editor.putString(NETWORK_ID_LABEL, bikeNetworks.get(position).getId());
                        editor.putBoolean("first-run", false);
                        editor.commit();
                        Toast.makeText(BikeNetworksListActivity.this,
                                bikeNetworks.get(position).getId() + " " + getString(R.string.network_selected),
                                Toast.LENGTH_SHORT).show();

                        finish();
                    }
                });
            } catch (JSONException e) {
                Toast.makeText(BikeNetworksListActivity.this,
                        R.string.json_error, Toast.LENGTH_LONG).show();
            } catch (OBSException e) {
                Toast.makeText(BikeNetworksListActivity.this,
                        e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

            }
        }
    }
}
