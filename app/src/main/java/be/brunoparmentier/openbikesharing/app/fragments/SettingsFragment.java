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

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import be.brunoparmentier.openbikesharing.app.BuildConfig;
import be.brunoparmentier.openbikesharing.app.R;

/**
 * Settings fragment
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        setupBitcoinButton();
        setupVersionEntry();
    }

    /* Setup "Donate Bitcoin" button */
    private void setupBitcoinButton() {
        final Preference donateBitcoinPref = findPreference("pref_donate_bitcoin");
        Intent bitcoinIntent = new Intent(Intent.ACTION_VIEW);
        bitcoinIntent.setData(Uri.parse("bitcoin:168utA5DWMVXLFVfQDahG5abEWUSk9Wcfm"));

        PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> bitcoinActivities =
                packageManager.queryIntentActivities(bitcoinIntent, 0);
        boolean isBitcoinIntentSafe = bitcoinActivities.size() > 0;

        if (isBitcoinIntentSafe) {
            donateBitcoinPref.setIntent(bitcoinIntent);
        } else {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
            marketIntent.setData(Uri.parse("market://search?q=bitcoin"));

            List<ResolveInfo> marketActivities =
                    packageManager.queryIntentActivities(marketIntent, 0);
            boolean isMarketIntentSafe = marketActivities.size() > 0;

            if (isMarketIntentSafe) {
                donateBitcoinPref.setIntent(marketIntent);
            }
        }
    }

    /* Setup version entry */
    private void setupVersionEntry() {
        String versionName;
        final Preference versionPref = findPreference("pref_version");
        try {
            versionName = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
            if (BuildConfig.DEBUG) {
                /* https://stackoverflow.com/a/7608719/3997816 */
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(getActivity().getPackageName(), 0);
                ZipFile zf = new ZipFile(ai.sourceDir);
                ZipEntry ze = zf.getEntry("classes.dex");
                zf.close();
                long time = ze.getTime();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmdd");

                versionName += "-debug-" + dateFormat.format(time);
            }
            versionPref.setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
