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

package be.brunoparmentier.openbikesharing.app.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
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
import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.utils.OBSException;
import be.brunoparmentier.openbikesharing.app.utils.parser.BikeNetworksListParser;


public class BikeNetworksListActivity extends Activity {

    private final String BASE_URL = "http://api.citybik.es/v2/networks";
    private final String NETWORK_ID_LABEL = "network-id";
    private ListView listView;
    private ArrayList<BikeNetworkInfo> bikeNetworks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_networks_list);

        listView = (ListView) findViewById(R.id.networksListView);
        new JSONDownloadTask().execute(BASE_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bike_networks_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            new JSONDownloadTask().execute(BASE_URL);
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
                    String strLine = null;
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


                final ArrayList<String> list = new ArrayList<String>();
                for (BikeNetworkInfo bikeNetwork : bikeNetworks) {
                    list.add(bikeNetwork.getLocation().getCity() + " - " + bikeNetwork.getName());
                }

                ArrayAdapter networksList = new ArrayAdapter(BikeNetworksListActivity.this,
                        android.R.layout.simple_list_item_2, android.R.id.text1, list) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                        text1.setText(bikeNetworks.get(position).getLocation().getCity()
                                + " (" + bikeNetworks.get(position).getLocation().getCountry() + ")");
                        text2.setText(bikeNetworks.get(position).getName());
                        return view;
                    }
                };

                listView.setAdapter(networksList);
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
