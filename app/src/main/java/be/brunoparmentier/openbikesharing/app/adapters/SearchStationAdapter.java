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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.models.Station;
import be.brunoparmentier.openbikesharing.app.activities.StationActivity;

/**
 * Cursor adapter to display search results in a dropdown list
 */
public class SearchStationAdapter extends CursorAdapter {

    private ArrayList<Station> stations;

    public SearchStationAdapter(Context context, Cursor cursor, ArrayList<Station> stations) {
        super(context, cursor, false);
        this.stations = stations;
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        final int position = cursor.getPosition();
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(stations.get(position).getName());
        textView.setTextColor(context.getResources().getColor(android.R.color.secondary_text_light));
        view.setBackgroundColor(Color.rgb(243, 243, 243)); // background_holo_light
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, StationActivity.class);
                intent.putExtra("station", stations.get(position));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        return inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
    }

}