package com.bot.TelegramBot.entities;

import com.bot.TelegramBot.dto.LessonTimeDto;
import lombok.Getter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;



@Getter
public enum LessonTime {

    FIRST_PAIR(1,LocalTime.of(8,0), LocalTime.of(9,20)),
    SECOND_PAIR(2, LocalTime.of(9,45), LocalTime.of(11,5)),
    THIRD_PAIR(3, LocalTime.of(11,30), LocalTime.of(12,50)),
    FOURTH_PAIR(4, LocalTime.of(13,15), LocalTime.of(14,35)),
    FIFTH_PAIR(5, LocalTime.of(15,0), LocalTime.of(16,20));

    private final int lessonNumber;
    private final LocalTime startTime;
    private final LocalTime endTime;


    LessonTime(int lessonNumber, LocalTime startTime, LocalTime endTime) {
        this.lessonNumber = lessonNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }


    public static LessonTimeDto getNumOfLesson(){

        LocalTime now = LocalTime.now();
        for (LessonTime lesson : LessonTime.values()){
            if (lesson.isInRange(now)){
                return new LessonTimeDto(String.format("Сейчас %d пара, ", lesson.lessonNumber), lesson.getLessonNumber());
            }
        }
        for (LessonTime lesson : LessonTime.values()){
            if (!now.isAfter(lesson.startTime)){
                return new LessonTimeDto(String.format("Сейчас перемена, следующая пара №%d, ", lesson.lessonNumber), lesson.lessonNumber);
            }
        }
        return new LessonTimeDto("На сегодня пары закончились, отдыхай!", null);
    }

    private boolean isInRange(LocalTime now){
        return !now.isBefore(this.startTime) && !now.isAfter(this.endTime);
    }
}

