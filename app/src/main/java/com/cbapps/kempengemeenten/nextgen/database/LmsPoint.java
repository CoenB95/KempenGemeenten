package com.cbapps.kempengemeenten.nextgen.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * @author CoenB95
 */

@Entity
public class LmsPoint {
	@PrimaryKey
	public int lmsNumber;
	public int rdX;
	public int rdY;
	public String town;
	public String address;

	public LmsPoint() {

	}

	public LmsPoint(int lmsNumber, int rdX, int rdY, String town, String address) {
		this.lmsNumber = lmsNumber;
		this.rdX = rdX;
		this.rdY = rdY;
		this.town = town;
		this.address = address;
	}

	public int getLmsNumber() {
		return lmsNumber;
	}
}
