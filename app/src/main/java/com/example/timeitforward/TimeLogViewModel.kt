package com.example.timeitforward

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timeitforward.data.db.TimeLogRoomDatabase
import com.example.timeitforward.model.db.TimeLog
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeLogViewModel(application: Application) : ViewModel() {

    val allTimeLogs: LiveData<List<TimeLog>>
    private val repository: TimeLogRepository
    val searchResults: MutableLiveData<List<TimeLog>>

    init {
        val timeLogDb = TimeLogRoomDatabase.getInstance(application)
        val timeLogDao = timeLogDb.TimeLogDao()
        repository = TimeLogRepository(timeLogDao)

        allTimeLogs = repository.allTimeLogs
        searchResults = repository.searchResults
    }

    fun insertTimeLog(timeLog: TimeLog) {
        repository.insertTimeLog(timeLog)
    }

    fun findTimeLogByDateTime(fromDateTime: LocalDateTime, untilDateTime: LocalDateTime) {
        repository.findTimeLogBetweenDateTimes(fromDateTime, untilDateTime)
    }

    fun findTimeLogOfContentTypeBetweenDateTimes(fromDateTime: LocalDateTime,
                                                 untilDateTime: LocalDateTime,
                                                 contentType: String) {
        repository.findTimeLogOfContentTypeBetweenDateTimes(fromDateTime, untilDateTime, contentType)
    }

    fun findTimeLogByContent(content: String) {
        repository.findTimeLogByContent(content)
    }

    fun findTimeLogByContentType(contentType: String) {
        repository.findTimeLogByContentType(contentType)
    }

    fun findTimeLogByNotContentTypes(contentTypes: List<String>) {
        repository.findTimeLogByNotContentTypes(contentTypes)
    }

    fun getLastLogInContent(contentType: String): TimeLog? {
        return repository.getLastLogInContentType(contentType)
    }

    fun getFistLogInContent(contentType: String): TimeLog? {
        return repository.getFirstLogInContentType(contentType)
    }

    fun getFistLog(): TimeLog? {
        return repository.getFirstLog()
    }

    fun deleteTimeLog(id: Int) {
        repository.deleteTimeLog(id)
    }
}


// TimeLogの保存
fun insertTimeLog(
    contentType: String = "", timeContent: String = "",
    fromDateTime: LocalDateTime?, untilDateTime: LocalDateTime?,
    viewModel: TimeLogViewModel
) {
    if (
        (fromDateTime != null) // 開始時刻がnullでない
        && (untilDateTime != null) // 終了時刻がnullでない
        && (untilDateTime > fromDateTime) // 開始時間が終了時間より先
        && (ChronoUnit.MINUTES.between(untilDateTime, fromDateTime) < 24 * 60) // 開始時刻と終了時刻の差が1日以内
    ) {
        // TimeLogをデータベースに挿入
        viewModel.insertTimeLog(
            TimeLog(
                contentType = contentType.ifBlank { "不明" },
                timeContent = timeContent.ifBlank { "不明" },
                fromDateTime = fromDateTime,
                untilDateTime = untilDateTime
            )
        )
    } else {
        // ログで通知
        Log.e("insertTimeLog", "Invalid time record")

    }
}
// TimeLogオブジェクトでできる版
fun insertTimeLog(timeLog: TimeLog, viewModel: TimeLogViewModel) {
    if (
        (timeLog.untilDateTime > timeLog.fromDateTime) // 開始時間が終了時間より先
        && (ChronoUnit.MINUTES.between(timeLog.untilDateTime, timeLog.fromDateTime) < 24 * 60) // 開始時刻と終了時刻の差が1日以内
    ) {
        // TimeLogをデータベースに挿入
        viewModel.insertTimeLog(timeLog)
    } else {
        // ログで通知
        Log.e("insertTimeLog", "Invalid time record")

    }
}
