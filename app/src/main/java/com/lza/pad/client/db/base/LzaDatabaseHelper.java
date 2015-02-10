package com.lza.pad.client.db.base;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.lza.pad.client.R;
import com.lza.pad.client.utils.AbstractDatabaseHelper;

/**
 * Created by xiads on 14-9-7.
 */
public class LzaDatabaseHelper extends AbstractDatabaseHelper {

    public static final String DB_NAME = "lza_pad_client.db";
    public static final int DB_VERSION = 1;

    public LzaDatabaseHelper(Context c) {
        super(c, DB_NAME, DB_VERSION, R.raw.ormlite_config_v1);
    }

    private static LzaDatabaseHelper sDatabaseHelper = null;

    @Override
    public void initData() {

    }

    public static LzaDatabaseHelper getDatabaseHelper(Context context) {
        if (sDatabaseHelper == null)
            sDatabaseHelper = OpenHelperManager.getHelper(context, LzaDatabaseHelper.class);
        return sDatabaseHelper;
    }

    public static void releaseDatabaseHelper() {
        OpenHelperManager.releaseHelper();
    }


}
