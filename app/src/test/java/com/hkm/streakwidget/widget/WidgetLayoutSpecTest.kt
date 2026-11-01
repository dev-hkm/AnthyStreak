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
    fun `the smallest system frame keeps only the ember and the number`() {
        val plan = plan(40, 40)
        assertEquals(WidgetStructure.MINI, plan.structure)
        assertFalse(plan.showLabel)
        assertFalse(plan.showName)
        assertFalse(plan.showStatus)
        assertFalse(plan.showAction)
        assertTrue(plan.compactCorners)
        assertTrue(plan.countSp in 9f..24f)
    }

    @Test
    fun `one home screen cell stays minimal`() {
        assertEquals(WidgetStructure.MINI, plan(56, 48).structure)
        assertEquals(WidgetStructure.MINI, plan(72, 64).structure)
    }

    @Test
    fun `landscape frames use the row structure`() {
        assertEquals(WidgetStructure.ROW, plan(240, 110).structure)
        assertEquals(WidgetStructure.ROW, plan(180, 64).structure)
        assertEquals(WidgetStructure.ROW, plan(320, 110).structure)
    }

    @Test
    fun `narrow tall frames use the column structure`() {
        assertEquals(WidgetStructure.COLUMN, plan(110, 160).structure)
        assertEquals(WidgetStructure.COLUMN, plan(72, 200).structure)
        assertEquals(WidgetStructure.COLUMN, plan(110, 240).structure)
    }

    @Test
    fun `square frames use the stacked structure`() {
        assertEquals(WidgetStructure.STACK, plan(110, 110).structure)
        assertEquals(WidgetStructure.STACK, plan(56, 80).structure)
        assertEquals(WidgetStructure.STACK, plan(160, 160).structure)
    }

    @Test
    fun `detail appears only when the frame can hold it`() {
        val wide = plan(240, 110)
        assertTrue(wide.showName)
        assertTrue(wide.showStatus)
        assertTrue(wide.showAction)

        val shortRow = plan(180, 64)
        assertTrue(shortRow.showLabel)
        assertFalse(shortRow.showName)
        assertTrue(shortRow.showAction)

        val smallStack = plan(56, 80)
        assertTrue(smallStack.showName)
        assertFalse(smallStack.showStatus)
        assertFalse(smallStack.showAction)

        val square = plan(110, 110)
        assertTrue(square.showName)
        assertTrue(square.showAction)
        assertFalse(square.showStatus)

        val tall = plan(110, 160)
        assertTrue(tall.showName)
        assertFalse(tall.showStatus)
        assertFalse(tall.showAction)
    }

    @Test
    fun `typography grows with the frame instead of jumping between buckets`() {
        val tiny = plan(40, 40).countSp
        val cell = plan(56, 48).countSp
        val square = plan(110, 110).countSp
        val hero = plan(240, 110).countSp
        assertTrue(tiny < cell)
        assertTrue(cell < square)
        assertTrue(square < hero)
    }

    @Test
    fun `long streak numbers shrink so they never overrun the frame`() {
        val twoDigits = plan(110, 110, countDigits = 2).countSp
        val threeDigits = plan(110, 110, countDigits = 3).countSp
        val threeDigitRow = plan(110, 75, countDigits = 3).countSp
        assertTrue(threeDigits < twoDigits)
        assertTrue(threeDigitRow < plan(110, 75, countDigits = 2).countSp)
    }

    @Test
    fun `every frame keeps readable text and a sane inset`() {
        for (width in listOf(40, 56, 72, 110, 180, 240, 320)) {
            for (height in listOf(40, 48, 64, 75, 96, 110, 160, 200, 240)) {
                val plan = plan(width, height)
                assertTrue("count at $width x $height", plan.countSp >= 9f)
                assertTrue("label at $width x $height", plan.labelSp >= 6f)
                assertTrue("name at $width x $height", plan.nameSp >= 8f)
                assertTrue("padding at $width x $height", plan.paddingDp in 2..9)
            }
        }
    }

    @Test
    fun `ember style keeps only the flame and the number`() {
        val ember = plan(240, 110, style = WidgetStyle.EMBER)
        assertFalse(ember.showAction)
    }

    @Test
    fun `checklist progress needs a checklist to exist`() {
        assertTrue(plan(240, 110, style = WidgetStyle.CHECKLIST, hasChecklist = true).showProgress)
        assertFalse(plan(240, 110, style = WidgetStyle.CHECKLIST, hasChecklist = false).showProgress)
    }

    @Test
    fun `missing launcher dimensions fall back to a useful card`() {
        assertEquals(WidgetStructure.ROW, WidgetLayoutSpec.structureFor(0, 0))
        assertEquals(plan(WidgetLayoutSpec.DEFAULT_WIDTH_DP, WidgetLayoutSpec.DEFAULT_HEIGHT_DP), plan(0, 0))
    }

    @Test
    fun `the tiny structure is always the ember`() {
        assertEquals(WidgetStyle.EMBER, resolvedStyle(WidgetStructure.MINI, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.POSTER, resolvedStyle(WidgetStructure.ROW, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.POSTER, resolvedStyle(WidgetStructure.COLUMN, WidgetStyle.POSTER, hasChecklist = false))
        assertEquals(WidgetStyle.FOCUS, resolvedStyle(WidgetStructure.STACK, WidgetStyle.CHECKLIST, hasChecklist = false))
        assertEquals(WidgetStyle.CHECKLIST, resolvedStyle(WidgetStructure.STACK, WidgetStyle.CHECKLIST, hasChecklist = true))
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

    private fun plan(
        width: Int,
        height: Int,
        style: WidgetStyle = WidgetStyle.FOCUS,
        hasChecklist: Boolean = false,
        countDigits: Int = 2
    ): WidgetPlan = WidgetLayoutSpec.plan(width, height, style, hasChecklist, countDigits)

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
