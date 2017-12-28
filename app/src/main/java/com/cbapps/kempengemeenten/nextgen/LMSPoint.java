package com.cbapps.kempengemeenten.nextgen;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * @author CoenB95
 */

@Entity
public class LMSPoint {
	@PrimaryKey
	public int lmsNumber;
	public int rdX;
	public int rdY;
	public String town;
	public String street;
	public int streetNumber;
	public String appendix;

	public LMSPoint() {

	}

	public LMSPoint(int lmsNumber, int rdX, int rdY, String town, String street, int streetNumber, String appendix) {
		this.lmsNumber = lmsNumber;
		this.rdX = rdX;
		this.rdY = rdY;
		this.town = town;
		this.street = street;
		this.streetNumber = streetNumber;
		this.appendix = appendix;
	}

	public int getLmsNumber() {
		return lmsNumber;
	}

	public boolean hasAppendix() {
		return appendix != null;
	}
}
