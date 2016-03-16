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
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.models.StationStatus;

/**
 * Define a list of stations with their title, number of bikes and empty slots.
 */
public class StationsListAdapter extends ArrayAdapter<Station> {

    public StationsListAdapter(Context context, int resource, ArrayList<Station> stations) {
        super(context, resource, stations);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.station_list_item, parent, false);
        }

        final Station station = getItem(position);

        if (station != null) {
            TextView stationNameTitle = (TextView) v.findViewById(R.id.stationNameTitle);
            TextView freeBikesValue = (TextView) v.findViewById(R.id.freeBikesValue);
            TextView emptySlotsValue = (TextView) v.findViewById(R.id.emptySlotsValue);
            StationStatus stationStatus = station.getStatus();

            if (stationNameTitle != null) {
                stationNameTitle.setText(station.getName());

                if (stationStatus == StationStatus.CLOSED) {
                    stationNameTitle.setPaintFlags(stationNameTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    stationNameTitle.setPaintFlags(stationNameTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }

            if (freeBikesValue != null) {
                int bikes = station.getFreeBikes();
                freeBikesValue.setText(String.valueOf(bikes));
            }

            if (emptySlotsValue != null) {
                int emptySlots = station.getEmptySlots();
                ImageView emptySlotsLogo = (ImageView) v.findViewById(R.id.emptySlotsLogo);
                if (emptySlots == -1) {
                    emptySlotsLogo.setVisibility(View.GONE);
                    emptySlotsValue.setVisibility(View.GONE);
                } else {
                    emptySlotsLogo.setVisibility(View.VISIBLE);
                    emptySlotsValue.setVisibility(View.VISIBLE);
                    emptySlotsValue.setText(String.valueOf(emptySlots));
                }
            }
        }

        return v;
    }
}