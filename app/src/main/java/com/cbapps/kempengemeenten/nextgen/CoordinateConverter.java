package com.cbapps.kempengemeenten.nextgen;

import com.google.android.gms.maps.model.LatLng;

/**
 * @author CoenB95
 */
public interface CoordinateConverter {
	LatLng toLatLng(double x, double y);
}
