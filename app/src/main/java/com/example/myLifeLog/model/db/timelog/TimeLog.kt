package com.example.myLifeLog.model.db.timelog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime


@Entity(tableName="time_log")
class TimeLog
    (
    @ColumnInfo(name = "content_type") var contentType: Int,
    @ColumnInfo(name = "time_content") var timeContent: String,
    @ColumnInfo(name = "from_datetime") var fromDateTime: LocalDateTime,
    @ColumnInfo(name = "until_datetime") var untilDateTime: LocalDateTime
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "time_log_id")
    var id: Int = 0

}