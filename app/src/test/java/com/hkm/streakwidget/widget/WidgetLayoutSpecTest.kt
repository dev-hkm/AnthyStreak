package com.hkm.streakwidget.widget

import android.content.SharedPreferences
import com.hkm.streakwidget.data.HabitRepository
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutSpecTest {
    @Test
    fun `classifier exposes all eight exact responsive buckets`() {
        assertEquals(WidgetSize.TILE, WidgetLayoutSpec.classify(56, 56))
        assertEquals(WidgetSize.STRIP, WidgetLayoutSpec.classify(112, 48))
        assertEquals(WidgetSize.TALL, WidgetLayoutSpec.classify(72, 200))
        assertEquals(WidgetSize.COMPACT, WidgetLayoutSpec.classify(112, 88))
        assertEquals(WidgetSize.WIDE, WidgetLayoutSpec.classify(240, 88))
        assertEquals(WidgetSize.SQUARE, WidgetLayoutSpec.classify(112, 120))
        assertEquals(WidgetSize.TALL_HERO, WidgetLayoutSpec.classify(112, 168))
        assertEquals(WidgetSize.HERO, WidgetLayoutSpec.classify(240, 168))
    }

    @Test
    fun `classifier handles tall and wide dimensions independently`() {
        assertEquals(WidgetSize.TALL_HERO, WidgetLayoutSpec.classify(112, 240))
        assertEquals(WidgetSize.WIDE, WidgetLayoutSpec.classify(320, 88))
        assertEquals(WidgetSize.STRIP, WidgetLayoutSpec.classify(240, 60))
        assertEquals(WidgetSize.TALL, WidgetLayoutSpec.classify(72, 96))
        assertEquals(WidgetSize.TILE, WidgetLayoutSpec.classify(72, 64))
    }

    @Test
    fun `responsive variants cover every bucket once`() {
        assertEquals(WidgetSize.entries.toSet(), WidgetLayoutSpec.responsiveSizes.toSet())
        assertEquals(WidgetSize.entries.size, WidgetLayoutSpec.responsiveSizes.size)
    }

    @Test
    fun `missing launcher dimensions use a useful compact fallback`() {
        assertEquals(WidgetSize.COMPACT, WidgetLayoutSpec.classify(0, 0))
    }

    @Test
    fun `poster selection falls back to ember for sizes without a detail hierarchy`() {
        assertEquals(WidgetStyle.EMBER, resolvedStyle(WidgetSize.TILE, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.EMBER, resolvedStyle(WidgetSize.STRIP, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.POSTER, resolvedStyle(WidgetSize.WIDE, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.POSTER, resolvedStyle(WidgetSize.HERO, WidgetStyle.POSTER, hasChecklist = false))
    }

    @Test
    fun `styles fall back only where their hierarchy cannot be rendered`() {
        assertEquals(WidgetStyle.POSTER, resolvedStyle(WidgetSize.SQUARE, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.POSTER, resolvedStyle(WidgetSize.HERO, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.FOCUS, resolvedStyle(WidgetSize.HERO, WidgetStyle.CHECKLIST, hasChecklist = false))
        assertEquals(WidgetStyle.CHECKLIST, resolvedStyle(WidgetSize.HERO, WidgetStyle.CHECKLIST, hasChecklist = true))
    }

    @Test
    fun `stored style parser defaults to focus and rejects invalid values`() {
        assertEquals(WidgetStyle.FOCUS, WidgetStyle.fromStored(null))
        assertEquals(WidgetStyle.EMBER, WidgetStyle.fromStored("EMBER"))
        assertEquals(WidgetStyle.CHECKLIST, WidgetStyle.fromStored("checklist"))
        assertEquals(WidgetStyle.FOCUS, WidgetStyle.fromStored("neon-rainbow"))
    }

    @Test
    fun `repository saves style and invalid stored values fall back to focus`() {
        val preferences = memoryPreferences()
        val repository = HabitRepository(preferences)

        assertEquals(WidgetStyle.FOCUS, repository.getWidgetStyle(41))
        repository.saveWidgetStyle(41, WidgetStyle.POSTER)
        assertEquals(WidgetStyle.POSTER, repository.getWidgetStyle(41))

        preferences.edit().putString("widget_style_41", "BROKEN").apply()
        assertEquals(WidgetStyle.FOCUS, repository.getWidgetStyle(41))
    }

    @Test
    fun `removing widget state also removes its persisted style`() {
        val preferences = memoryPreferences()
        val repository = HabitRepository(preferences)
        repository.saveWidgetHabitId(52, "goal")
        repository.saveWidgetStyle(52, WidgetStyle.CHECKLIST)
        val token = repository.widgetActionToken(52)

        assertTrue(preferences.contains("widget_style_52"))
        assertTrue(repository.isValidWidgetAction(52, "goal", token))

        repository.removeWidgetState(52)

        assertFalse(preferences.contains("widget_style_52"))
        assertEquals(WidgetStyle.FOCUS, repository.getWidgetStyle(52))
        assertFalse(repository.isValidWidgetAction(52, "goal", token))
    }

    private fun memoryPreferences(): SharedPreferences {
        val values = linkedMapOf<String, Any?>()
        lateinit var preferences: SharedPreferences
        lateinit var editor: SharedPreferences.Editor

        editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "putString", "putStringSet", "putInt", "putLong", "putFloat", "putBoolean" -> {
                    values[args!![0] as String] = args[1]
                    editor
                }
                "remove" -> {
                    values.remove(args!![0] as String)
                    editor
                }
                "clear" -> {
                    values.clear()
                    editor
                }
                "commit" -> true
                "apply" -> null
                "toString" -> "MemoryPreferences.Editor"
                else -> error("Unsupported editor method: ${method.name}")
            }
        } as SharedPreferences.Editor

        preferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when (method.name) {
                "getAll" -> values.toMap()
                "getString" -> values[key] as? String ?: args!![1] as? String
                "getStringSet" -> values[key] as? Set<*> ?: args!![1]
                "getInt" -> values[key] as? Int ?: args!![1]
                "getLong" -> values[key] as? Long ?: args!![1]
                "getFloat" -> values[key] as? Float ?: args!![1]
                "getBoolean" -> values[key] as? Boolean ?: args!![1]
                "contains" -> values.containsKey(key)
                "edit" -> editor
                "registerOnSharedPreferenceChangeListener", "unregisterOnSharedPreferenceChangeListener" -> null
                "toString" -> "MemoryPreferences"
                else -> error("Unsupported preferences method: ${method.name}")
            }
        } as SharedPreferences
        return preferences
    }
}
