package com.cbapps.kempengemeenten.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.io.Serializable;

/**
 * @author CoenB95
 */

@Entity
public class LmsPoint implements Serializable {
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
