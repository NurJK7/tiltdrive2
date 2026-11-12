package dev.tiltdrive.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalLang = compositionLocalOf { "en" }

/** English text is the key. {0}, {1}… are replaced by [args]. */
@Composable
fun tr(en: String, vararg args: Any): String {
    var s = if (LocalLang.current == "ru") RU[en] ?: en else en
    args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
    return s
}

private val RU = mapOf(
    // connect screen
    "Use your phone as a steering wheel" to "Телефон вместо руля",
    "Phone and PC on the same Wi-Fi" to "Телефон и ПК в одной сети Wi-Fi",
    "In the game: Options → Controls → Hardware → “Use your phone or tablet” = Yes" to
        "В игре: Настройки → Управление → Оборудование → «Использовать телефон или планшет» = Да",
    "Tap AUTO-CONNECT. Tilt to steer, right half = gas, left half = brake" to
        "Нажми АВТОПОДКЛЮЧЕНИЕ. Наклон — руль, правая часть — газ, левая — тормоз",
    "Dashboard (optional)" to "Приборная панель (по желанию)",
    "In the game: Options → Advanced → Protocols → OutGauge. Address {0}, port 4445, rate 60. Then reload the car (Ctrl+R)." to
        "В игре: Настройки → Расширенные параметры → Протоколы → OutGauge. Адрес {0}, порт 4445, частота 60. Затем перезагрузи машину (Ctrl+R).",
    "(join the Wi-Fi first)" to "(сначала подключись к Wi-Fi)",
    "AUTO-CONNECT" to "АВТОПОДКЛЮЧЕНИЕ",
    "Looking for the game… {0}%" to "Ищу игру… {0}%",
    "Cancel" to "Отмена",
    "Know the 5-digit code? Connect with it" to "Знаешь 5-значный код? Подключись по нему",
    "Code" to "Код",
    "PC IP address (optional)" to "IP компьютера (необязательно)",
    "Connect" to "Подключить",
    "Dashboard only" to "Только приборка",
    "Use this phone as a display, no pairing needed" to "Телефон как экран приборки, подключение не нужно",
    "Game not found. Same Wi-Fi? In the game set Options → Controls → Hardware → “Use your phone or tablet” to Yes, and allow BeamNG.drive in the Windows firewall." to
        "Игра не найдена. Одна сеть Wi-Fi? В игре включи Настройки → Управление → Оборудование → «Использовать телефон или планшет» и разреши BeamNG.drive в брандмауэре Windows.",
    // drive screen
    "Connected" to "Подключено",
    "Connected · no dashboard data" to "Подключено · нет данных приборки",
    "No telemetry from the game" to "Игра не шлёт телеметрию",
    "Nothing received on :4445. In the game set OutGauge to this phone's IP, port 4445, then Ctrl+R." to
        "На :4445 ничего не приходит. В игре укажи в OutGauge IP этого телефона, порт 4445, затем Ctrl+R.",
    "{0} · data not recognised" to "{0} · данные не распознаны",
    "BRAKE" to "ТОРМОЗ",
    "GAS" to "ГАЗ",
    "Tilt" to "Наклон",
    "Wheel" to "Руль",
    "Arrows" to "Стрелки",
    "Center" to "Центр",
    "Dash" to "Приборка",
    "Controls" to "Управление",
    "Settings" to "Настройки",
    "Exit" to "Выход",
    "km/h" to "км/ч",
    "mph" to "mph",
    "FUEL" to "ТОПЛ",
    "TEMP" to "ТЕМП",
    // settings
    "‹ Done" to "‹ Готово",
    "Steering" to "Руль",
    "Dashboard" to "Приборка",
    "Language" to "Язык",
    "About" to "О приложении",
    "Control" to "Управление",
    "Steering control" to "Способ управления",
    "Reverse steering" to "Реверс руля",
    "Sensitivity" to "Чувствительность",
    "Steering range" to "Диапазон руля",
    "Dead zone (tilt)" to "Мёртвая зона (наклон)",
    "Smoothing (tilt)" to "Сглаживание (наклон)",
    "Style" to "Стиль",
    "Dashboard style" to "Стиль приборки",
    "Tap the dashboard while driving to switch the style" to "Нажми на приборку во время езды, чтобы сменить стиль",
    "Units" to "Единицы",
    "Speed in mph" to "Скорость в mph",
    "Race" to "Гонка",
    "EV" to "Электро",
    "Classic" to "Классика",
    "Unofficial app, not affiliated with BeamNG. Protocol info from github.com/BeamNG/remotecontrol (ISC license, © 2014 BeamNG)." to
        "Неофициальное приложение, не связано с BeamNG. Данные о протоколе: github.com/BeamNG/remotecontrol (лицензия ISC, © 2014 BeamNG).",
)
