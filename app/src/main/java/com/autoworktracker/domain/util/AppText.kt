package com.autoworktracker.domain.util

import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.GeofenceStatus
import com.autoworktracker.domain.model.ShiftRule

class AppText(
    val navShifts: String,
    val navSettings: String,
    val addShift: String,
    val shiftHistory: String,
    val restoreTitle: String,
    val restoreBody: String,
    val chooseFile: String,
    val later: String,
    val copyReport: String,
    val shareFile: String,
    val refreshDistance: String,
    val backupActive: String,
    val backupWaiting: String,
    val ecoMode: String,
    val geofenceOff: String,
    val previousMonth: String,
    val nextMonth: String,
    val othersLabel: String,
    val noShiftsYet: String,
    val toPayPrefix: String,
    val emptyMonthTitle: String,
    val emptyMonthBody: String,
    val delete: String,
    val entryPrefix: String,
    val exitPrefix: String,
    val shortShift: String,
    val longShift: String,
    val workplaceMissing: String,
    val workingNow: String,
    val waitingFifteen: String,
    val sleepingGeofence: String,
    val waitingGps: String,
    val allowLocationDistance: String,
    val setWorkplace: String,
    val insidePrefix: String,
    val toWorkSuffix: String,
    val workplaceRadiusPattern: (Int) -> String,
    val noLocationPermission: String,
    val distanceUnavailable: String,
    val radiusPending: String,
    val enteredPrefix: String,
    val hoursOutOfRange: String,
    val shiftSaved: String,
    val monthCopied: String,
    val restoredPrefix: String,
    val backupUnreadable: String,
    val clipboardTitle: String,
    val settingsTitle: String,
    val settingsSubtitle: String,
    val workplaceSection: String,
    val openMap: String,
    val workplaceHint: String,
    val locatingGps: String,
    val suggestMyPlace: String,
    val nameLabel: String,
    val latitude: String,
    val longitude: String,
    val savePoint: String,
    val rulesSection: String,
    val overwriteTitle: String,
    val overwriteHint: String,
    val addRule: String,
    val paySection: String,
    val ratePattern: (String) -> String,
    val saveRate: String,
    val backupSection: String,
    val backupOk: String,
    val backupWaitingFile: String,
    val restoreBackup: String,
    val permissionsSection: String,
    val permissionBackground: String,
    val allowAlways: String,
    val permissionNotifications: String,
    val allowNotifications: String,
    val permissionBattery: String,
    val disableBattery: String,
    val permissionAlarms: String,
    val allowAlarms: String,
    val permissionDocuments: String,
    val allowAllFiles: String,
    val granted: String,
    val needed: String,
    val appearanceSection: String,
    val themeDark: String,
    val themeLight: String,
    val themeSystem: String,
    val dynamicColor: String,
    val backgroundTitle: String,
    val backgroundBody: String,
    val continueAction: String,
    val notNow: String,
    val ruleEnabled: String,
    val fromPrefix: String,
    val untilPrefix: String,
    val hoursLabel: String,
    val hoursFractionHint: String,
    val alarmPrefix: String,
    val alarmOnHint: String,
    val alarmOffHint: String,
    val deleteRule: String,
    val timeWindow: String,
    val done: String,
    val cancel: String,
    val gpsNoResponse: String,
    val coordinatesInvalid: String,
    val pointSaved: String,
    val coordinateBounds: String,
    val defaultWorkplaceName: String,
    val rateNotNumber: String,
    val defaultShiftName: String,
    val ruleHoursOutOfRange: String,
    val newRuleName: String,
    val currentMonthCopied: String,
    val back: String,
    val searchHint: String,
    val searching: String,
    val scheme: String,
    val satellite: String,
    val zoomIn: String,
    val zoomOut: String,
    val myLocation: String,
    val pinCenter: String,
    val radiusPattern: (Int) -> String,
    val mapHint: String,
    val saveThisPoint: String,
    val mapAttribution: String,
    val shiftRecordedTitle: String,
    val channelShifts: String,
    val channelShiftsDescription: String,
    val channelPresence: String,
    val channelPresenceDescription: String,
    val newShift: String,
    val editShift: String,
    val oneDayHint: String,
    val dateLabel: String,
    val choose: String,
    val quickPick: String,
    val hoursFieldHint: String,
    val save: String,
    val deleteRecord: String,
    val close: String,
    val language: AppLanguage,
) {
    fun others(count: Int): String = "$othersLabel: $count"

    fun toPay(amount: String): String = "$toPayPrefix $amount"

    fun entryAt(time: String): String = "$entryPrefix $time"

    fun exitAt(time: String): String = "$exitPrefix $time"

    fun insideZone(distance: String): String = "$insidePrefix · $distance"

    fun distanceToWork(distance: String): String = "$distance $toWorkSuffix"

    fun workplaceRadius(meters: Int): String = workplaceRadiusPattern(meters)

    fun enteredAt(time: String): String = "$enteredPrefix $time"

    fun restoredCount(count: Int): String = "$restoredPrefix: $count"

    fun rateLabel(currency: String): String = ratePattern(currency)

    fun fromTime(time: String): String = "$fromPrefix $time"

    fun untilTime(time: String): String = "$untilPrefix $time"

    fun alarmTitle(time: String): String = "$alarmPrefix ($time)"

    fun radiusMeters(meters: Int): String = radiusPattern(meters)

    fun ruleBadge(rule: ShiftRule): String {
        val defaultShorts = setOf("Короткая смена", "Коротка зміна", "Krátká směna", "Short shift")
        val defaultLongs = setOf("Длинная смена", "Довга зміна", "Dlouhá směna", "Long shift")
        if (rule.id == "short" && rule.name in defaultShorts) return shortShift
        if (rule.id == "long" && rule.name in defaultLongs) return longShift
        return rule.name
    }

    fun workplaceFieldName(stored: String?): String {
        val defaults = setOf("Работа", "Робота", "Práce", "Work")
        if (stored.isNullOrBlank() || stored in defaults) return defaultWorkplaceName
        return stored
    }

    fun geofenceStatus(status: GeofenceStatus): String = when (status) {
        GeofenceStatus.NotRegistered -> when (language) {
            AppLanguage.RU -> "Геозона не зарегистрирована"
            AppLanguage.UK -> "Геозона не зареєстрована"
            AppLanguage.CS -> "Geozóna není zaregistrovaná"
            AppLanguage.EN -> "Geofence is not registered"
        }
        GeofenceStatus.WorkplaceNotSet -> when (language) {
            AppLanguage.RU -> "Точка работы не задана"
            AppLanguage.UK -> "Точку роботи не задано"
            AppLanguage.CS -> "Místo práce není nastaveno"
            AppLanguage.EN -> "Workplace is not set"
        }
        GeofenceStatus.NeedBackgroundPermission -> when (language) {
            AppLanguage.RU -> "Нужно разрешение геолокации «в любом режиме»"
            AppLanguage.UK -> "Потрібен дозвіл геолокації «у будь-якому режимі»"
            AppLanguage.CS -> "Je potřeba poloha „v každém režimu“"
            AppLanguage.EN -> "Location “all the time” is required"
        }
        GeofenceStatus.PlayServicesUnavailable -> when (language) {
            AppLanguage.RU -> "Сервисы Google Play недоступны"
            AppLanguage.UK -> "Сервіси Google Play недоступні"
            AppLanguage.CS -> "Služby Google Play nejsou dostupné"
            AppLanguage.EN -> "Google Play services are unavailable"
        }
        is GeofenceStatus.Active -> when (language) {
            AppLanguage.RU -> "Геозона активна · радиус ${status.radiusMeters} м"
            AppLanguage.UK -> "Геозона активна · радіус ${status.radiusMeters} м"
            AppLanguage.CS -> "Geozóna aktivní · poloměr ${status.radiusMeters} m"
            AppLanguage.EN -> "Geofence active · radius ${status.radiusMeters} m"
        }
        GeofenceStatus.Timeout -> when (language) {
            AppLanguage.RU -> "Геозона не ответила вовремя"
            AppLanguage.UK -> "Геозона не відповіла вчасно"
            AppLanguage.CS -> "Geozóna neodpověděla včas"
            AppLanguage.EN -> "The geofence did not answer in time"
        }
        is GeofenceStatus.Error -> geofenceError(status.code)
    }

    private fun geofenceError(code: Int?): String {
        if (code == null) {
            return when (language) {
                AppLanguage.RU -> "Геозона не включилась"
                AppLanguage.UK -> "Геозона не ввімкнулася"
                AppLanguage.CS -> "Geozónu se nepodařilo zapnout"
                AppLanguage.EN -> "The geofence did not turn on"
            }
        }
        return when (code) {
            1000 -> when (language) {
                AppLanguage.RU -> "Геозоны недоступны. Включите геолокацию и сервисы Google Play."
                AppLanguage.UK -> "Геозони недоступні. Увімкніть геолокацію та сервіси Google Play."
                AppLanguage.CS -> "Geozóny nejsou dostupné. Zapněte polohu a služby Google Play."
                AppLanguage.EN -> "Geofences are unavailable. Turn on location and Google Play services."
            }
            1001 -> when (language) {
                AppLanguage.RU -> "Слишком много геозон в системе."
                AppLanguage.UK -> "Забагато геозон у системі."
                AppLanguage.CS -> "V systému je příliš mnoho geozón."
                AppLanguage.EN -> "Too many geofences on the device."
            }
            1002 -> when (language) {
                AppLanguage.RU -> "Слишком много ожидающих геозон."
                AppLanguage.UK -> "Забагато геозон в очікуванні."
                AppLanguage.CS -> "Příliš mnoho čekajících geozón."
                AppLanguage.EN -> "Too many pending geofences."
            }
            else -> when (language) {
                AppLanguage.RU -> "Не удалось включить геозону ($code)"
                AppLanguage.UK -> "Не вдалося ввімкнути геозону ($code)"
                AppLanguage.CS -> "Geozónu se nepodařilo zapnout ($code)"
                AppLanguage.EN -> "Could not turn the geofence on ($code)"
            }
        }
    }

    fun onShiftTitle(): String = when (language) {
        AppLanguage.RU -> "На смене"
        AppLanguage.UK -> "На зміні"
        AppLanguage.CS -> "Na směně"
        AppLanguage.EN -> "On shift"
    }

    fun onShiftBody(clock: String): String = when (language) {
        AppLanguage.RU -> "Вход в $clock. Смена подтвердится через 15 минут на территории."
        AppLanguage.UK -> "Вхід о $clock. Зміна підтвердиться через 15 хвилин на території."
        AppLanguage.CS -> "Příchod v $clock. Směna se potvrdí po 15 minutách v areálu."
        AppLanguage.EN -> "Arrived at $clock. The shift is confirmed after 15 minutes on site."
    }

    fun dwellTitle(): String = when (language) {
        AppLanguage.RU -> "На работе — смена идёт"
        AppLanguage.UK -> "На роботі — зміна триває"
        AppLanguage.CS -> "V práci — směna běží"
        AppLanguage.EN -> "At work — shift in progress"
    }

    fun dwellBody(): String = when (language) {
        AppLanguage.RU -> "Вы на территории больше 15 минут."
        AppLanguage.UK -> "Ви на території більше 15 хвилин."
        AppLanguage.CS -> "Jste v areálu déle než 15 minut."
        AppLanguage.EN -> "You have been on site for more than 15 minutes."
    }

    fun shortVisitTitle(): String = when (language) {
        AppLanguage.RU -> "Короткий визит"
        AppLanguage.UK -> "Короткий візит"
        AppLanguage.CS -> "Krátká návštěva"
        AppLanguage.EN -> "Short visit"
    }

    fun shortVisitBody(): String = when (language) {
        AppLanguage.RU -> "Вы были в зоне меньше 15 минут — часы не записаны."
        AppLanguage.UK -> "Ви були в зоні менше 15 хвилин — години не записані."
        AppLanguage.CS -> "V zóně jste byli méně než 15 minut — hodiny se nezapsaly."
        AppLanguage.EN -> "You were in the zone for less than 15 minutes — hours were not saved."
    }

    fun outsideWindowTitle(): String = when (language) {
        AppLanguage.RU -> "Выход вне окна смены"
        AppLanguage.UK -> "Вихід поза вікном зміни"
        AppLanguage.CS -> "Odchod mimo okno směny"
        AppLanguage.EN -> "Exit outside the shift window"
    }

    fun outsideWindowBody(time: String): String = when (language) {
        AppLanguage.RU -> "Уход в $time не попал ни в одно правило. Запись не создана."
        AppLanguage.UK -> "Вихід о $time не потрапив у жодне правило. Запис не створено."
        AppLanguage.CS -> "Odchod v $time nespadl do žádného pravidla. Záznam nebyl vytvořen."
        AppLanguage.EN -> "Leaving at $time did not match any rule. No record was created."
    }

    fun recordDeleted(): String = when (language) {
        AppLanguage.RU -> "Запись удалена"
        AppLanguage.UK -> "Запис видалено"
        AppLanguage.CS -> "Záznam smazán"
        AppLanguage.EN -> "Record deleted"
    }

    fun undo(): String = when (language) {
        AppLanguage.RU -> "Вернуть"
        AppLanguage.UK -> "Повернути"
        AppLanguage.CS -> "Vrátit"
        AppLanguage.EN -> "Undo"
    }

    fun shareFailed(): String = when (language) {
        AppLanguage.RU -> "Не удалось открыть меню «Поделиться»"
        AppLanguage.UK -> "Не вдалося відкрити меню «Поділитися»"
        AppLanguage.CS -> "Nepodařilo se otevřít nabídku sdílení"
        AppLanguage.EN -> "Could not open the share menu"
    }

    fun shareChooser(): String = when (language) {
        AppLanguage.RU -> "Поделиться отчётом"
        AppLanguage.UK -> "Поділитися звітом"
        AppLanguage.CS -> "Sdílet výkaz"
        AppLanguage.EN -> "Share report"
    }

    fun historyRestored(count: Int): String = when (language) {
        AppLanguage.RU -> "История восстановлена: $count"
        AppLanguage.UK -> "Історію відновлено: $count"
        AppLanguage.CS -> "Historie obnovena: $count"
        AppLanguage.EN -> "History restored: $count"
    }

    fun backupUnreadableManual(): String = when (language) {
        AppLanguage.RU -> "Бэкап найден, но не прочитан. Выберите файл вручную."
        AppLanguage.UK -> "Бекап знайдено, але не прочитано. Виберіть файл вручну."
        AppLanguage.CS -> "Záloha byla nalezena, ale nešla přečíst. Vyberte soubor ručně."
        AppLanguage.EN -> "A backup was found but could not be read. Choose the file manually."
    }

    fun jsonCorrupt(): String = when (language) {
        AppLanguage.RU -> "Файл JSON повреждён или это не бэкап AutoWorkTracker"
        AppLanguage.UK -> "Файл JSON пошкоджений або це не бекап AutoWorkTracker"
        AppLanguage.CS -> "Soubor JSON je poškozený, nebo to není záloha AutoWorkTracker"
        AppLanguage.EN -> "The JSON file is damaged or it is not an AutoWorkTracker backup"
    }

    fun noReportLines(): String = when (language) {
        AppLanguage.RU -> "В файле не найдено строк вида «2 ноября: 11 часов»"
        AppLanguage.UK -> "У файлі не знайдено рядків на кшталт «2 листопада: 11 годин»"
        AppLanguage.CS -> "V souboru nejsou řádky jako «2. listopadu: 11 hodin»"
        AppLanguage.EN -> "The file has no lines like “November 2: 11 hours”"
    }

    fun writeFailed(missing: String): String = when (language) {
        AppLanguage.RU -> "Не удалось записать $missing в Documents"
        AppLanguage.UK -> "Не вдалося записати $missing у Documents"
        AppLanguage.CS -> "Nepodařilo se zapsat $missing do Documents"
        AppLanguage.EN -> "Could not write $missing to Documents"
    }

    fun fileDidNotOpen(): String = when (language) {
        AppLanguage.RU -> "Файл не открылся"
        AppLanguage.UK -> "Файл не відкрився"
        AppLanguage.CS -> "Soubor se neotevřel"
        AppLanguage.EN -> "The file could not be opened"
    }

    companion object {
        fun of(language: AppLanguage): AppText = when (language) {
            AppLanguage.RU -> russian()
            AppLanguage.UK -> ukrainian()
            AppLanguage.CS -> czech()
            AppLanguage.EN -> english()
        }
    }
}

private fun russian() = AppText(
    navShifts = "Смены",
    navSettings = "Настройки",
    addShift = "Добавить смену",
    shiftHistory = "История смен",
    restoreTitle = "Восстановить историю?",
    restoreBody = "База пустая. Если приложение переустанавливали, выберите autowork_backup.json " +
        "или Отработанные_часы.txt из папки Documents/AutoWorkTracker.",
    chooseFile = "Выбрать файл",
    later = "Позже",
    copyReport = "Скопировать отчёт за месяц",
    shareFile = "Поделиться файлом",
    refreshDistance = "Обновить расстояние",
    backupActive = "Бэкап в Documents: Активен",
    backupWaiting = "Бэкап: ждёт запись",
    ecoMode = "Эко-режим",
    geofenceOff = "Геозона выкл.",
    previousMonth = "Предыдущий месяц",
    nextMonth = "Следующий месяц",
    othersLabel = "Другие",
    noShiftsYet = "Смен пока нет",
    toPayPrefix = "К выплате",
    emptyMonthTitle = "За этот месяц смен пока нет",
    emptyMonthBody = "Геозона запишет их сама, когда вы уйдёте с работы в окно смены. Или добавьте день кнопкой «+».",
    delete = "Удалить",
    entryPrefix = "Вход",
    exitPrefix = "Выход",
    shortShift = "Короткая смена",
    longShift = "Длинная смена",
    workplaceMissing = "Точка работы не задана",
    workingNow = "На работе — смена идёт",
    waitingFifteen = "В зоне работы — ждём 15 минут",
    sleepingGeofence = "Вне работы — спящий режим геозоны",
    waitingGps = "Ждём точный GPS…",
    allowLocationDistance = "Разрешите геолокацию, чтобы видеть расстояние",
    setWorkplace = "Укажите точку работы в настройках",
    insidePrefix = "Вы в зоне работы",
    toWorkSuffix = "до работы",
    workplaceRadiusPattern = { meters -> "Точка работы: радиус $meters м" },
    noLocationPermission = "Нет доступа к геолокации",
    distanceUnavailable = "Расстояние сейчас недоступно",
    radiusPending = "Точка работы: радиус появится после проверки",
    enteredPrefix = "Вход в",
    hoursOutOfRange = "Укажите часы от 0,5 до 24",
    shiftSaved = "Смена сохранена",
    monthCopied = "Отчёт за месяц скопирован",
    restoredPrefix = "Восстановлено записей",
    backupUnreadable = "Не удалось прочитать бэкап",
    clipboardTitle = "Отработанные часы",
    settingsTitle = "Настройки",
    settingsSubtitle = "Точка работы, правила смен и резервная копия.",
    workplaceSection = "Точка работы",
    openMap = "Открыть карту",
    workplaceHint = "На карте сдвиньте булавку на работу и выберите радиус зоны.",
    locatingGps = "Ищем точный GPS…",
    suggestMyPlace = "Подсказать моё место на карте",
    nameLabel = "Название",
    latitude = "Широта",
    longitude = "Долгота",
    savePoint = "Сохранить точку",
    rulesSection = "Правила смен",
    overwriteTitle = "Обновлять смену при уходе вечером",
    overwriteHint = "Если вы выходили с территории днём (например, на перерыв в 14:00) и записалась короткая смена, то при уходе домой вечером запись за сегодня автоматически обновится на полную длинную смену.",
    addRule = "Добавить правило",
    paySection = "Оплата",
    ratePattern = { currency -> "Ставка, $currency/час" },
    saveRate = "Сохранить ставку",
    backupSection = "Резервная копия",
    backupOk = "Последняя запись прошла успешно",
    backupWaitingFile = "Файл появится после первой смены или восстановления",
    restoreBackup = "Восстановить записи из файла бэкапа",
    permissionsSection = "Разрешения",
    permissionBackground = "Геолокация «в любом режиме»",
    allowAlways = "Разрешить в любом режиме",
    permissionNotifications = "Уведомления",
    allowNotifications = "Разрешить уведомления",
    permissionBattery = "Без ограничений батареи",
    disableBattery = "Отключить экономию батареи",
    permissionAlarms = "Точные будильники",
    allowAlarms = "Разрешить точные будильники",
    permissionDocuments = "Доступ к папке Documents",
    allowAllFiles = "Открыть доступ ко всем файлам",
    granted = "Выдано",
    needed = "Нужно",
    appearanceSection = "Оформление",
    themeDark = "Тёмная",
    themeLight = "Светлая",
    themeSystem = "Система",
    dynamicColor = "Динамические цвета Material You",
    backgroundTitle = "Разрешить в любом режиме",
    backgroundBody = "Геозона должна срабатывать, когда приложение закрыто. На следующем экране выберите «Разрешить в любом режиме».",
    continueAction = "Продолжить",
    notNow = "Не сейчас",
    ruleEnabled = "Правило включено",
    fromPrefix = "С",
    untilPrefix = "До",
    hoursLabel = "Часы",
    hoursFractionHint = "Можно 11,5 или 11.5 — без округления.",
    alarmPrefix = "Страховка по будильнику",
    alarmOnHint = "Включена только со свежим GPS точнее 50 м. Иначе смена не закроется.",
    alarmOffHint = "Выключена: часы пишет только геозона.",
    deleteRule = "Удалить правило",
    timeWindow = "Время окна",
    done = "Готово",
    cancel = "Отмена",
    gpsNoResponse = "GPS не ответил. Поставьте булавку на карте вручную.",
    coordinatesInvalid = "Точка на карте вне допустимых координат",
    pointSaved = "Точка работы сохранена",
    coordinateBounds = "Широта от −90 до 90, долгота от −180 до 180",
    defaultWorkplaceName = "Работа",
    rateNotNumber = "Ставка должна быть числом",
    defaultShiftName = "Смена",
    ruleHoursOutOfRange = "Часы правила: от 0,5 до 24",
    newRuleName = "Новое правило",
    currentMonthCopied = "Отчёт за текущий месяц скопирован",
    back = "Назад",
    searchHint = "Улица, дом, город",
    searching = "Ищем…",
    scheme = "Схема",
    satellite = "Спутник",
    zoomIn = "Приблизить",
    zoomOut = "Отдалить",
    myLocation = "Где я",
    pinCenter = "Центр точки работы",
    radiusPattern = { meters -> "Радиус $meters м" },
    mapHint = "Наведите булавку на работу и выберите радиус зоны.",
    saveThisPoint = "Сохранить эту точку",
    mapAttribution = "Снимок © Esri, Maxar · подписи © Esri · поиск © OpenStreetMap",
    shiftRecordedTitle = "Смена записана!",
    channelShifts = "Смены",
    channelShiftsDescription = "Автоматическая запись отработанных часов",
    channelPresence = "Присутствие на работе",
    channelPresenceDescription = "Вход, подтверждение и короткие визиты",
    newShift = "Новая смена",
    editShift = "Изменить смену",
    oneDayHint = "Один день — одна запись. Если в выбранный день уже есть часы, они будут заменены.",
    dateLabel = "Дата",
    choose = "Выбрать",
    quickPick = "Быстрый выбор",
    hoursFieldHint = "От 0,5 до 24. Дробь можно писать через запятую.",
    save = "Сохранить",
    deleteRecord = "Удалить запись",
    close = "Закрыть",
    language = AppLanguage.RU,
)

private fun ukrainian() = AppText(
    navShifts = "Зміни",
    navSettings = "Налаштування",
    addShift = "Додати зміну",
    shiftHistory = "Історія змін",
    restoreTitle = "Відновити історію?",
    restoreBody = "База порожня. Якщо застосунок перевстановлювали, виберіть autowork_backup.json " +
        "або Отработанные_часы.txt з папки Documents/AutoWorkTracker.",
    chooseFile = "Вибрати файл",
    later = "Пізніше",
    copyReport = "Скопіювати звіт за місяць",
    shareFile = "Поділитися файлом",
    refreshDistance = "Оновити відстань",
    backupActive = "Бекап у Documents: активний",
    backupWaiting = "Бекап: чекає на запис",
    ecoMode = "Екорежим",
    geofenceOff = "Геозона вимкнена",
    previousMonth = "Попередній місяць",
    nextMonth = "Наступний місяць",
    othersLabel = "Інші",
    noShiftsYet = "Змін поки немає",
    toPayPrefix = "До виплати",
    emptyMonthTitle = "За цей місяць змін поки немає",
    emptyMonthBody = "Геозона запише їх сама, коли ви підете з роботи у вікно зміни. Або додайте день кнопкою «+».",
    delete = "Видалити",
    entryPrefix = "Вхід",
    exitPrefix = "Вихід",
    shortShift = "Коротка зміна",
    longShift = "Довга зміна",
    workplaceMissing = "Точку роботи не задано",
    workingNow = "На роботі — зміна триває",
    waitingFifteen = "У зоні роботи — чекаємо 15 хвилин",
    sleepingGeofence = "Поза роботою — сплячий режим геозони",
    waitingGps = "Чекаємо точний GPS…",
    allowLocationDistance = "Дозвольте геолокацію, щоб бачити відстань",
    setWorkplace = "Вкажіть точку роботи в налаштуваннях",
    insidePrefix = "Ви в зоні роботи",
    toWorkSuffix = "до роботи",
    workplaceRadiusPattern = { meters -> "Точка роботи: радіус $meters м" },
    noLocationPermission = "Немає доступу до геолокації",
    distanceUnavailable = "Відстань зараз недоступна",
    radiusPending = "Точка роботи: радіус з’явиться після перевірки",
    enteredPrefix = "Вхід о",
    hoursOutOfRange = "Вкажіть години від 0,5 до 24",
    shiftSaved = "Зміну збережено",
    monthCopied = "Звіт за місяць скопійовано",
    restoredPrefix = "Відновлено записів",
    backupUnreadable = "Не вдалося прочитати бекап",
    clipboardTitle = "Відпрацьовані години",
    settingsTitle = "Налаштування",
    settingsSubtitle = "Точка роботи, правила змін і резервна копія.",
    workplaceSection = "Точка роботи",
    openMap = "Відкрити карту",
    workplaceHint = "На карті зсуньте шпильку на роботу і виберіть радіус зони.",
    locatingGps = "Шукаємо точний GPS…",
    suggestMyPlace = "Підказати моє місце на карті",
    nameLabel = "Назва",
    latitude = "Широта",
    longitude = "Довгота",
    savePoint = "Зберегти точку",
    rulesSection = "Правила змін",
    overwriteTitle = "Оновлювати зміну при виході ввечері",
    overwriteHint = "Якщо ви виходили з території вдень (наприклад, на перерву о 14:00) і записалася коротка зміна, то при виході додому ввечері запис за сьогодні автоматично оновиться на повну довгу зміну.",
    addRule = "Додати правило",
    paySection = "Оплата",
    ratePattern = { currency -> "Ставка, $currency/год" },
    saveRate = "Зберегти ставку",
    backupSection = "Резервна копія",
    backupOk = "Останній запис пройшов успішно",
    backupWaitingFile = "Файл з’явиться після першої зміни або відновлення",
    restoreBackup = "Відновити записи з файлу бекапу",
    permissionsSection = "Дозволи",
    permissionBackground = "Геолокація «у будь-якому режимі»",
    allowAlways = "Дозволити в будь-якому режимі",
    permissionNotifications = "Сповіщення",
    allowNotifications = "Дозволити сповіщення",
    permissionBattery = "Без обмежень батареї",
    disableBattery = "Вимкнути економію батареї",
    permissionAlarms = "Точні будильники",
    allowAlarms = "Дозволити точні будильники",
    permissionDocuments = "Доступ до папки Documents",
    allowAllFiles = "Відкрити доступ до всіх файлів",
    granted = "Надано",
    needed = "Потрібно",
    appearanceSection = "Оформлення",
    themeDark = "Темна",
    themeLight = "Світла",
    themeSystem = "Система",
    dynamicColor = "Динамічні кольори Material You",
    backgroundTitle = "Дозволити в будь-якому режимі",
    backgroundBody = "Геозона має спрацьовувати, коли застосунок закрито. На наступному екрані виберіть «Дозволити в будь-якому режимі».",
    continueAction = "Продовжити",
    notNow = "Не зараз",
    ruleEnabled = "Правило увімкнено",
    fromPrefix = "З",
    untilPrefix = "До",
    hoursLabel = "Години",
    hoursFractionHint = "Можна 11,5 або 11.5 — без округлення.",
    alarmPrefix = "Страховка за будильником",
    alarmOnHint = "Увімкнена лише зі свіжим GPS точнішим за 50 м. Інакше зміна не закриється.",
    alarmOffHint = "Вимкнена: години пише лише геозона.",
    deleteRule = "Видалити правило",
    timeWindow = "Час вікна",
    done = "Готово",
    cancel = "Скасувати",
    gpsNoResponse = "GPS не відповів. Поставте шпильку на карті вручну.",
    coordinatesInvalid = "Точка на карті поза допустимими координатами",
    pointSaved = "Точку роботи збережено",
    coordinateBounds = "Широта від −90 до 90, довгота від −180 до 180",
    defaultWorkplaceName = "Робота",
    rateNotNumber = "Ставка має бути числом",
    defaultShiftName = "Зміна",
    ruleHoursOutOfRange = "Години правила: від 0,5 до 24",
    newRuleName = "Нове правило",
    currentMonthCopied = "Звіт за поточний місяць скопійовано",
    back = "Назад",
    searchHint = "Вулиця, будинок, місто",
    searching = "Шукаємо…",
    scheme = "Схема",
    satellite = "Супутник",
    zoomIn = "Наблизити",
    zoomOut = "Віддалити",
    myLocation = "Де я",
    pinCenter = "Центр точки роботи",
    radiusPattern = { meters -> "Радіус $meters м" },
    mapHint = "Наведіть шпильку на роботу і виберіть радіус зони.",
    saveThisPoint = "Зберегти цю точку",
    mapAttribution = "Знімок © Esri, Maxar · підписи © Esri · пошук © OpenStreetMap",
    shiftRecordedTitle = "Зміну записано!",
    channelShifts = "Зміни",
    channelShiftsDescription = "Автоматичний запис відпрацьованих годин",
    channelPresence = "Присутність на роботі",
    channelPresenceDescription = "Вхід, підтвердження і короткі візити",
    newShift = "Нова зміна",
    editShift = "Змінити зміну",
    oneDayHint = "Один день — один запис. Якщо в обраний день уже є години, їх буде замінено.",
    dateLabel = "Дата",
    choose = "Вибрати",
    quickPick = "Швидкий вибір",
    hoursFieldHint = "Від 0,5 до 24. Дріб можна писати через кому.",
    save = "Зберегти",
    deleteRecord = "Видалити запис",
    close = "Закрити",
    language = AppLanguage.UK,
)

private fun czech() = AppText(
    navShifts = "Směny",
    navSettings = "Nastavení",
    addShift = "Přidat směnu",
    shiftHistory = "Historie směn",
    restoreTitle = "Obnovit historii?",
    restoreBody = "Databáze je prázdná. Pokud jste aplikaci přeinstalovali, vyberte autowork_backup.json " +
        "nebo Отработанные_часы.txt ze složky Documents/AutoWorkTracker.",
    chooseFile = "Vybrat soubor",
    later = "Později",
    copyReport = "Kopírovat výkaz za měsíc",
    shareFile = "Sdílet soubor",
    refreshDistance = "Obnovit vzdálenost",
    backupActive = "Záloha v Documents: aktivní",
    backupWaiting = "Záloha: čeká na zápis",
    ecoMode = "Eko režim",
    geofenceOff = "Geozóna vypnutá",
    previousMonth = "Předchozí měsíc",
    nextMonth = "Další měsíc",
    othersLabel = "Ostatní",
    noShiftsYet = "Zatím žádné směny",
    toPayPrefix = "K výplatě",
    emptyMonthTitle = "Za tento měsíc zatím žádné směny",
    emptyMonthBody = "Geozóna je zapíše sama, až odejdete z práce v okně směny. Nebo přidejte den tlačítkem «+».",
    delete = "Smazat",
    entryPrefix = "Příchod",
    exitPrefix = "Odchod",
    shortShift = "Krátká směna",
    longShift = "Dlouhá směna",
    workplaceMissing = "Místo práce není nastaveno",
    workingNow = "V práci — směna běží",
    waitingFifteen = "V zóně práce — čekáme 15 minut",
    sleepingGeofence = "Mimo práci — spící geozóna",
    waitingGps = "Čekáme na přesnou GPS…",
    allowLocationDistance = "Povolte polohu, abyste viděli vzdálenost",
    setWorkplace = "Nastavte místo práce v nastavení",
    insidePrefix = "Jste v zóně práce",
    toWorkSuffix = "do práce",
    workplaceRadiusPattern = { meters -> "Místo práce: poloměr $meters m" },
    noLocationPermission = "Není přístup k poloze",
    distanceUnavailable = "Vzdálenost teď není dostupná",
    radiusPending = "Místo práce: poloměr se ukáže po kontrole",
    enteredPrefix = "Příchod v",
    hoursOutOfRange = "Zadejte hodiny od 0,5 do 24",
    shiftSaved = "Směna uložena",
    monthCopied = "Výkaz za měsíc zkopírován",
    restoredPrefix = "Obnoveno záznamů",
    backupUnreadable = "Zálohu se nepodařilo přečíst",
    clipboardTitle = "Odpracované hodiny",
    settingsTitle = "Nastavení",
    settingsSubtitle = "Místo práce, pravidla směn a záloha.",
    workplaceSection = "Místo práce",
    openMap = "Otevřít mapu",
    workplaceHint = "Na mapě posuňte špendlík na práci a zvolte poloměr zóny.",
    locatingGps = "Hledáme přesnou GPS…",
    suggestMyPlace = "Ukázat moji polohu na mapě",
    nameLabel = "Název",
    latitude = "Zeměpisná šířka",
    longitude = "Zeměpisná délka",
    savePoint = "Uložit místo",
    rulesSection = "Pravidla směn",
    overwriteTitle = "Aktualizovat směnu při večerním odchodu",
    overwriteHint = "Pokud jste přes den odešli z areálu (např. na pauzu ve 14:00) a zapsala se krátká směna, při odchodu domů večer se dnešní záznam automaticky přepíše na celou dlouhou směnu.",
    addRule = "Přidat pravidlo",
    paySection = "Odměna",
    ratePattern = { currency -> "Sazba, $currency/h" },
    saveRate = "Uložit sazbu",
    backupSection = "Záloha",
    backupOk = "Poslední zápis proběhl úspěšně",
    backupWaitingFile = "Soubor se objeví po první směně nebo obnovení",
    restoreBackup = "Obnovit záznamy ze souboru zálohy",
    permissionsSection = "Oprávnění",
    permissionBackground = "Poloha „v každém režimu“",
    allowAlways = "Povolit v každém režimu",
    permissionNotifications = "Oznámení",
    allowNotifications = "Povolit oznámení",
    permissionBattery = "Bez omezení baterie",
    disableBattery = "Vypnout úsporu baterie",
    permissionAlarms = "Přesné budíky",
    allowAlarms = "Povolit přesné budíky",
    permissionDocuments = "Přístup ke složce Documents",
    allowAllFiles = "Otevřít přístup ke všem souborům",
    granted = "Uděleno",
    needed = "Chybí",
    appearanceSection = "Vzhled",
    themeDark = "Tmavý",
    themeLight = "Světlý",
    themeSystem = "Systém",
    dynamicColor = "Dynamické barvy Material You",
    backgroundTitle = "Povolit v každém režimu",
    backgroundBody = "Geozóna má fungovat, i když je aplikace zavřená. Na další obrazovce zvolte „Povolit v každém režimu“.",
    continueAction = "Pokračovat",
    notNow = "Teď ne",
    ruleEnabled = "Pravidlo zapnuto",
    fromPrefix = "Od",
    untilPrefix = "Do",
    hoursLabel = "Hodiny",
    hoursFractionHint = "Lze zadat 11,5 nebo 11.5 — bez zaokrouhlení.",
    alarmPrefix = "Pojistka budíkem",
    alarmOnHint = "Zapnutá jen s čerstvou GPS přesnější než 50 m. Jinak se směna nezavře.",
    alarmOffHint = "Vypnutá: hodiny zapisuje jen geozóna.",
    deleteRule = "Smazat pravidlo",
    timeWindow = "Čas okna",
    done = "Hotovo",
    cancel = "Zrušit",
    gpsNoResponse = "GPS neodpověděla. Umístěte špendlík na mapě ručně.",
    coordinatesInvalid = "Bod na mapě je mimo povolené souřadnice",
    pointSaved = "Místo práce uloženo",
    coordinateBounds = "Šířka od −90 do 90, délka od −180 do 180",
    defaultWorkplaceName = "Práce",
    rateNotNumber = "Sazba musí být číslo",
    defaultShiftName = "Směna",
    ruleHoursOutOfRange = "Hodiny pravidla: od 0,5 do 24",
    newRuleName = "Nové pravidlo",
    currentMonthCopied = "Výkaz za aktuální měsíc zkopírován",
    back = "Zpět",
    searchHint = "Ulice, číslo, město",
    searching = "Hledáme…",
    scheme = "Mapa",
    satellite = "Satelit",
    zoomIn = "Přiblížit",
    zoomOut = "Oddálit",
    myLocation = "Kde jsem",
    pinCenter = "Střed místa práce",
    radiusPattern = { meters -> "Poloměr $meters m" },
    mapHint = "Namiřte špendlík na práci a zvolte poloměr zóny.",
    saveThisPoint = "Uložit toto místo",
    mapAttribution = "Snímek © Esri, Maxar · popisky © Esri · hledání © OpenStreetMap",
    shiftRecordedTitle = "Směna zapsána!",
    channelShifts = "Směny",
    channelShiftsDescription = "Automatický zápis odpracovaných hodin",
    channelPresence = "Přítomnost v práci",
    channelPresenceDescription = "Příchod, potvrzení a krátké návštěvy",
    newShift = "Nová směna",
    editShift = "Upravit směnu",
    oneDayHint = "Jeden den — jeden záznam. Pokud ve vybraném dni už hodiny jsou, budou nahrazeny.",
    dateLabel = "Datum",
    choose = "Vybrat",
    quickPick = "Rychlá volba",
    hoursFieldHint = "Od 0,5 do 24. Desetinné číslo lze psát čárkou.",
    save = "Uložit",
    deleteRecord = "Smazat záznam",
    close = "Zavřít",
    language = AppLanguage.CS,
)

private fun english() = AppText(
    navShifts = "Shifts",
    navSettings = "Settings",
    addShift = "Add shift",
    shiftHistory = "Shift history",
    restoreTitle = "Restore history?",
    restoreBody = "The database is empty. If you reinstalled the app, choose autowork_backup.json " +
        "or Отработанные_часы.txt from Documents/AutoWorkTracker.",
    chooseFile = "Choose file",
    later = "Later",
    copyReport = "Copy the month report",
    shareFile = "Share file",
    refreshDistance = "Refresh distance",
    backupActive = "Backup in Documents: active",
    backupWaiting = "Backup: waiting to write",
    ecoMode = "Eco mode",
    geofenceOff = "Geofence off",
    previousMonth = "Previous month",
    nextMonth = "Next month",
    othersLabel = "Other",
    noShiftsYet = "No shifts yet",
    toPayPrefix = "To pay",
    emptyMonthTitle = "No shifts this month yet",
    emptyMonthBody = "The geofence writes them when you leave work inside a shift window. Or add a day with «+».",
    delete = "Delete",
    entryPrefix = "In",
    exitPrefix = "Out",
    shortShift = "Short shift",
    longShift = "Long shift",
    workplaceMissing = "Workplace is not set",
    workingNow = "At work — shift in progress",
    waitingFifteen = "Inside the workplace — waiting 15 minutes",
    sleepingGeofence = "Away from work — geofence is asleep",
    waitingGps = "Waiting for a precise GPS fix…",
    allowLocationDistance = "Allow location to see the distance",
    setWorkplace = "Set the workplace in Settings",
    insidePrefix = "You are inside the workplace",
    toWorkSuffix = "to work",
    workplaceRadiusPattern = { meters -> "Workplace: radius $meters m" },
    noLocationPermission = "No location access",
    distanceUnavailable = "Distance is unavailable right now",
    radiusPending = "Workplace: the radius appears after a check",
    enteredPrefix = "Arrived at",
    hoursOutOfRange = "Enter hours from 0.5 to 24",
    shiftSaved = "Shift saved",
    monthCopied = "Month report copied",
    restoredPrefix = "Records restored",
    backupUnreadable = "Could not read the backup",
    clipboardTitle = "Worked hours",
    settingsTitle = "Settings",
    settingsSubtitle = "Workplace, shift rules, and backup.",
    workplaceSection = "Workplace",
    openMap = "Open map",
    workplaceHint = "On the map, move the pin to work and choose the zone radius.",
    locatingGps = "Looking for a precise GPS fix…",
    suggestMyPlace = "Show my location on the map",
    nameLabel = "Name",
    latitude = "Latitude",
    longitude = "Longitude",
    savePoint = "Save workplace",
    rulesSection = "Shift rules",
    overwriteTitle = "Update shift on evening exit",
    overwriteHint = "If you stepped out during the day (for example, on a break at 14:00) and a short shift was logged, leaving work in the evening will automatically update today's record to a full long shift.",
    addRule = "Add rule",
    paySection = "Pay",
    ratePattern = { currency -> "Rate, $currency/hour" },
    saveRate = "Save rate",
    backupSection = "Backup",
    backupOk = "The last write succeeded",
    backupWaitingFile = "The file appears after the first shift or a restore",
    restoreBackup = "Restore records from a backup file",
    permissionsSection = "Permissions",
    permissionBackground = "Location “all the time”",
    allowAlways = "Allow all the time",
    permissionNotifications = "Notifications",
    allowNotifications = "Allow notifications",
    permissionBattery = "Unrestricted battery",
    disableBattery = "Turn off battery optimization",
    permissionAlarms = "Exact alarms",
    allowAlarms = "Allow exact alarms",
    permissionDocuments = "Access to the Documents folder",
    allowAllFiles = "Allow access to all files",
    granted = "Granted",
    needed = "Needed",
    appearanceSection = "Appearance",
    themeDark = "Dark",
    themeLight = "Light",
    themeSystem = "System",
    dynamicColor = "Material You dynamic colors",
    backgroundTitle = "Allow all the time",
    backgroundBody = "The geofence must fire while the app is closed. On the next screen, choose “Allow all the time”.",
    continueAction = "Continue",
    notNow = "Not now",
    ruleEnabled = "Rule enabled",
    fromPrefix = "From",
    untilPrefix = "Until",
    hoursLabel = "Hours",
    hoursFractionHint = "11.5 or 11,5 is kept as written — no rounding.",
    alarmPrefix = "Alarm backup",
    alarmOnHint = "Runs only with a fresh GPS fix better than 50 m. Otherwise the shift stays open.",
    alarmOffHint = "Off: only the geofence writes hours.",
    deleteRule = "Delete rule",
    timeWindow = "Window time",
    done = "Done",
    cancel = "Cancel",
    gpsNoResponse = "GPS did not respond. Place the pin on the map yourself.",
    coordinatesInvalid = "The map point is outside valid coordinates",
    pointSaved = "Workplace saved",
    coordinateBounds = "Latitude from −90 to 90, longitude from −180 to 180",
    defaultWorkplaceName = "Work",
    rateNotNumber = "The rate must be a number",
    defaultShiftName = "Shift",
    ruleHoursOutOfRange = "Rule hours: from 0.5 to 24",
    newRuleName = "New rule",
    currentMonthCopied = "Current month report copied",
    back = "Back",
    searchHint = "Street, number, city",
    searching = "Searching…",
    scheme = "Map",
    satellite = "Satellite",
    zoomIn = "Zoom in",
    zoomOut = "Zoom out",
    myLocation = "Where I am",
    pinCenter = "Workplace pin",
    radiusPattern = { meters -> "Radius $meters m" },
    mapHint = "Place the pin on work and choose the zone radius.",
    saveThisPoint = "Save this place",
    mapAttribution = "Imagery © Esri, Maxar · labels © Esri · search © OpenStreetMap",
    shiftRecordedTitle = "Shift saved!",
    channelShifts = "Shifts",
    channelShiftsDescription = "Automatic record of worked hours",
    channelPresence = "Presence at work",
    channelPresenceDescription = "Arrival, confirmation, and short visits",
    newShift = "New shift",
    editShift = "Edit shift",
    oneDayHint = "One day — one record. Hours already saved for the chosen day will be replaced.",
    dateLabel = "Date",
    choose = "Choose",
    quickPick = "Quick pick",
    hoursFieldHint = "From 0.5 to 24. A fraction can use a comma or a dot.",
    save = "Save",
    deleteRecord = "Delete record",
    close = "Close",
    language = AppLanguage.EN,
)
