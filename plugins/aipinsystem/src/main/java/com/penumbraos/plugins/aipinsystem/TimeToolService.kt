package com.penumbraos.plugins.aipinsystem

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import com.penumbraos.mabl.sdk.ISystemServiceRegistry
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolParameter
import com.penumbraos.mabl.sdk.ToolService
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "TimeToolService"

private const val GET_CURRENT_TIME_TOOL = "get_current_time"
private const val CREATE_TIMER_TOOL = "create_timer"
private const val LIST_TIMERS_TOOL = "list_timers"
private const val CANCEL_TIMER_TOOL = "cancel_timer"
private const val CREATE_ALARM_TOOL = "create_alarm"
private const val LIST_ALARMS_TOOL = "list_alarms"
private const val CANCEL_ALARM_TOOL = "cancel_alarm"

data class TimerData(
    val id: String,
    val name: String,
    val durationSeconds: Long,
    val startTime: Long,
    val endTime: Long
)

data class AlarmData(
    val id: String,
    val name: String,
    val triggerTime: Long,
    val recurring: Boolean = false
)

class TimeToolService : ToolService("TimeToolService") {

    private lateinit var prefs: SharedPreferences
    private val activeTimers = ConcurrentHashMap<String, Timer>()
    private val activeAlarms = ConcurrentHashMap<String, Timer>()
    private val handler = Handler(Looper.getMainLooper())
    private var systemServices: ISystemServiceRegistry? = null

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences("timers_alarms", MODE_PRIVATE)

        restoreTimersAndAlarms()
    }

    override fun executeTool(call: ToolCall, params: JSONObject?, callback: IToolCallback) {
        when (call.name) {
            GET_CURRENT_TIME_TOOL -> getCurrentTime(callback, call.isLLM)
            CREATE_TIMER_TOOL -> createTimer(params, callback)
            LIST_TIMERS_TOOL -> listTimers(callback)
            CANCEL_TIMER_TOOL -> cancelTimer(params, callback)
            CREATE_ALARM_TOOL -> createAlarm(params, callback)
            LIST_ALARMS_TOOL -> listAlarms(callback)
            CANCEL_ALARM_TOOL -> cancelAlarm(params, callback)
            else -> callback.onError("Unknown tool: ${call.name}")
        }
    }

    override fun getToolDefinitions(): Array<ToolDefinition> {
        return arrayOf(
            ToolDefinition().apply {
                name = GET_CURRENT_TIME_TOOL
                description = "Get the current date and time"
                examples = arrayOf(
                    "what time is it",
                    "current time",
                    "tell me the time"
                )
                parameters = emptyArray()
            },
            ToolDefinition().apply {
                name = CREATE_TIMER_TOOL
                description = "Create a timer that counts down for a specified duration"
                parameters = arrayOf(ToolParameter().apply {
                    name = "name"
                    type = "string"
                    description = "Name of the timer"
                    required = true
                    enumValues = emptyArray()
                }, ToolParameter().apply {
                    name = "duration_seconds"
                    type = "number"
                    description = "Duration of the timer in seconds"
                    required = true
                    enumValues = emptyArray()
                })
            },
            ToolDefinition().apply {
                name = LIST_TIMERS_TOOL
                description = "List all active timers"
                parameters = emptyArray()
            },
            ToolDefinition().apply {
                name = CANCEL_TIMER_TOOL
                description = "Cancel a specific timer by ID"
                parameters = arrayOf(ToolParameter().apply {
                    name = "timer_id"
                    type = "string"
                    description = "ID of the timer to cancel"
                    required = true
                    enumValues = emptyArray()
                })
            },
            ToolDefinition().apply {
                name = CREATE_ALARM_TOOL
                description = "Create an alarm for a specific date and time"
                parameters = arrayOf(ToolParameter().apply {
                    name = "name"
                    type = "string"
                    description = "Name of the alarm"
                    required = true
                    enumValues = emptyArray()
                }, ToolParameter().apply {
                    name = "datetime_iso"
                    type = "string"
                    description =
                        "ISO 8601 formatted date and time when the alarm will trigger"
                    required = true
                    enumValues = emptyArray()
                })
            },
            ToolDefinition().apply {
                name = LIST_ALARMS_TOOL
                description = "List all active alarms"
                parameters = emptyArray()
            },
            ToolDefinition().apply {
                name = CANCEL_ALARM_TOOL
                description = "Cancel a specific alarm by ID"
                parameters = arrayOf(ToolParameter().apply {
                    name = "alarm_id"
                    type = "string"
                    description = "ID of the alarm to cancel"
                    required = true
                    enumValues = emptyArray()
                })
            }
        )
    }

    private fun getCurrentTime(callback: IToolCallback, isLLM: Boolean) {
        val now = ZonedDateTime.now()

        if (isLLM) {
            val isoFormat = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val timezone = now.zone.toString()

            val result = """
            {
                "datetime_iso": "$isoFormat",
                "timezone": "$timezone"
            }
        """.trimIndent()

            callback.onSuccess(result)
        } else {
            val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

            callback.onSuccess("It is ${now.format(time)}")
        }
    }

    private fun createTimer(params: JSONObject?, callback: IToolCallback) {
        if (params == null) return callback.onError("Invalid parameters")

        try {
            val name = params.getString("name")
            val durationSeconds = params.getLong("duration_seconds")

            val timerId = "timer_${System.currentTimeMillis()}"
            val startTime = System.currentTimeMillis()
            val endTime = startTime + (durationSeconds * 1000)

            val timerData = TimerData(timerId, name, durationSeconds, startTime, endTime)
            saveTimer(timerData)

            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    handler.post {
                        val message = if (name.lowercase().trim() == "timer") {
                            // Don't say timer twice in a row
                            "Timer has finished!"
                        } else {
                            "Timer '$name' has finished!"
                        }
                        speakAlert(message)
                        removeTimer(timerId)
                    }
                }
            }, durationSeconds * 1000)

            activeTimers[timerId] = timer

            callback.onSuccess("""{"timer_id": "$timerId", "status": "created"}""")
        } catch (e: Exception) {
            callback.onError("Failed to create timer: ${e.message}")
        }
    }

    private fun listTimers(callback: IToolCallback) {
        val timers = getStoredTimers()
        val currentTime = System.currentTimeMillis()

        val activeList = timers.filter { currentTime < it.endTime }
        val jsonArray = JSONArray()

        activeList.forEach { timer ->
            val remaining = (timer.endTime - currentTime) / 1000
            jsonArray.put(JSONObject().apply {
                put("id", timer.id)
                put("name", timer.name)
                put("remaining_seconds", remaining)
            })
        }

        callback.onSuccess("""{"timers": $jsonArray}""")
    }

    private fun cancelTimer(params: JSONObject?, callback: IToolCallback) {
        if (params == null) return callback.onError("Invalid parameters")

        try {
            val timerId = params.getString("timer_id")

            activeTimers[timerId]?.cancel()
            activeTimers.remove(timerId)
            removeTimer(timerId)

            callback.onSuccess("""{"status": "cancelled"}""")
        } catch (e: Exception) {
            callback.onError("Failed to cancel timer: ${e.message}")
        }
    }

    private fun createAlarm(params: JSONObject?, callback: IToolCallback) {
        if (params == null) return callback.onError("Invalid parameters")

        try {
            val name = params.getString("name")
            val datetimeIso = params.getString("datetime_iso")

            val triggerTime = ZonedDateTime.parse(datetimeIso).toInstant().toEpochMilli()
            val alarmId = "alarm_${System.currentTimeMillis()}"

            val alarmData = AlarmData(alarmId, name, triggerTime)
            saveAlarm(alarmData)

            val delay = triggerTime - System.currentTimeMillis()
            if (delay > 0) {
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        handler.post {
                            val message = if (name.lowercase().trim() == "alarm") {
                                // Don't say timer twice in a row
                                "Alarm has finished!"
                            } else {
                                "Alrm '$name' has finished!"
                            }
                            speakAlert(message)
                            removeAlarm(alarmId)
                        }
                    }
                }, delay)

                activeAlarms[alarmId] = timer
            }

            callback.onSuccess("""{"alarm_id": "$alarmId", "status": "created"}""")
        } catch (e: Exception) {
            callback.onError("Failed to create alarm: ${e.message}")
        }
    }

    private fun listAlarms(callback: IToolCallback) {
        val alarms = getStoredAlarms()
        val currentTime = System.currentTimeMillis()

        val activeList = alarms.filter { currentTime < it.triggerTime }
        val jsonArray = JSONArray()

        activeList.forEach { alarm ->
            jsonArray.put(JSONObject().apply {
                put("id", alarm.id)
                put("name", alarm.name)
                put(
                    "trigger_time", ZonedDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(alarm.triggerTime),
                        java.time.ZoneId.systemDefault()
                    ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            })
        }

        callback.onSuccess("""{"alarms": $jsonArray}""")
    }

    private fun cancelAlarm(params: JSONObject?, callback: IToolCallback) {
        if (params == null) return callback.onError("Invalid parameters")

        try {
            val alarmId = params.getString("alarm_id")

            activeAlarms[alarmId]?.cancel()
            activeAlarms.remove(alarmId)
            removeAlarm(alarmId)

            callback.onSuccess("""{"status": "cancelled"}""")
        } catch (e: Exception) {
            callback.onError("Failed to cancel alarm: ${e.message}")
        }
    }

    private fun saveTimer(timer: TimerData) {
        val timers = getStoredTimers().toMutableList()
        timers.add(timer)
        val jsonArray = JSONArray()
        timers.forEach { t ->
            jsonArray.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("duration_seconds", t.durationSeconds)
                put("start_time", t.startTime)
                put("end_time", t.endTime)
            })
        }
        prefs.edit { putString("timers", jsonArray.toString()) }
    }

    private fun saveAlarm(alarm: AlarmData) {
        val alarms = getStoredAlarms().toMutableList()
        alarms.add(alarm)
        val jsonArray = JSONArray()
        alarms.forEach { a ->
            jsonArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("trigger_time", a.triggerTime)
                put("recurring", a.recurring)
            })
        }
        prefs.edit { putString("alarms", jsonArray.toString()) }
    }

    private fun getStoredTimers(): List<TimerData> {
        val timersJson = prefs.getString("timers", "[]") ?: "[]"
        val jsonArray = JSONArray(timersJson)
        val timers = mutableListOf<TimerData>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            timers.add(
                TimerData(
                    obj.getString("id"),
                    obj.getString("name"),
                    obj.getLong("duration_seconds"),
                    obj.getLong("start_time"),
                    obj.getLong("end_time")
                )
            )
        }
        return timers
    }

    private fun getStoredAlarms(): List<AlarmData> {
        val alarmsJson = prefs.getString("alarms", "[]") ?: "[]"
        val jsonArray = JSONArray(alarmsJson)
        val alarms = mutableListOf<AlarmData>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            alarms.add(
                AlarmData(
                    obj.getString("id"),
                    obj.getString("name"),
                    obj.getLong("trigger_time"),
                    obj.optBoolean("recurring", false)
                )
            )
        }
        return alarms
    }

    private fun removeTimer(timerId: String) {
        val timers = getStoredTimers().filter { it.id != timerId }
        val jsonArray = JSONArray()
        timers.forEach { t ->
            jsonArray.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("duration_seconds", t.durationSeconds)
                put("start_time", t.startTime)
                put("end_time", t.endTime)
            })
        }
        prefs.edit { putString("timers", jsonArray.toString()) }
        activeTimers.remove(timerId)
    }

    private fun removeAlarm(alarmId: String) {
        val alarms = getStoredAlarms().filter { it.id != alarmId }
        val jsonArray = JSONArray()
        alarms.forEach { a ->
            jsonArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("trigger_time", a.triggerTime)
                put("recurring", a.recurring)
            })
        }
        prefs.edit { putString("alarms", jsonArray.toString()) }
        activeAlarms.remove(alarmId)
    }

    private fun restoreTimersAndAlarms() {
        val currentTime = System.currentTimeMillis()

        // Restore active timers
        getStoredTimers().forEach { timer ->
            if (currentTime < timer.endTime) {
                val remaining = timer.endTime - currentTime
                val systemTimer = Timer()
                systemTimer.schedule(object : TimerTask() {
                    override fun run() {
                        handler.post {
                            speakAlert("Timer '${timer.name}' has finished!")
                            removeTimer(timer.id)
                        }
                    }
                }, remaining)
                activeTimers[timer.id] = systemTimer
            } else {
                removeTimer(timer.id)
            }
        }

        // Restore active alarms
        getStoredAlarms().forEach { alarm ->
            if (currentTime < alarm.triggerTime) {
                val delay = alarm.triggerTime - currentTime
                val systemTimer = Timer()
                systemTimer.schedule(object : TimerTask() {
                    override fun run() {
                        handler.post {
                            speakAlert("Alarm '${alarm.name}' is ringing!")
                            removeAlarm(alarm.id)
                        }
                    }
                }, delay)
                activeAlarms[alarm.id] = systemTimer
            } else {
                removeAlarm(alarm.id)
            }
        }
    }

    private fun speakAlert(message: String) {
        try {
            systemServices?.getTtsService()?.speakImmediately(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak alert: ${e.message}")
        }
    }
}
