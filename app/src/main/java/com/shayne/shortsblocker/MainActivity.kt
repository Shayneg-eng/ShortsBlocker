package com.shayne.shortsblocker

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/** One simple screen: on/off switches and a shortcut to enable the service. */
class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(ShortsBlockerService.PREFS, Context.MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        root.addView(TextView(this).apply {
            text = "Shorts Blocker"
            textSize = 28f
        })

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, pad / 2, 0, pad)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        fun toggle(label: String, key: String) {
            root.addView(Switch(this).apply {
                text = label
                textSize = 18f
                isChecked = prefs.getBoolean(key, true)
                setPadding(0, pad, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
                setOnCheckedChangeListener { _, on ->
                    prefs.edit().putBoolean(key, on).apply()
                }
            })
        }
        toggle("Blocking enabled", ShortsBlockerService.KEY_MASTER)
        toggle("Instagram Reels", ShortsBlockerService.KEY_IG)
        toggle("YouTube Shorts", ShortsBlockerService.KEY_YT)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled()) {
            "Service is ON. Reels and Shorts will be blocked."
        } else {
            "Service is OFF. Tap below, find \"Shorts Blocker\", and turn it on."
        }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, ShortsBlockerService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
