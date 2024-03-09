package chat.revolt.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import chat.revolt.RevoltApplication

object SqlStorage {
    val driver: SqlDriver = AndroidSqliteDriver(
        Database.Schema,
        RevoltApplication.instance.applicationContext,
        "revolt.db"
    )
}