package com.cbapps.kempengemeenten.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import org.threeten.bp.Duration;
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
	private String street;
	private int houseNumber;
	private String appendix;
	private String measured;
	private int measuringMinutes;
	private String photos;

	private transient LocalDateTime parsedDate;
	private transient Duration parsedDuration;

	public LmsPoint(int lmsNumber, int rdX, int rdY, String town, String street,
	                int houseNumber, String appendix, String measured, int measuringMinutes,
	                String photos) {
		this.lmsNumber = lmsNumber;
		this.rdX = rdX;
		this.rdY = rdY;
		this.town = town;
		this.street = street;
		this.houseNumber = houseNumber;
		this.appendix = appendix;
		this.measured = measured;
		this.measuringMinutes = measuringMinutes;
		this.photos = photos;
	}

	public String getAddress() {
		return street + " " + houseNumber + " " + appendix;
	}

	public String getAppendix() {
		return appendix;
	}

	public LocalDateTime getDateMeasured() {
		if (measured == null)
			return null;
		if (parsedDate == null)
			parsedDate = LocalDateTime.parse(measured);
		return parsedDate;
	}

	public int getHouseNumber() {
		return houseNumber;
	}

	public int getLmsNumber() {
		return lmsNumber;
	}

	public String getMeasured() {
		return measured;
	}

	public Duration getMeasuringDuration() {
		if (parsedDuration == null)
			parsedDuration = Duration.ofMinutes(measuringMinutes);
		return parsedDuration;
	}

	public int getMeasuringMinutes() {
		return measuringMinutes;
	}

	public JsonArray getPhotoLocations() {
		if (photos == null || photos.isEmpty())
			return null;
		return new Gson().fromJson(photos, JsonArray.class);
	}

	public String getPhotos() {
		return photos;
	}

	public int getRdX() {
		return rdX;
	}

	public int getRdY() {
		return rdY;
	}

	public String getStreet() {
		return street;
	}

	public String getTown() {
		return town;
	}

	public boolean isMeasured() {
		return !(measured == null || measured.isEmpty()) && getDateMeasured() != null;
	}
}
