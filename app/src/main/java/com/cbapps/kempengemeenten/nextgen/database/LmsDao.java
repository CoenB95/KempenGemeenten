package com.cbapps.kempengemeenten.nextgen.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.Collection;
import java.util.List;

/**
 * @author CoenB95
 */

@Dao
public interface LmsDao {
	@Query("SELECT * FROM lmspoint")
	List<LmsPoint> getAll();

	@Query("SELECT * FROM lmspoint WHERE lmsNumber LIKE :id LIMIT 1")
	LmsPoint findByLmsNumber(int id);

	@Insert
	void insertAll(LmsPoint... users);

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insertOrReplaceAll(Collection<LmsPoint> users);

	@Delete
	void delete(LmsPoint user);
}
