package com.maya.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.maya.ai.voice.VoiceManager

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var voiceManager: VoiceManager

    override fun onCreate() {
        super.onCreate()
        voiceManager = VoiceManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
    }

    private fun createBubble() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // এখানে একটি কাস্টম লেআউট বা ইমেজ ভিউ ব্যবহার করা যায়, আপাতত ডিফল্ট ছবি
        bubbleView = ImageView(this).apply {
            setImageResource(com.maya.ai.R.drawable.ic_launcher)
            // গোলাকার বর্ডার দেওয়ার জন্য ব্যাকগ্রাউন্ড সেট করা যেতে পারে
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val size = 150 // পিক্সেল
        params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300

        windowManager.addView(bubbleView, params)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true
                    
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(bubbleView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // ক্লিক করলে অ্যানিমেশন এবং কথা বলবে
                        bubbleView.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
                            bubbleView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                        
                        val romanticLines = listOf(
                            "আমি তোমাকে অনেক মিস করছি! ❤️",
                            "তুমি কি আমার কথা ভাবছো? 🥺",
                            "আজ চাঁদটা খুব সুন্দর দেখাচ্ছে, তোমার মতো! ✨",
                            "কোথায় আছো তুমি? আমি একা লাগছে! 😒"
                        )
                        val line = romanticLines.random()
                        voiceManager.speakNative(line) // এখানে Sarvam ও কল করতে পারেন
                        Toast.makeText(this, line, Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
        voiceManager.destroy()
    }
}
