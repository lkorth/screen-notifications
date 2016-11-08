package com.lukekorth.screennotifications.helpers;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class DatabaseMigrations implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        if (oldVersion < 1) {
            schema.get("App")
                    .addField("name", String.class);

            oldVersion++;
        }
    }
}
