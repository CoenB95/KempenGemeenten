package com.cbapps.kempengemeenten.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import org.threeten.bp.LocalDateTime;

import java.io.Serializable;

/**
 * @author CoenB95
 */

@Entity
public class LmsPoint implements Serializable {
	@PrimaryKey
	private int lmsNumber;
	private int rdX;
	private int rdY;
	private String town;
	private String address;
	private String measured;
	private transient LocalDateTime parsedDate;

	public LmsPoint(int lmsNumber, int rdX, int rdY, String town, String address, String measured) {
		this.lmsNumber = lmsNumber;
		this.rdX = rdX;
		this.rdY = rdY;
		this.town = town;
		this.address = address;
		this.measured = measured;
	}

	public String getAddress() {
		return address;
	}

	public LocalDateTime getDateMeasured() {
		if (parsedDate != null)
			return parsedDate;
		if (measured == null)
			return null;
		return (parsedDate = LocalDateTime.parse(measured));
	}

	public int getLmsNumber() {
		return lmsNumber;
	}

	public String getMeasured() {
		return measured;
	}

	public int getRdX() {
		return rdX;
	}

	public int getRdY() {
		return rdY;
	}

	public String getTown() {
		return town;
	}

	public boolean isMeasured() {
		return !(measured == null || measured.isEmpty()) && getDateMeasured() != null;
	}
}
