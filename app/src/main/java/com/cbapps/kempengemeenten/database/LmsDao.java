package com.cbapps.kempengemeenten.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.Collection;
import java.util.List;

/**
 * @author CoenB95
 */

@Dao
public interface LmsDao {
	@Query("SELECT * FROM lmspoint")
	LiveData<List<LmsPoint>> getAllLive();

	@Query("SELECT * FROM lmspoint")
	List<LmsPoint> getAll();

	@Query("SELECT * FROM lmspoint WHERE lmsNumber LIKE :id LIMIT 1")
	LmsPoint findByLmsNumber(int id);

	@Query("SELECT * FROM lmspoint WHERE lmsNumber LIKE :id LIMIT 1")
	LiveData<LmsPoint> findLiveByLmsNumber(int id);

	@Insert
	void insertAll(LmsPoint... users);

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insertOrReplaceAll(Collection<LmsPoint> users);

	@Update
	void update(LmsPoint point);

	@Delete
	void delete(LmsPoint user);
}
