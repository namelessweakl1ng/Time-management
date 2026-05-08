package com.yourapp.timemanagement.domain

import java.time.DayOfWeek
import java.time.LocalDate

class RecurrencePlanner {
    fun dates(rule: RecurrenceRule, start: LocalDate, count: Int): List<LocalDate> {
        if (count <= 0) return emptyList()
        val result = mutableListOf<LocalDate>()
        var cursor = start
        while (result.size < count) {
            when (rule) {
                RecurrenceRule.None -> {
                    result += start
                    return result
                }
                RecurrenceRule.Daily -> {
                    result += cursor
                    cursor = cursor.plusDays(1)
                }
                RecurrenceRule.Weekdays -> {
                    if (cursor.dayOfWeek != DayOfWeek.SATURDAY && cursor.dayOfWeek != DayOfWeek.SUNDAY) {
                        result += cursor
                    }
                    cursor = cursor.plusDays(1)
                }
                RecurrenceRule.Weekly -> {
                    result += cursor
                    cursor = cursor.plusWeeks(1)
                }
            }
        }
        return result
    }
}
