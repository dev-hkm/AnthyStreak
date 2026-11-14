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
    fun `structure selector covers all eight topologies across crop aspect ratios`() {
        assertEquals(WidgetStructure.MICRO_ROW, WidgetLayoutSpec.structureFor(68, 48))
        assertEquals(WidgetStructure.MICRO_STACK, WidgetLayoutSpec.structureFor(68, 72))
        assertEquals(WidgetStructure.STRIP_ROW, WidgetLayoutSpec.structureFor(180, 56))
        assertEquals(WidgetStructure.COMPACT_SPLIT, WidgetLayoutSpec.structureFor(140, 76))
        assertEquals(WidgetStructure.COLUMN, WidgetLayoutSpec.structureFor(68, 180))
        assertEquals(WidgetStructure.SQUARE, WidgetLayoutSpec.structureFor(140, 140))
        assertEquals(WidgetStructure.POSTER_STACK, WidgetLayoutSpec.structureFor(140, 140, WidgetStyle.POSTER))
        assertEquals(WidgetStructure.HERO, WidgetLayoutSpec.structureFor(240, 140))
    }

    @Test
    fun `missing launcher dimensions fall back to square default frame`() {
        val frames = WidgetLayoutSpec.responsiveFrames(
            launcherFrames = emptyList(),
            fallbackWidthDp = 0,
            fallbackHeightDp = 0
        )
        assertTrue(frames.contains(WidgetFrame(WidgetLayoutSpec.DEFAULT_WIDTH_DP, WidgetLayoutSpec.DEFAULT_HEIGHT_DP)))
        assertEquals(WidgetStructure.SQUARE, WidgetLayoutSpec.structureFor(0, 0))
    }

    @Test
    fun `responsiveFrames preserves exact launcher sizes without duplicating close anchors`() {
        val exact = listOf(WidgetFrame(140, 140), WidgetFrame(270, 140))
        val merged = WidgetLayoutSpec.responsiveFrames(
            launcherFrames = exact,
            fallbackWidthDp = 140,
            fallbackHeightDp = 140
        )
        assertEquals(WidgetFrame(140, 140), merged[0])
        assertEquals(WidgetFrame(270, 140), merged[1])
        assertFalse(merged.contains(WidgetFrame(139, 139)))
        assertEquals(WidgetFrame(270, 140), WidgetLayoutSpec.closestFrame(merged, 272, 138))
    }

    @Test
    fun `micro and digit scaling prevent clipping at 1x1 and 4-digit streaks`() {
        val tinyOneDigit = WidgetLayoutSpec.plan(
            widthDp = 56,
            heightDp = 48,
            style = WidgetStyle.FOCUS,
            hasChecklist = false,
            countDigits = 1
        )
        val tinyFourDigits = WidgetLayoutSpec.plan(
            widthDp = 56,
            heightDp = 48,
            style = WidgetStyle.FOCUS,
            hasChecklist = false,
            countDigits = 4
        )
        assertEquals(WidgetStructure.MICRO_ROW, tinyOneDigit.structure)
        assertFalse(tinyOneDigit.showName)
        assertFalse(tinyOneDigit.showAction)
        assertTrue(tinyFourDigits.countSp < tinyOneDigit.countSp)
        assertTrue(tinyFourDigits.flameSizeDp >= 24)
    }

    @Test
    fun `strip and tall column crops rearrange elements proportionally`() {
        val wideStrip = WidgetLayoutSpec.plan(260, 58, WidgetStyle.FOCUS, hasChecklist = false)
        assertEquals(WidgetStructure.STRIP_ROW, wideStrip.structure)
        assertTrue(wideStrip.showName)
        assertTrue(wideStrip.showAction)

        val tightStrip = WidgetLayoutSpec.plan(110, 48, WidgetStyle.FOCUS, hasChecklist = false)
        assertEquals(WidgetStructure.STRIP_ROW, tightStrip.structure)
        assertFalse(tightStrip.showAction)

        val tallColumn = WidgetLayoutSpec.plan(72, 220, WidgetStyle.FOCUS, hasChecklist = false)
        assertEquals(WidgetStructure.COLUMN, tallColumn.structure)
        assertTrue(tallColumn.useBalanceSpacers)
        assertTrue(tallColumn.showName)
        assertTrue(tallColumn.showAction)
    }

    @Test
    fun `style resolution respects ember poster checklist and long Vietnamese actions`() {
        val emberHero = WidgetLayoutSpec.plan(240, 140, WidgetStyle.EMBER, hasChecklist = false)
        assertFalse(emberHero.showName)
        assertFalse(emberHero.showAction)

        val posterSquare = WidgetLayoutSpec.plan(140, 140, WidgetStyle.POSTER, hasChecklist = false)
        assertEquals(WidgetStructure.POSTER_STACK, posterSquare.structure)
        assertTrue(posterSquare.showName)
        assertTrue(posterSquare.showAction)
        assertFalse(posterSquare.showProgress)

        val posterHero = WidgetLayoutSpec.plan(240, 140, WidgetStyle.POSTER, hasChecklist = true)
        assertEquals(WidgetStructure.HERO, posterHero.structure)
        assertTrue(posterHero.showName)
        assertTrue(posterHero.showAction)
        assertFalse(posterHero.showEyebrow)
        assertFalse(posterHero.showStatus)
        assertFalse(posterHero.showProgress)

        val checklistSquare = WidgetLayoutSpec.plan(140, 140, WidgetStyle.CHECKLIST, hasChecklist = true)
        assertTrue(checklistSquare.showProgress)

        val shortAction = WidgetLayoutSpec.plan(140, 140, WidgetStyle.FOCUS, hasChecklist = false, actionCharCount = 6)
        val longViAction = WidgetLayoutSpec.plan(140, 140, WidgetStyle.FOCUS, hasChecklist = false, actionCharCount = 16)
        assertTrue(longViAction.actionSp < shortAction.actionSp)

        assertEquals(WidgetStyle.FOCUS, resolvedStyle(240, 140, WidgetStyle.CHECKLIST, hasChecklist = false))
        assertEquals(WidgetStyle.CHECKLIST, resolvedStyle(240, 140, WidgetStyle.CHECKLIST, hasChecklist = true))
        assertEquals(WidgetStyle.POSTER, resolvedStyle(240, 140, WidgetStyle.POSTER, hasChecklist = false))
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
