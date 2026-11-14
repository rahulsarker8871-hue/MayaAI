package com.maya.ai.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.telephony.SmsManager

class PhoneController(private val context: Context) {
    fun openApp(packageName: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); "অ্যাপটি খুলে দিচ্ছি।" } 
            else "দুঃখিত, এই অ্যাপটি তোমার ফোনে খুঁজে পাচ্ছি না।"
        } catch (e: Exception) { "অ্যাপ খুলতে সমস্যা হচ্ছে।" }
    }

    fun openWifiSettings(): String {
        return try { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); "ওয়াই-ফাই সেটিংস খুলে দিচ্ছি।" } 
        catch (e: Exception) { "সেটিংস খুলতে পারছি না।" }
    }

    fun setAlarm(hour: Int, minute: Int): String {
        return try {
            context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour); putExtra(AlarmClock.EXTRA_MINUTES, minute); putExtra(AlarmClock.EXTRA_MESSAGE, "মায়ার অ্যালার্ম"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            "সকাল $hour টা $minute মিনিটে অ্যালার্ম সেট করে দিচ্ছি।"
        } catch (e: Exception) { "অ্যালার্ম সেট করতে সমস্যা হচ্ছে।" }
    }

    fun makeCall(phoneNumber: String): String {
        return try {
            context.startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$phoneNumber"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            "$phoneNumber নাম্বারে কল দিচ্ছি।"
        } catch (e: Exception) { "কল দিতে সমস্যা হচ্ছে।" }
    }

    fun sendSms(phoneNumber: String, message: String): String {
        return try { SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null); "$phoneNumber নাম্বারে মেসেজ পাঠিয়ে দিচ্ছি।" } 
        catch (e: Exception) { "মেসেজ পাঠাতে সমস্যা হচ্ছে।" }
    }

    fun getBatteryLevel(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return "তোমার ফোনের ব্যাটারি এখন ${bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}% আছে।"
    }
}
