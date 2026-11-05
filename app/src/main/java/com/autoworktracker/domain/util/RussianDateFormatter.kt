package com.autoworktracker.domain.util

import com.autoworktracker.domain.model.AppLanguage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale

object RussianDateFormatter {
    val monthsGenitive: List<String> = listOf(
        "января",
        "февраля",
        "марта",
        "апреля",
        "мая",
        "июня",
        "июля",
        "августа",
        "сентября",
        "октября",
        "ноября",
        "декабря",
    )

    val monthsNominative: List<String> = listOf(
        "Январь",
        "Февраль",
        "Март",
        "Апрель",
        "Май",
        "Июнь",
        "Июль",
        "Август",
        "Сентябрь",
        "Октябрь",
        "Ноябрь",
        "Декабрь",
    )

    private data class MonthForms(
        val nominative: List<String>,
        val genitive: List<String>,
        val phrase: List<String>,
    )

    private val forms: Map<AppLanguage, MonthForms> = mapOf(
        AppLanguage.RU to MonthForms(
            nominative = monthsNominative,
            genitive = monthsGenitive,
            phrase = monthsNominative.map { it.lowercase(Locale.ROOT) },
        ),
        AppLanguage.UK to MonthForms(
            nominative = listOf(
                "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
                "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень",
            ),
            genitive = listOf(
                "січня", "лютого", "березня", "квітня", "травня", "червня",
                "липня", "серпня", "вересня", "жовтня", "листопада", "грудня",
            ),
            phrase = listOf(
                "січень", "лютий", "березень", "квітень", "травень", "червень",
                "липень", "серпень", "вересень", "жовтень", "листопад", "грудень",
            ),
        ),
        AppLanguage.CS to MonthForms(
            nominative = listOf(
                "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
                "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec",
            ),
            genitive = listOf(
                "ledna", "února", "března", "dubna", "května", "června",
                "července", "srpna", "září", "října", "listopadu", "prosince",
            ),
            phrase = listOf(
                "leden", "únor", "březen", "duben", "květen", "červen",
                "červenec", "srpen", "září", "říjen", "listopad", "prosinec",
            ),
        ),
        AppLanguage.EN to MonthForms(
            nominative = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December",
            ),
            genitive = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December",
            ),
            phrase = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December",
            ),
        ),
    )

    private val nominativeLookup: Map<String, Int> = buildLookup { it.nominative }

    private val genitiveLookup: Map<String, Int> = buildLookup { it.genitive }

    fun monthGenitive(month: Int, language: AppLanguage = AppLanguage.RU): String =
        forms.getValue(language).genitive[month - 1]

    fun monthNominative(month: Int, language: AppLanguage = AppLanguage.RU): String =
        forms.getValue(language).nominative[month - 1]

    fun monthAccusative(month: Int, language: AppLanguage = AppLanguage.RU): String =
        forms.getValue(language).phrase[month - 1]

    fun monthIndexFromNominative(value: String): Int? = nominativeLookup[value.lowercase(Locale.ROOT)]

    fun monthIndexFromGenitive(value: String): Int? = genitiveLookup[value.lowercase(Locale.ROOT)]

    fun dayMonth(date: LocalDate, language: AppLanguage = AppLanguage.RU): String {
        return when (language) {
            AppLanguage.CS -> "${date.dayOfMonth}. ${monthGenitive(date.monthValue, language)}"
            AppLanguage.EN -> "${monthNominative(date.monthValue, language)} ${date.dayOfMonth}"
            AppLanguage.RU, AppLanguage.UK -> "${date.dayOfMonth} ${monthGenitive(date.monthValue, language)}"
        }
    }

    fun fullDate(date: LocalDate, language: AppLanguage = AppLanguage.RU): String {
        return when (language) {
            AppLanguage.CS -> "${date.dayOfMonth}. ${monthGenitive(date.monthValue, language)} ${date.year}"
            AppLanguage.EN -> "${monthNominative(date.monthValue, language)} ${date.dayOfMonth}, ${date.year}"
            AppLanguage.RU, AppLanguage.UK -> "${dayMonth(date, language)} ${date.year}"
        }
    }

    fun monthTitle(yearMonth: YearMonth, language: AppLanguage = AppLanguage.RU): String =
        "${monthNominative(yearMonth.monthValue, language)} ${yearMonth.year}"

    fun monthHoursLine(hours: Double, month: Int, language: AppLanguage = AppLanguage.RU): String {
        val word = HoursFormatter.hourWord(hours, language)
        val name = monthAccusative(month, language)
        return when (language) {
            AppLanguage.RU, AppLanguage.UK -> "$word за $name"
            AppLanguage.CS -> "$word za $name"
            AppLanguage.EN -> "$word in $name"
        }
    }

    fun recordLine(date: LocalDate, hours: Double, language: AppLanguage = AppLanguage.RU): String =
        "${dayMonth(date, language)}: ${HoursFormatter.formatHours(hours, language)}"

    fun formatUpdated(dateTime: LocalDateTime, language: AppLanguage = AppLanguage.RU): String {
        val clock = formatTime(dateTime.toLocalTime())
        val date = when (language) {
            AppLanguage.CS -> "${dateTime.dayOfMonth}. ${monthGenitive(dateTime.monthValue, language)} ${dateTime.year}"
            AppLanguage.EN -> "${monthNominative(dateTime.monthValue, language)} ${dateTime.dayOfMonth}, ${dateTime.year}"
            AppLanguage.RU, AppLanguage.UK ->
                "${dateTime.dayOfMonth} ${monthGenitive(dateTime.monthValue, language)} ${dateTime.year}"
        }
        return "$date, $clock"
    }

    fun formatTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

    private fun buildLookup(select: (MonthForms) -> List<String>): Map<String, Int> {
        return AppLanguage.entries
            .flatMap { language ->
                select(forms.getValue(language)).mapIndexed { index, name ->
                    name.lowercase(Locale.ROOT) to index + 1
                }
            }
            .toMap()
    }
}
