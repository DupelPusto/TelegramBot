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
}
