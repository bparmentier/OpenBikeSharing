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

package be.brunoparmentier.openbikesharing.app.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * On some versions of Android (4.2?), the default ListPreference view doesn't update when summary
 * changes.
 * cf. https://code.google.com/p/android/issues/detail?id=27867
 */
public class UpdateListPreference extends ListPreference {

    public UpdateListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UpdateListPreference(Context context) {
        super(context);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();
    }
}
