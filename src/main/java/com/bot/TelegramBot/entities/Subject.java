package com.bot.TelegramBot.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String lessonName;

    private String zoomLink;

    private String teacher;

    private boolean isSelectiveSub;

    @Override
    public String toString() {
        return lessonName;
    }


    public String toTelegramFormat(){
        StringBuilder subjectInfo = new StringBuilder("------------------------------");
        subjectInfo.append("\nПредмет: ").append(getLessonName()).append("\n");
        subjectInfo.append("ID: <code>").append(getId()).append("</code>\n");
        subjectInfo.append("Преподователь: ").append(getTeacher()).append("\n");
        String isSelective = isSelectiveSub() ? "Да" : "Нет";
        subjectInfo.append("Выборочный предмет: ").append(isSelective).append("\n");
        subjectInfo.append("Ссылка на пару: ").append(getZoomLink());
        subjectInfo.append("\n------------------------------");
        return subjectInfo.toString();
    }
}
