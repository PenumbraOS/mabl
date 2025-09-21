package com.penumbraos.plugins.aipinsystem

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolService
import com.penumbraos.sdk.PenumbraClient
import org.json.JSONObject

private const val TAG = "BatteryToolService"

private const val GET_BATTERY_LEVELS_TOOL = "get_battery_levels"

class BatteryToolService : ToolService("BatteryToolService") {

    private lateinit var client: PenumbraClient

    override fun onCreate() {
        super.onCreate()
        client = PenumbraClient(this@BatteryToolService)
    }

    override fun executeTool(call: ToolCall, params: JSONObject?, callback: IToolCallback) {
        when (call.name) {
            GET_BATTERY_LEVELS_TOOL -> getBatteryLevels(callback)
            else -> callback.onError("Unknown tool: ${call.name}")
        }
    }

    override fun getToolDefinitions(): Array<ToolDefinition> {
        return arrayOf(
            ToolDefinition().apply {
                name = GET_BATTERY_LEVELS_TOOL
                description =
                    "Get battery levels and status for main and expansion batteries. The expansion battery is called the 'booster'"
                parameters = emptyArray()
            }
        )
    }

    private fun getBatteryLevels(callback: IToolCallback) {
        try {
            val boosterInfo = client.accessory.getBatteryInfo()
            val boosterBatteryIsConnected = boosterInfo?.isConnected == true
            val boosterBatteryLevel = if (boosterBatteryIsConnected) {
                boosterInfo.batteryLevel.toFloat()
            } else 0f

            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            val internalBatteryLevel = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            } ?: 0f

//            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
//            val isCharging =
//                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val combinedBattery =
                if (boosterInfo?.isConnected == true && boosterBatteryLevel >= 0) {
                    (internalBatteryLevel + boosterBatteryLevel) / 2
                } else {
                    internalBatteryLevel
                }

            val result = JSONObject().apply {
                put("main_battery_percent", internalBatteryLevel)
                if (boosterBatteryIsConnected) {
                    put("booster_battery_connected", true)
                    put("booster_battery_percent", boosterBatteryLevel)
                    put("combined_level_percent", combinedBattery)
                }
                // TODO: Not sure how this is calculated. Booster seems to report charging if it's charging the Pin?
//                put("is_charging", boosterInfo?.isCharging == true)
            }

            Log.d(TAG, "Battery levels: $result")

            callback.onSuccess(result.toString())
        } catch (e: Exception) {
            callback.onError("Failed to get battery levels: ${e.message}")
        }
    }
}