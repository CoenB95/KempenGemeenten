package com.cbapps.kempengemeenten.nextgen.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * @author CoenB95
 */

@Database(entities = {LmsPoint.class}, version = 1)
public abstract class LmsDatabase extends RoomDatabase {

	private static LmsDatabase instance;

	public static LmsDatabase newInstance(Context context) {
		if (instance == null) {
			instance = Room.databaseBuilder(context, LmsDatabase.class, "lms-database")
					.build();
		}
		return instance;
	}

	public abstract LmsDao lmsDao();
}
