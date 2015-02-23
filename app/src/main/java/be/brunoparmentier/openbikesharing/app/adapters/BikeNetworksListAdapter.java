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

package be.brunoparmentier.openbikesharing.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.models.BikeNetworkInfo;

/**
 * Define a list of bike networks.
 */
public class BikeNetworksListAdapter extends ArrayAdapter<BikeNetworkInfo> {

    public BikeNetworksListAdapter(Context context, int resource, int textViewResourceId, ArrayList<BikeNetworkInfo> networks) {
        super(context, resource, textViewResourceId, networks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        BikeNetworkInfo network = getItem(position);

        if (network != null) {
            TextView text1 = (TextView) v.findViewById(android.R.id.text1);
            TextView text2 = (TextView) v.findViewById(android.R.id.text2);

            text1.setText(network.getLocation().getCity()
                    + " (" + network.getLocation().getCountry() + ")");
            text2.setText(network.getName());
        }

        return v;
    }
}
