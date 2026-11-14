package com.maya.ai.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.telephony.SmsManager
import android.view.KeyEvent

class PhoneController(private val context: Context) {

    // মিউজিক প্লে/পজ করার ফাংশন (YouTube, Spotify সবখানে কাজ করবে)
    fun togglePlayPause(): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            "গান প্লে বা পজ করে দিলাম।"
        } catch (e: Exception) { "মিউজিক কন্ট্রোল করতে পারছি না।" }
    }

    // নেক্সট গানে যাওয়া
    fun nextTrack(): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
            "নেক্সট গান চালু করলাম।"
        } catch (e: Exception) { "গান চেঞ্জ করতে পারছি না।" }
    }

    // আগের গানে যাওয়া
    fun previousTrack(): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            "আগের গান চালু করলাম।"
        } catch (e: Exception) { "গান চেঞ্জ করতে পারছি না।" }
    }

    // হোয়াটসঅ্যাপ চ্যাট ওপেন করা
    fun openWhatsAppChat(phoneNumber: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://wa.me/$phoneNumber")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "হোয়াটসঅ্যাপে চ্যাট ওপেন করে দিচ্ছি।"
        } catch (e: Exception) { "হোয়াটসঅ্যাপ খুলতে পারছি না।" }
    }

    // অ্যাপের নাম দিয়ে সার্চ
    private fun findPackageByAppName(appName: String): String? {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
        for (app in apps) {
            val label = app.loadLabel(pm).toString().lowercase()
            if (label.contains(appName.lowercase())) return app.activityInfo.packageName
        }
        return null
    }

    fun openApp(appNameOrPackage: String): String {
        var packageName = appNameOrPackage.trim()
        if (!packageName.contains(".")) {
            val foundPackage = findPackageByAppName(packageName)
            packageName = if (foundPackage != null) foundPackage else {
                when(packageName.lowercase()) {
                    "youtube", "ইউটিউব" -> "com.google.android.youtube"
                    "whatsapp", "হোয়াটসঅ্যাপ" -> "com.whatsapp"
                    "facebook", "ফেসবুক" -> "com.facebook.katana"
                    "spotify", "স্পটিফাই" -> "com.spotify.music"
                    "chrome", "ক্রোম" -> "com.android.chrome"
                    else -> packageName
                }
            }
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); "অ্যাপটি খুলে দিচ্ছি।" } 
            else "দুঃখিত, '$appNameOrPackage' নামে কোনো অ্যাপ তোমার ফোনে খুঁজে পাচ্ছি না।"
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
