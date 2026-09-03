package com.bot.TelegramBot.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long chatId;

    private String name;

    private String surname;

    @Column(unique = true)
    private String inviteCode;

    @ManyToMany
    private Set<Subject> subjects;


    public String toTelegramFormat(){
        StringBuilder studentInfo = new StringBuilder("------------------------------");
        studentInfo.append("\nСтудент: ").append(getName()).append(" ").append(getSurname()).append("\n");
        studentInfo.append("ID: <code>").append(getId()).append("</code>\n");
        studentInfo.append("Инвайт-код: <code>").append(getInviteCode()).append("</code>\n");
        studentInfo.append("Выборочные предметы: ");
        if (getSubjects() == null || getSubjects().isEmpty()) {
            studentInfo.append("отсутствуют");
        } else {
            for (Subject s : getSubjects()) {
                if (s != null) {
                    studentInfo.append(s.getLessonName()).append(" ");
                }
            }
        }
        studentInfo.append("\n------------------------------");
        return studentInfo.toString();
    }

    public String toEditFormat(){
        StringBuilder studentInfo = new StringBuilder();
        studentInfo.append("\nСтудент: ").append(getName()).append(" ").append(getSurname()).append("\n");
        studentInfo.append("ID: ").append(getId()).append("\n");
        studentInfo.append("Инвайт-код: ").append(getInviteCode()).append("\n");
        studentInfo.append("Выборочные предметы: ");
        if (getSubjects() == null || getSubjects().isEmpty()) {
            studentInfo.append("отсутствуют");
        } else {
            for (Subject s : getSubjects()) {
                if (s != null) {
                    studentInfo.append(s.getLessonName()).append(" ");
                }
            }
        }
        return studentInfo.toString();
    }
}
