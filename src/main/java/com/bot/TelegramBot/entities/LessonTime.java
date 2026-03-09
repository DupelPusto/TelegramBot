package com.bot.TelegramBot.entities;

import lombok.Getter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;



@Getter
public enum LessonTime {

    FIRST(1,LocalTime.of(8,0), LocalTime.of(9,20)),
    SECOND(2, LocalTime.of(9,45), LocalTime.of(11,5)),
    THIRD(3, LocalTime.of(11,30), LocalTime.of(12,50)),
    FOURTH(4, LocalTime.of(13,15), LocalTime.of(14,35)),
    FIFTH(5, LocalTime.of(15,0), LocalTime.of(16,20));

    private final int lessonNumber;
    private final LocalTime startTime;
    private final LocalTime endTime;


    LessonTime(int lessonNumber, LocalTime startTime, LocalTime endTime) {
        this.lessonNumber = lessonNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Integer getNumOfLesson(){
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY){
            return null;
        }
        for (LessonTime lesson : LessonTime.values()) {
            if (now.isBefore(lesson.getEndTime())) {
                return lesson.getLessonNumber();
            }
        }
        return null;
    }
}

