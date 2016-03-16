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

package be.brunoparmentier.openbikesharing.app.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.activities.StationActivity;
import be.brunoparmentier.openbikesharing.app.adapters.StationsListAdapter;
import be.brunoparmentier.openbikesharing.app.models.Station;

public class StationsListFragment extends Fragment {
    private static final String KEY_STATION = "station";
    private static final String KEY_STATIONS = "stations";
    private static final String KEY_EMPTY_LIST_RESOURCE_ID = "emptyListResourceId";

    private ArrayList<Station> stations;
    private StationsListAdapter stationsListAdapter;
    private int emptyListResourceId;

    /* newInstance constructor for creating fragment with arguments */
    public static StationsListFragment newInstance(ArrayList<Station> stations, int emptyListResourceId) {
        StationsListFragment stationsListFragment = new StationsListFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_STATIONS, stations);
        args.putInt(KEY_EMPTY_LIST_RESOURCE_ID, emptyListResourceId);
        stationsListFragment.setArguments(args);
        return stationsListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stations = (ArrayList<Station>) getArguments().getSerializable(KEY_STATIONS);
        emptyListResourceId = getArguments().getInt(KEY_EMPTY_LIST_RESOURCE_ID);

        stationsListAdapter = new StationsListAdapter(getActivity(),
                R.layout.station_list_item, stations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations_list, container, false);
        ListView listView = (ListView) view.findViewById(R.id.stationsListView);
        listView.setAdapter(stationsListAdapter);
        TextView emptyView = (TextView) view.findViewById(R.id.emptyList);
        emptyView.setText(emptyListResourceId);
        listView.setEmptyView(view.findViewById(R.id.emptyList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(getActivity(), StationActivity.class);
                intent.putExtra(KEY_STATION, stations.get(position));
                startActivity(intent);
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
                if(firstVisibleItem == 0) refreshLayout.setEnabled(true);
                else refreshLayout.setEnabled(false);
            }
        });
        return view;
    }

    public void updateStationsList(ArrayList<Station> stations) {
        if (stationsListAdapter != null) {
            stationsListAdapter.clear();
            stationsListAdapter.addAll(stations);
            stationsListAdapter.notifyDataSetChanged();
        }
    }

}
