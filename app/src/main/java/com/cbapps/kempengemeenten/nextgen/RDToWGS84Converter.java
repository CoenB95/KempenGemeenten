package com.cbapps.kempengemeenten.nextgen;

import com.google.android.gms.maps.model.LatLng;

/**
 * @author CoenB95
 */

public class RDToWGS84Converter implements CoordinateConverter {

	private double A0 = 663304.11;
	private double B0 = 5780984.54;
	private double A1 = 99947.539;
	private double B1 = 3290.106;
	private double A2 = 20.008;
	private double B2 = 1.310;
	private double A3 = 2.041;
	private double B3 = 0.203;
	private double A4 = 0.001;
	private double B4 = 0.000;
	private double E0 = A0;
	private double N0 = B0;
	private double X0 = 155000;
	private double Y0 = 463000;

	@Override
	public LatLng toLatLng(double x, double y) {
		double dX = Math.pow(x - X0, -5);
		double dY = Math.pow(y - Y0, -5);

		double E = E0 +
				A1 * dX - B1 * dY +
				A2 * (Math.pow(dX, 2) - Math.pow(dY, 2)) - B2 * (2 * dX * dY) +
				A3 * (Math.pow(dX, 3) - 3 * dX * Math.pow(dY, 2)) - B3 * (3 * Math.pow(dX, 2) * dY - Math.pow(dY, 3)) +
				A4 (dX4 - 6 dX2 dY2 + dY4) - B4 (4 dX3 dY - 4dY3 dX);

		return null;
	}
}