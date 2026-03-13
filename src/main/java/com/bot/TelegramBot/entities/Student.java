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
        StringBuilder studentInfo = new StringBuilder("--------------------");
        studentInfo.append("\nСтудент: ").append(getName()).append(" ").append(getSurname()).append("\n");
        studentInfo.append("ID: ").append(getId()).append("\n");
        studentInfo.append("Инвайт-код: ").append(getInviteCode()).append("\n");
        studentInfo.append("Выборочные предметы: ");
        for (Subject s : getSubjects()){
            if (getSubjects() == null){
                studentInfo.append("отсутствуют");
            }
            if (s != null) {
                studentInfo.append(s.getLessonName()).append(" ");
            }
        }
        studentInfo.append("\n--------------------");
        return studentInfo.toString();
    }
}
