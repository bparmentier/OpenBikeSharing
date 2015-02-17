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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.List;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.Station;
import be.brunoparmentier.openbikesharing.app.StationStatus;
import be.brunoparmentier.openbikesharing.app.db.StationsDataSource;

public class StationActivity extends Activity {
    private SharedPreferences settings;
    private Station station;
    private MapView map;
    private IMapController mapController;
    private MenuItem favStar;
    private StationsDataSource stationsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        stationsDataSource = new StationsDataSource(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        station = (Station) getIntent().getSerializableExtra("station");

        map = (MapView) findViewById(R.id.mapView);
        final GeoPoint stationLocation = new GeoPoint((int) (station.getLatitude() * 1000000),
                (int) (station.getLongitude() * 1000000));

        mapController = map.getController();
        mapController.setZoom(16);

        /* map tile source */
        String mapLayer = settings.getString("pref_map_layer", "");
        switch (mapLayer) {
            case "mapnik":
                map.setTileSource(TileSourceFactory.MAPNIK);
                break;
            case "cyclemap":
                map.setTileSource(TileSourceFactory.CYCLEMAP);
                break;
            case "osmpublictransport":
                map.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT);
                break;
            case "mapquestosm":
                map.setTileSource(TileSourceFactory.MAPQUESTOSM);
                break;
            default:
                map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                break;
        }

        map.setMultiTouchControls(true);

        /* Station marker */
        Marker marker = new Marker(map);
        marker.setPosition(stationLocation);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return false;
            }
        });

        /* Marker icon */
        int emptySlots = station.getEmptySlots();
        int freeBikes = station.getFreeBikes();
        if (emptySlots + freeBikes == 0 || station.getStatus() == StationStatus.CLOSED) {
            marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker_unavailable));
        } else {
            double ratio = (double) freeBikes / (double) (freeBikes + emptySlots);
            if (freeBikes == 0) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker0));
            } else if (freeBikes >= 1 && ratio <= 0.3) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker25));
            } else if (ratio > 0.3 && ratio < 0.7) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker50));
            } else if (ratio >= 0.7 && emptySlots >= 1) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker75));
            } else if (emptySlots == 0) {
                marker.setIcon(getResources().getDrawable(R.drawable.ic_station_marker100));
            }
        }

        map.getOverlays().add(marker);

        TextView stationName = (TextView) findViewById(R.id.stationName);
        TextView stationEmptySlots = (TextView) findViewById(R.id.stationEmptySlots);
        TextView stationFreeBikes = (TextView) findViewById(R.id.stationFreeBikes);

        stationName.setText(station.getName());
        stationEmptySlots.setText(String.valueOf(station.getEmptySlots()));
        stationFreeBikes.setText(String.valueOf(station.getFreeBikes()));

        if (station.getAddress() != null) {
            TextView stationAddress = (TextView) findViewById(R.id.stationAddress);
            stationAddress.setText(station.getAddress());
            stationAddress.setVisibility(View.VISIBLE);
        }

        /* extra info on station */
        Boolean isBankingStation = station.isBanking();
        Boolean isBonusStation = station.isBonus();
        StationStatus stationStatus = station.getStatus();

        if (isBankingStation != null) {
            ImageView stationBanking = (ImageView) findViewById(R.id.stationBanking);
            stationBanking.setVisibility(View.VISIBLE);
            if (isBankingStation) {
                stationBanking.setImageDrawable(getResources().getDrawable(R.drawable.ic_banking_on));
                stationBanking.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.cards_accepted),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        if (isBonusStation != null) {
            ImageView stationBonus = (ImageView) findViewById(R.id.stationBonus);
            stationBonus.setVisibility(View.VISIBLE);
            if (isBonusStation) {
                stationBonus.setImageDrawable(getResources().getDrawable(R.drawable.ic_bonus_on));
                stationBonus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.is_bonus_station),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        if ((stationStatus != null) && stationStatus == StationStatus.CLOSED) {
            stationName.setPaintFlags(stationName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        mapController.setCenter(stationLocation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.station, menu);

        favStar = menu.findItem(R.id.action_favorite);
        if (isFavorite()) {
            favStar.setIcon(R.drawable.ic_menu_favorite);
        } else {
            favStar.setIcon(R.drawable.ic_menu_favorite_outline);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_directions:
                Intent intent = new Intent(Intent.ACTION_VIEW, getStationLocationUri());
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.no_nav_application), Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_favorite:
                setFavorite(!isFavorite());
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Uri getStationLocationUri() {
        return Uri.parse("geo:" + station.getLatitude() + "," + station.getLongitude());
    }

    private boolean isFavorite() {
        return stationsDataSource.isFavoriteStation(station.getId());
    }

    private void setFavorite(boolean favorite) {
        if (favorite) {
            stationsDataSource.addFavoriteStation(station.getId());
            favStar.setIcon(R.drawable.ic_menu_favorite);
            Toast.makeText(StationActivity.this,
                    getString(R.string.station_added_to_favorites), Toast.LENGTH_SHORT).show();
        } else {
            stationsDataSource.removeFavoriteStation(station.getId());
            favStar.setIcon(R.drawable.ic_menu_favorite_outline);
            Toast.makeText(StationActivity.this,
                    getString(R.string.stations_removed_from_favorites), Toast.LENGTH_SHORT).show();
        }
    }
}
