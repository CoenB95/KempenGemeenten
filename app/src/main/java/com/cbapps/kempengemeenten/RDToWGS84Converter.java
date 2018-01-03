package com.cbapps.kempengemeenten;

import com.google.android.gms.maps.model.LatLng;

/**
 * @author CoenB95
 */

public class RDToWGS84Converter implements CoordinateConverter {

	private double X0      = 155000;
	private double Y0      = 463000;
	private double phi0    = 52.15517440;
	private double lam0    = 5.38720621;

	@Override
	public LatLng toLatLng(double x, double y) {
		double[] Kp = {0,2,0,2,0,2,1,4,2,4,1};
		double[] Kq = {1,0,2,1,3,2,0,0,3,1,1};
		double[] Kpq = {3235.65389,-32.58297,-0.24750,-0.84978,-0.06550,-0.01709,-0.00738,0.00530,-0.00039,0.00033,-0.00012};

		double[] Lp = {1,1,1,3,1,3,0,3,1,0,2,5};
		double[] Lq = {0,1,2,0,3,1,1,2,4,2,0,0};
		double[] Lpq = {5260.52916,105.94684,2.45656,-0.81885,0.05594,-0.05607,0.01199,-0.00256,0.00128,0.00022,-0.00022,0.00026};

		double dX = 1E-5 * ( x - X0 );
		double dY = 1E-5 * ( y - Y0 );

		double phi = 0;
		double lam = 0;

		for (int k = 0; k < Kpq.length; k++)
			phi = phi + (Kpq[k] * Math.pow(dX, Kp[k]) * Math.pow(dY, Kq[k]));
		phi = phi0 + phi / 3600;

		for (int l = 0; l < Lpq.length; l++)
			lam = lam + (Lpq[l] * Math.pow(dX, Lp[l]) * Math.pow(dY, Lq[l]));
		lam = lam0 + lam / 3600;

		return new LatLng(phi, lam);
	}
}