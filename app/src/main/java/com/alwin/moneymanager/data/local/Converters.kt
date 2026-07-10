package com.alwin.moneymanager.data.local

import androidx.room.TypeConverter
import com.alwin.moneymanager.data.local.entity.EmiFrequency

/** Room type converters. [EmiFrequency] is stored as its enum name so the column stays readable
 * and matches the JSON backup encoding (kotlinx serialization also emits the name). */
class Converters {
    @TypeConverter
    fun fromEmiFrequency(value: EmiFrequency): String = value.name

    @TypeConverter
    fun toEmiFrequency(value: String): EmiFrequency =
        runCatching { EmiFrequency.valueOf(value) }.getOrDefault(EmiFrequency.MONTHLY)
}
