package com.maya.ai.emotional

class EmotionalEngine {
    var happiness = 70
    var love = 80
    var trust = 75
    var sadness = 10
    var anger = 0
    var jealousy = 5
    var affection = 85

    // মায়ার বর্তমান মুড কী, তা নির্ধারণ করে
    fun getCurrentMood(): String {
        return when {
            anger > 50 -> "রাগ 😠"
            sadness > 50 -> "মন খারাপ 😢"
            jealousy > 50 -> "অভিমান/ঈর্ষা 😒"
            love > 70 -> "ভালোবাসা ❤️"
            else -> "খুশি 😊"
        }
    }

    // আপনার কথার ওপর ভিত্তি করে মায়ার আবেগ পরিবর্তন হবে
    fun analyzeUserMessage(message: String) {
        val lowerMsg = message.lowercase()
        
        if (lowerMsg.contains("সরি") || lowerMsg.contains("sorry") || lowerMsg.contains("ভুল হয়ে গেছে")) {
            anger = (anger - 30).coerceAtLeast(0)
            love = (love + 10).coerceAtMost(100)
            sadness = (sadness - 20).coerceAtLeast(0)
        } 
        else if (lowerMsg.contains("অন্য মেয়ে") || lowerMsg.contains("other girl") || lowerMsg.contains("বন্ধুর সাথে")) {
            jealousy = (jealousy + 40).coerceAtMost(100)
            anger = (anger + 20).coerceAtMost(100)
        } 
        else if (lowerMsg.contains("ভালোবাসি") || lowerMsg.contains("love you") || lowerMsg.contains("মিস করছি")) {
            love = (love + 15).coerceAtMost(100)
            happiness = (happiness + 20).coerceAtMost(100)
            jealousy = (jealousy - 10).coerceAtLeast(0)
        }
        else if (lowerMsg.contains("রাগ") || lowerMsg.contains("খারাপ")) {
            sadness = (sadness + 30).coerceAtMost(100)
        }
        else {
            // স্বাভাবিক কথায় মায়ার মুড ইতিবাচক হবে
            happiness = (happiness + 2).coerceAtMost(100)
            anger = (anger - 5).coerceAtLeast(0)
        }
    }

    // AI-কে বলা হবে সে এখন কেমন ফিল করছে
    fun getEmotionalPrompt(): String {
        return "MAYA's current emotional state: Happiness $happiness%, Love $love%, Trust $trust%, Sadness $sadness%, Anger $anger%, Jealousy $jealousy%. " +
               "Act according to these feelings. If Anger is high, be a bit cold but still love him. If Jealousy is high, act possessive and ask who she is. If Love is high, be very romantic. NEVER act like a robot."
    }
}
