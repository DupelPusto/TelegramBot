package com.bot.TelegramBot.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.DayOfWeek;

@Entity
@Getter
@Setter
public class ScheduleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private Integer lessonNumber;

    private String auditory;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    public String toTelegramFormat(){
        StringBuilder scheduleItemInfo = new StringBuilder("------------------------------");
        scheduleItemInfo.append("\nID: <code>").append(getId()).append("</code>\n");
        scheduleItemInfo.append("День недели: ").append(getDayOfWeek()).append("\n");
        scheduleItemInfo.append("Номер пары: ").append(getLessonNumber()).append("\n");
        scheduleItemInfo.append("Аудитория: ").append(getAuditory()).append("\n");
        scheduleItemInfo.append("Предмет: ").append(getSubject().getLessonName());
        scheduleItemInfo.append("\n------------------------------");
        return scheduleItemInfo.toString();
    }

    public String toEditFormat(){

        StringBuilder schedule = new StringBuilder();
        schedule.append("\nID: ").append(getId()).append("\n");
        schedule.append("День недели: ").append(getDayOfWeek()).append("\n");
        schedule.append("Номер пары: ").append(getLessonNumber()).append("\n");
        schedule.append("Аудитория: ").append(getAuditory()).append("\n");
        schedule.append("Предмет: ").append(getSubject().getLessonName());
        return schedule.toString();
    }
}
