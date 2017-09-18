package com.ruuvi.tag.database;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
import com.ruuvi.tag.model.RuuviTag;

/**
 * Created by berg on 10/09/17.
 */

@Database(name = LocalDatabase.NAME, version = LocalDatabase.VERSION)
public class LocalDatabase {
    public static final String NAME = "LocalDatabase";
    public static final int VERSION = 2;

    @Migration(version = 2, database = LocalDatabase.class)
    public static class Migration2 extends AlterTableMigration<RuuviTag> {
        public Migration2(Class<RuuviTag> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.TEXT, "gatewayUrl");
        }
    }
}
