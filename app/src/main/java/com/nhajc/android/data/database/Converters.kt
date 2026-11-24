package com.nhajc.android.data.database

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class Converters {
    @ColumnTypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @ColumnTypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @ColumnTypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}
