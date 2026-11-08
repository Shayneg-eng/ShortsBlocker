package com.shayne.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches Instagram and YouTube. When the Reels / Shorts player is on screen,
 * it taps the Home tab (or presses Back) to get you out.
 *
 * Detection uses view IDs and tab labels from the apps' UI. Those can change
 * when the apps update; see README.md for how to find new ones.
 */
class ShortsBlockerService : AccessibilityService() {

    private var lastCheck = 0L
    private var lastExit = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != IG && pkg != YT) return

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MASTER, true)) return
        if (pkg == IG && !prefs.getBoolean(KEY_IG, true)) return
        if (pkg == YT && !prefs.getBoolean(KEY_YT, true)) return

        // Throttle: content-changed events fire constantly while scrolling.
        val now = System.currentTimeMillis()
        if (now - lastCheck < CHECK_INTERVAL_MS) return
        lastCheck = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val inShorts = if (pkg == IG) isInstagramReels(root) else isYouTubeShorts(root)
        if (!inShorts) return

        // Don't fire repeatedly while the exit animation is running.
        if (now - lastExit < EXIT_COOLDOWN_MS) return
        lastExit = now

        Log.d(TAG, "Blocking short-form video in $pkg")
        exitToHome(root, pkg)
    }

    override fun onInterrupt() = Unit

    // ---- Detection -------------------------------------------------------

    private fun isInstagramReels(root: AccessibilityNodeInfo): Boolean =
        anyNode(root) { n ->
            val id = n.viewIdResourceName.orEmpty()
            // Full-screen Reels pager. (Not "clips_tab", which is the bottom-bar button.)
            id.endsWith("clips_viewer_view_pager") ||
                id.endsWith("clips_viewer_fragment_container") ||
                id.endsWith("clips_viewer_recycler_view") ||
                // Bottom-bar Reels tab currently selected.
                (id.endsWith("clips_tab") && n.isSelected)
        }

    private fun isYouTubeShorts(root: AccessibilityNodeInfo): Boolean =
        anyNode(root) { n ->
            val id = n.viewIdResourceName.orEmpty()
            id.endsWith("reel_player_page_container") ||
                id.endsWith("reel_recycler") ||
                id.endsWith("shorts_container") ||
                // Bottom "Shorts" tab currently selected.
                (n.isSelected && n.contentDescription?.toString()
                    ?.equals("Shorts", ignoreCase = true) == true)
        }

    // ---- Exit ------------------------------------------------------------

    private fun exitToHome(root: AccessibilityNodeInfo, pkg: String) {
        val clicked = if (pkg == IG) {
            clickFirst(root) { n ->
                n.viewIdResourceName.orEmpty().endsWith("feed_tab")
            }
        } else {
            clickFirst(root) { n ->
                n.contentDescription?.toString()?.equals("Home", ignoreCase = true) == true
            }
        }
        if (!clicked) performGlobalAction(GLOBAL_ACTION_BACK)
    }

    // ---- Tree helpers ----------------------------------------------------

    private fun anyNode(
        node: AccessibilityNodeInfo?,
        depth: Int = 0,
        pred: (AccessibilityNodeInfo) -> Boolean,
    ): Boolean {
        if (node == null || depth > MAX_DEPTH) return false
        if (pred(node)) return true
        for (i in 0 until node.childCount) {
            if (anyNode(node.getChild(i), depth + 1, pred)) return true
        }
        return false
    }

    private fun clickFirst(
        root: AccessibilityNodeInfo,
        pred: (AccessibilityNodeInfo) -> Boolean,
    ): Boolean {
        val target = findNode(root, 0, pred) ?: return false
        var n: AccessibilityNodeInfo? = target
        while (n != null) {
            if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            n = n.parent
        }
        return false
    }

    private fun findNode(
        node: AccessibilityNodeInfo?,
        depth: Int,
        pred: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (node == null || depth > MAX_DEPTH) return null
        if (pred(node)) return node
        for (i in 0 until node.childCount) {
            findNode(node.getChild(i), depth + 1, pred)?.let { return it }
        }
        return null
    }

    companion object {
        const val PREFS = "shorts_blocker"
        const val KEY_MASTER = "master"
        const val KEY_IG = "instagram"
        const val KEY_YT = "youtube"

        private const val TAG = "ShortsBlocker"
        private const val IG = "com.instagram.android"
        private const val YT = "com.google.android.youtube"
        private const val MAX_DEPTH = 40
        private const val CHECK_INTERVAL_MS = 200L
        private const val EXIT_COOLDOWN_MS = 700L
    }
}
