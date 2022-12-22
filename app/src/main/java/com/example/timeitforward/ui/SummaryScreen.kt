package com.example.timeitforward.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.timeitforward.*
import com.example.timeitforward.R
import com.example.timeitforward.model.AppLogManager
import com.example.timeitforward.model.LocationLogManager
import com.example.timeitforward.model.db.timelog.TimeLog
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.round

@Composable
fun SummaryScreenSetup(viewModel: MainViewModel) {
    val allTimeLogs by viewModel.allTimeLogs.observeAsState(listOf())
    val activity = LocalContext.current as MainActivity
    val appLogManager = AppLogManager(activity, viewModel)
    val locationLogManager = LocationLogManager(activity, viewModel)
    val sleepLogManager = SleepLogManager(activity, viewModel)
    //allTimeLogsを、contextTypeで、fromDateの00:00からuntilDateの23:59までに動いていたTimeLogのリストに更新する

    SummaryScreen(
        allTimeLogs = allTimeLogs,
        firstDate = getFirstDate(viewModel),
        appLogManager = appLogManager,
        locationLogManager = locationLogManager,
        sleepLogManager = sleepLogManager
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SummaryScreen(
    allTimeLogs: List<TimeLog>,
    firstDate: LocalDate,
    appLogManager: AppLogManager,
    locationLogManager: LocationLogManager,
    sleepLogManager: SleepLogManager
) {
    var periodTabIndex by remember { mutableStateOf(2) }
    val contentTypes = listOf("app","location","sleep","others")
    var contentTabIndex by remember { mutableStateOf(0) }
    val contentType = contentTypes[contentTabIndex]
    val currentDate = LocalDate.now(ZoneId.systemDefault()) //現在の日付

    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        UpdateButton(
            modifier = Modifier,
            appLogManager = appLogManager,
            locationLogManager = locationLogManager,
            sleepLogManager = sleepLogManager
        )
        when(periodTabIndex) {
            0 -> MonthPage(
                modifier = Modifier.weight(2f),
                currentDate = currentDate,
                firstDate = firstDate,
                allTimeLogs = allTimeLogs,
                contentType = contentType
                )
            1 -> WeekPage(
                modifier = Modifier.weight(2f),
                currentDate = currentDate,
                firstDate = firstDate,
                allTimeLogs = allTimeLogs,
                contentType = contentType
            )
            2 -> DayPage(
                modifier = Modifier.weight(2f),
                currentDate = currentDate,
                firstDate = firstDate,
                allTimeLogs = allTimeLogs,
                contentType = contentType
            )
        }
        //集計期間の単位（日・週・月）を決めるタブ
        PeriodTabBar(
            modifier = Modifier,
            periodTabIndex = periodTabIndex,
            onTabSwitch = { index, _ -> periodTabIndex = index}
        )
        //コンテンツタイプ（アプリ、場所など）を決めるタブ
        AppTabBar(
            modifier = Modifier,
            contentTabIndex = contentTabIndex,
            onTabSwitch = {index, _ -> contentTabIndex = index }
        )
    }
}

@Composable
fun DayPage(
    modifier: Modifier,
    currentDate: LocalDate,
    firstDate: LocalDate,
    allTimeLogs: List<TimeLog>,
    contentType: String
) {
    val heightAlpha = 2f
    val dateList = mutableListOf<LocalDate>()
    var tmpDate = currentDate
    while (tmpDate>=firstDate) {
        dateList.add(tmpDate)
        tmpDate = tmpDate.minusDays(1)
    }
    val calcEndDate: (LocalDate) -> LocalDate = { selectedDate -> selectedDate }
    val calcTimeIndices: (LocalDate, LocalDate) -> List<LocalDateTime>
            = { selectedDate, _ ->
        (0..23).toList().map { LocalDateTime.of(selectedDate, LocalTime.of(it, 0)) } }
    SummaryContent(
        modifier = modifier,
        dateList = dateList,
        calcEndDate = calcEndDate,
        heightAlpha = heightAlpha,
        calcTimeIndices = calcTimeIndices,
        allTimeLogs = allTimeLogs,
        contentType = contentType
    )
}

@Composable
fun WeekPage(
    modifier: Modifier,
    currentDate: LocalDate,
    firstDate: LocalDate,
    allTimeLogs: List<TimeLog>,
    contentType: String,
) {
    val heightAlpha = 2f/6f
    val dateList = mutableListOf<LocalDate>()
    var tmpSunday = currentDate.minusDays(currentDate.dayOfWeek.value.toLong())
    while (tmpSunday >= firstDate.minusDays(firstDate.dayOfWeek.value.toLong())) {
        dateList.add(tmpSunday)
        tmpSunday = tmpSunday.minusWeeks(1)
    }
    val calcEndDate: (LocalDate) -> LocalDate
            = { selectedDate -> selectedDate.plusWeeks(1).minusDays(1 ) }
    val calcTimeIndices: (LocalDate, LocalDate) -> List<LocalDateTime>
            = { selectedDate, endDate ->
        val timeIndices = mutableListOf<LocalDateTime>()
        var tmpDate = selectedDate
        while (tmpDate <= endDate){
            listOf<Int>(0,6,12,18).forEach {
                timeIndices.add(LocalDateTime.of(tmpDate, LocalTime.of(it,0)))
            }
            tmpDate = tmpDate.plusDays(1)
        }
        timeIndices }
    SummaryContent(
        modifier = modifier,
        dateList = dateList,
        calcEndDate = calcEndDate,
        heightAlpha = heightAlpha,
        calcTimeIndices = calcTimeIndices,
        allTimeLogs = allTimeLogs,
        contentType = contentType
    )
}

@Composable
fun MonthPage(
    modifier: Modifier,
    currentDate: LocalDate,
    firstDate: LocalDate,
    allTimeLogs: List<TimeLog>,
    contentType: String
) {
    val heightAlpha = 2f/24f
    val dateList = mutableListOf<LocalDate>()
    var tmpMonthDay = currentDate.withDayOfMonth(1)
    while (tmpMonthDay >= firstDate.withDayOfMonth(1)) {
        dateList.add(tmpMonthDay)
        tmpMonthDay = tmpMonthDay.minusMonths(1)
    }
    val calcEndDate: (LocalDate) -> LocalDate
            = { selectedDate -> selectedDate.plusMonths(1).minusDays(1 ) }
    val calcTimeIndices: (LocalDate, LocalDate) -> List<LocalDateTime>
            = { selectedDate, endDate ->
        val timeIndices = mutableListOf<LocalDateTime>()
        var tmpDate = selectedDate
        while (tmpDate <= endDate){
            timeIndices.add(LocalDateTime.of(tmpDate, LocalTime.of(0,0)))
            tmpDate = tmpDate.plusDays(1)
        }
        timeIndices }
    SummaryContent(
        modifier = modifier,
        dateList = dateList,
        calcEndDate = calcEndDate,
        heightAlpha = heightAlpha,
        calcTimeIndices = calcTimeIndices,
        allTimeLogs = allTimeLogs,
        contentType = contentType
    )
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun SummaryContent(modifier: Modifier,
                   dateList: List<LocalDate>,
                   calcEndDate: (LocalDate) -> LocalDate,
                   heightAlpha: Float,
                   calcTimeIndices: (LocalDate, LocalDate) -> List<LocalDateTime>,
                   allTimeLogs: List<TimeLog>,
                   contentType: String
) {
    val pagerState = rememberPagerState(initialPage = dateList.size-1)
    val coroutineScope = rememberCoroutineScope()
    Log.d("tag1", "$dateList")
    var dateList = dateList.reversed()
    Log.d("tag", "$dateList")
    HorizontalPager(modifier = modifier, count = dateList.size, state = pagerState) { page ->
        var selectedDate = dateList[page]
        val endDate = calcEndDate(selectedDate)
        val timeUnit = TimeUnit.MINUTES.toMillis(1) //分を単位とする
        val allTime
        = LocalDateTime.of(endDate, LocalTime.MAX).toMilliSec() - LocalDateTime.of(selectedDate, LocalTime.MIN).toMilliSec()
        val timeLogs = allTimeLogs.betweenOf(contentType, selectedDate, endDate).cropped(selectedDate, endDate)
        Column() {
            DateSelectionDropDown(dateList = dateList, selectedDate = selectedDate,
                onSelected = {
                    coroutineScope.launch { pagerState.scrollToPage(dateList.indexOf(it)) }
                })

            var appNameSelected: String by remember{ mutableStateOf("") }
            val timeLogSummaryListState = rememberLazyListState()

            val timeIndices = calcTimeIndices(selectedDate, endDate)
            val timeLine: List<TimeLog> = timeLogs.sortByDateTime().toCloseConcatenated(timeUnit)
                .toTimeGapFilled(
                    LocalDateTime.of(selectedDate, LocalTime.MIN),
                    LocalDateTime.of(endDate, LocalTime.MAX)
                )
                .toTimeIndexInserted(timeIndices)

            val timeLogSummaryList: List<TimeLogSummary> = timeLogs.toTimeLogSummaryList().sorted()

            //timeLineのlazyList
            LazyColumn(modifier = Modifier
                .border(width = 1.dp, color = Color.Gray)
                .weight(2f)
            ) {
                items(timeLine) { item ->
                    TimeLogLine(timeLog = item, timeUnit = timeUnit, heightAlpha = heightAlpha,
                        isPackageNameSelected = appNameSelected == item.timeContent,
                        onClick = {
                            appNameSelected = item.timeContent
                            coroutineScope.launch {
                                timeLogSummaryListState.scrollToItem(
                                    timeLogSummaryList.indexOfFirst { it.timeContent == item.timeContent }
                                )
                            }
                        })
                }
            }

            // timeLogSummaryのlazyList
            Box(
                modifier = Modifier
                    .border(width = 1.dp, color = Color.Gray)
                    .weight(1f, true)
                    .background(Color(0x44FFEBCD))
            ){
                if (timeLogSummaryList.isNotEmpty()) {
                    LazyColumn(
                        state = timeLogSummaryListState
                    ) {
                        items(timeLogSummaryList) { item ->
                            TimeLogSummaryRow(
                                contentType = contentType,
                                timeLogSummary = item,
                                onClick = { appNameSelected = if (appNameSelected != item.timeContent) item.timeContent else "" },
                                non_transparency = if(appNameSelected==item.timeContent) 255 else 128,
                                allTime = allTime
                            )
                        }
                    }
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .align(Alignment.Center),
                        fontSize = 20.sp,
                        text =stringResource(id = R.string.no_log))
                }
            }
        }
    }

}

// Roomに保存された最古のTimeLogの日付（開始）を出力する。もしTimeLogが1つも存在しなければ現在の日付を出力
private fun getFirstDate(viewModel: MainViewModel): LocalDate {
    return viewModel.getFistLog().let {
        if (it!=null) {
            it.fromDateTime.toLocalDate()
        } else {
            LocalDate.now(ZoneId.systemDefault())
        }
    }
}

//TimeLogを表示するボックス。
@Composable
fun TimeLogLine(timeLog: TimeLog,
                timeUnit:Long, heightAlpha: Float,
                modifier: Modifier = Modifier,
                isPackageNameSelected: Boolean,
                onClick: () -> Unit
){
    val height = ((timeLog.untilDateTime.toMilliSec() - timeLog.fromDateTime.toMilliSec()) / timeUnit * heightAlpha).toInt().dp
    val context = LocalContext.current
    when (timeLog.contentType) {
        "__IndexedDateTime__" -> Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0x88CCFFFF), shape = RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "${timeLog.fromDateTime.toLocalDate()} ${timeLog.fromDateTime.hour}:00~"
                )
            }

        "__Dummy__" -> Row(modifier = modifier
            .fillMaxSize()
            .height(height)) {}

        else -> Row(modifier = modifier
            .fillMaxSize()
            .background(
                getAppName(timeLog.timeContent, context).toColor(
                    if (isPackageNameSelected) 255 else 126
                ), shape = RoundedCornerShape(4.dp)
            )
            .height(height)
            .selectable(false, onClick = onClick)){}
    }
}

@Composable
fun DateSelectionDropDown(
    modifier: Modifier = Modifier,
    dateList: List<LocalDate>,
    selectedDate: LocalDate,
    onSelected: (LocalDate) -> Unit,
){
    var dropDownExpanded: Boolean by remember { mutableStateOf(false) }
    Box(modifier = modifier
        .padding(8.dp)
        .background(Color.LightGray),
        contentAlignment = Alignment.Center
        ) {
        Text(
            selectedDate.toString(),
            modifier = Modifier
                .clickable(onClick = {
                    dropDownExpanded = true
                }))
        DropdownMenu(
            expanded = dropDownExpanded,
            onDismissRequest = { dropDownExpanded = false }) {
            dateList.forEach { item ->
                DropdownMenuItem(onClick = {
                    dropDownExpanded = false
                    onSelected(item)
                }) {
                    Text(text = item.toString())
                }
            }
        }
    }
}

//
fun List<TimeLog>.sortByDateTime(): List<TimeLog> {
    return this.sortedBy { it.fromDateTime  }
}

//TimeLogリストに時刻を表すものをはさみこむ。toTimeGapFilledの行われたTimeLogリストでやること。
fun List<TimeLog>.toTimeIndexInserted(indexedDateTimes: List<LocalDateTime>): List<TimeLog> {
    if (this.isEmpty()){ return this }

    val newTimeLogList = mutableListOf<TimeLog>()
    this.forEach { timeLog ->
        var tmpTimeLog = timeLog
        indexedDateTimes.forEach { indexedDateTime ->
            if (tmpTimeLog.fromDateTime <= indexedDateTime && indexedDateTime < tmpTimeLog.untilDateTime) {
                newTimeLogList.add(
                    TimeLog(
                    contentType = tmpTimeLog.contentType,
                    fromDateTime = tmpTimeLog.fromDateTime,
                    untilDateTime = indexedDateTime,
                    timeContent = tmpTimeLog.timeContent
                )
                )
                newTimeLogList.add(
                    TimeLog(
                    contentType = "__IndexedDateTime__",
                    fromDateTime = indexedDateTime,
                    untilDateTime = indexedDateTime,
                    timeContent = ""
                )
                )
                tmpTimeLog = TimeLog(
                    contentType = tmpTimeLog.contentType,
                    fromDateTime = indexedDateTime,
                    untilDateTime = tmpTimeLog.untilDateTime,
                    timeContent = tmpTimeLog.timeContent
                )
            }
        }
        newTimeLogList.add(tmpTimeLog)
    }
    return newTimeLogList
}

fun max(a: LocalDateTime, b: LocalDateTime) : LocalDateTime{
    return if (a > b) { a } else { b }
}

fun min(a: LocalDateTime, b: LocalDateTime) : LocalDateTime{
    return if (a < b) { a } else { b }
}

fun List<TimeLog>.cropped(fromDate: LocalDate, untilDate: LocalDate): List<TimeLog> {
    return this.map{
        TimeLog(
            it.contentType,
            it.timeContent,
            max(it.fromDateTime, LocalDateTime.of(fromDate, LocalTime.MIN)),
            min(it.untilDateTime, LocalDateTime.of(untilDate, LocalTime.MAX))
        )
    }
}

fun List<TimeLog>.toTimeGapFilled(fromDateTime: LocalDateTime, untilDateTime: LocalDateTime): List<TimeLog> {
    val newTimeLogList = mutableListOf<TimeLog>()
    var tmpDateTime = fromDateTime

    //TimeLogが空なら、すべてダミーで埋める。
    if(this.isEmpty()) {
        newTimeLogList.add(
            TimeLog(
                contentType = "__Dummy__",
                fromDateTime = fromDateTime,
                untilDateTime = untilDateTime,
                timeContent = "")
        )
        return newTimeLogList
    }
    //TimeLogが空でないなら
    this.forEach {
        //もしtmpDateTimeがit(TimeLog)の開始時間より早ければ、
        if(tmpDateTime < it.fromDateTime) {
            // fromDateTimeがtmpDateTime、untilDateTimeがit.fromDateTimeのダミーTimeLogを新しいリストに追加する
            newTimeLogList.add(
                TimeLog(
                contentType = "__Dummy__",
                fromDateTime = tmpDateTime,
                untilDateTime = it.fromDateTime,
                timeContent = "")
            )
        }
        //itを新しいリストに追加する
        newTimeLogList.add(it)
        //tmpDateTimeにit.untilDateTimeを代入する
        tmpDateTime = it.untilDateTime
    }
    //最後のTimeLogの後ろも埋める
    if(tmpDateTime < untilDateTime) {
        newTimeLogList.add(
            TimeLog(
                contentType = "__Dummy__",
                fromDateTime = tmpDateTime,
                untilDateTime = untilDateTime,
                timeContent = "")
        )
    }
    return newTimeLogList
}

//TimeLogリストの中の異なる要素において、内容が同じで時間間隔が短いもの（thrTimeSpan未満）を連続とみなして接合した、新しいリストを返す。
fun List<TimeLog>.toCloseConcatenated(thrTimeSpan: Long): List<TimeLog> {
    val newTimeLogList = mutableListOf<TimeLog>()

    if (this.isEmpty()){ return this }

    var tmpTimeLog: TimeLog = this[0]
    this.forEach {
        if(tmpTimeLog.timeContent==it.timeContent
            && it.fromDateTime.toMilliSec() - tmpTimeLog.untilDateTime.toMilliSec() < thrTimeSpan) {
            tmpTimeLog = TimeLog(
                contentType = tmpTimeLog.contentType,
                fromDateTime = tmpTimeLog.fromDateTime,
                untilDateTime = it.untilDateTime,
                timeContent = tmpTimeLog.timeContent
            )
        } else {
            newTimeLogList.add(tmpTimeLog)
            tmpTimeLog = it
        }
    }
    newTimeLogList.add(tmpTimeLog)
    return newTimeLogList
}

data class TimeLogSummary(
    val contentType: String,
    val timeContent: String,
    val duration: Long //millisecond
)
// TimeLogSummary
@Composable
fun TimeLogSummaryRow(timeLogSummary: TimeLogSummary,
                      onClick: () -> Unit,
                      non_transparency: Int,
                      contentType: String,
                      allTime: Long
) {
    when(contentType) {
        "app" -> AppLogSummaryRow(timeLogSummary, onClick, non_transparency, allTime)
        "location" -> LocationLogSummaryRow(timeLogSummary, onClick, non_transparency, allTime)
        "sleep" -> SleepLogSummaryRow(timeLogSummary, onClick, non_transparency, allTime)
        "others" -> OthersLogSummaryRow(timeLogSummary, onClick, non_transparency, allTime)
    }
}

// TimeLogSummaryの情報を表示（Others用）
@Composable
fun OthersLogSummaryRow(timeLogSummary: TimeLogSummary,
                     onClick: () -> Unit,
                     non_transparency: Int,
                     allTime: Long
) {
    val timeContent = timeLogSummary.timeContent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .background(color = timeContent.toColor(non_transparency))
            .selectable(false, onClick = onClick)
    ) {
        OthersIcon(modifier = Modifier.size(40.dp, 40.dp))
        Text(timeContent, modifier = Modifier.weight(0.2f))
        Text(text = "${timeLogSummary.duration.toHMS()} ${round(timeLogSummary.duration.toDouble()/allTime.toDouble()*1000.0)/10.0}%", modifier = Modifier.weight(0.3f))
    }
}

//　TimeLogSummaryの情報を表示（App用）
@Composable
fun AppLogSummaryRow(timeLogSummary: TimeLogSummary,
                     onClick: () -> Unit,
                     non_transparency: Int,
                     allTime: Long
) { val context: Context = LocalContext.current
    val appName: String = getAppName(timeLogSummary.timeContent, context)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .background(color = appName.toColor(non_transparency))
            .selectable(false, onClick = onClick)
    ) {
        AppIcon(appName = timeLogSummary.timeContent, modifier = Modifier.size(40.dp, 40.dp))
        Text(appName, modifier = Modifier.weight(0.2f))
        Text(text = "${timeLogSummary.duration.toHMS()} ${round(timeLogSummary.duration.toDouble()/allTime.toDouble()*1000.0)/10.0}%", modifier = Modifier.weight(0.3f))
    }
}

//　TimeLogSummaryの情報を表示（Location用）
@Composable
fun LocationLogSummaryRow(timeLogSummary: TimeLogSummary,
                     onClick: () -> Unit,
                     non_transparency: Int,
                          allTime: Long
) { val context: Context = LocalContext.current
    rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .background(color = timeLogSummary.timeContent.toColor(non_transparency))
            .selectable(false, onClick = onClick)
    ) {
        val (activityType, latitude, longitude) = timeLogSummary.timeContent.parseLocation()
        ActivityIcon(activityType = activityType, modifier = Modifier.size(40.dp, 40.dp))
        if (activityType == DetectedActivity.STILL) {
            if (latitude != null && longitude != null) {
                var popupState by remember { mutableStateOf(false) }
                Button(
                    onClick = { popupState = true },
                    modifier = Modifier
                        .weight(0.2f)
                        .height(40.dp)
                ) {
                    Text(text = stringResource(id = R.string.show_map))
                }
                if (popupState){
                    GoogleMapPopup(
                        modifier = Modifier.size(300.dp, 300.dp),
                        lat = latitude,
                        lng = longitude,
                        closeThis = { popupState = false }
                    )
                }
            } else {
                Text(modifier = Modifier.weight(0.2f), text = stringResource(id = R.string.null_location))
            }
        } else { Text(modifier = Modifier
            .weight(0.2f)
            .fillMaxWidth(), text = "") }

        Text(text = "${timeLogSummary.duration.toHMS()} ${round(timeLogSummary.duration.toDouble()/allTime.toDouble()*1000.0)/10.0}%", modifier = Modifier.weight(0.3f))
    }
}

//　TimeLogSummaryの情報を表示（Location用）
@Composable
fun SleepLogSummaryRow(timeLogSummary: TimeLogSummary,
                       onClick: () -> Unit,
                       non_transparency: Int,
                       allTime: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .background(color = timeLogSummary.timeContent.toColor(non_transparency))
            .selectable(false, onClick = onClick)
    ) {
        SleepIcon(sleepState = timeLogSummary.timeContent, modifier = Modifier.size(40.dp, 40.dp))
        Text(when(timeLogSummary.timeContent)
        {"sleep" -> stringResource(id = R.string.sleep_state)
            "awake" -> stringResource(id = R.string.awake_state)
            "unsure" -> stringResource(id = R.string.unsure_state)
            else -> ""
        }, modifier = Modifier.weight(0.2f))
        Text(text = "${timeLogSummary.duration.toHMS()} ${round(timeLogSummary.duration.toDouble()/allTime.toDouble()*1000.0)/10.0}%", modifier = Modifier.weight(0.3f))
    }
}

@Composable
fun GoogleMapPopup(modifier: Modifier, lat:Double, lng:Double, closeThis: () -> Unit) {
    val loc = LatLng(lat, lng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(loc, 15f)
    }
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = closeThis
    ) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = loc),
            )
        }
    }
}

fun List<TimeLog>.toTimeLogSummaryList(): List<TimeLogSummary> {
    val contentsSet: Set<Pair<String, String>> = this.map { Pair(it.timeContent, it.contentType) }.toSet()
    val retList = mutableListOf<TimeLogSummary>()
    contentsSet.forEach { pair ->
        val sameTimeLogList = mutableListOf<TimeLog>()
        this.forEach { timeLog ->
            if(timeLog.timeContent == pair.first && timeLog.contentType == pair.second) {
                sameTimeLogList.add(timeLog)
            }
        }
        val duration = sameTimeLogList.sumOf { it.untilDateTime.toMilliSec() - it.fromDateTime.toMilliSec() }
        retList.add(TimeLogSummary(
            timeContent = pair.first, contentType = pair.second, duration = duration
        ))
    }
    return retList
}

fun List<TimeLogSummary>.sorted(): List<TimeLogSummary> {
    val comparator: Comparator<TimeLogSummary> = compareBy<TimeLogSummary> { it.duration }
    return this.sortedWith(comparator).reversed()
}

fun List<TimeLog>.betweenOf(contentType: String, fromDate: LocalDate, untilDate: LocalDate): List<TimeLog> 
{
    val fromDateTime = LocalDateTime.of(fromDate, LocalTime.MIN)
    val untilDateTime = LocalDateTime.of(untilDate, LocalTime.MAX)
    return if (this.isEmpty()){
        this
    } else {
        this.filter { timeLog ->
                    timeLog.contentType == contentType
                    && (timeLog.untilDateTime > fromDateTime)
                    && (timeLog.fromDateTime < untilDateTime)
        }
    }
}