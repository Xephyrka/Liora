package com.xephyrka.liora.data.local

import androidx.room.TypeConverter
import com.xephyrka.liora.data.model.ItemType
import com.xephyrka.liora.data.model.RecurrenceUnit

/**
 * Type converters for Room to handle custom data types like Enums.
 */
class Converters {
    @TypeConverter
    fun fromItemType(value: ItemType): String {
        return value.name
    }

    @TypeConverter
    fun toItemType(value: String): ItemType {
        return try {
            ItemType.valueOf(value)
        } catch (e: Exception) {
            ItemType.TASK
        }
    }

    @TypeConverter
    fun fromRecurrenceUnit(value: RecurrenceUnit): String {
        return value.name
    }

    @TypeConverter
    fun toRecurrenceUnit(value: String): RecurrenceUnit {
        return try {
            RecurrenceUnit.valueOf(value)
        } catch (e: Exception) {
            RecurrenceUnit.DAY
        }
    }
}
