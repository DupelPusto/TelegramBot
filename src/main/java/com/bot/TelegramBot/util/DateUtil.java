package com.bot.TelegramBot.util;

import java.time.DayOfWeek;
import java.util.Map;

public class DateUtil {

    private static final Map<String, DayOfWeek> DAYS_DICT = Map.of(
            "Пн", DayOfWeek.MONDAY,
            "пн", DayOfWeek.MONDAY,
            "Вт", DayOfWeek.TUESDAY,
            "вт", DayOfWeek.TUESDAY,
            "Ср", DayOfWeek.WEDNESDAY,
            "ср", DayOfWeek.WEDNESDAY,
            "Чт", DayOfWeek.THURSDAY,
            "чт", DayOfWeek.THURSDAY,
            "Пт", DayOfWeek.FRIDAY,
            "пт", DayOfWeek.FRIDAY
    );

    public static DayOfWeek parseDayOfWeek(String day){
        return DAYS_DICT.get(day);
    }
}
