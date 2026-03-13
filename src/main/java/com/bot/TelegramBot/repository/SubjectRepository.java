package com.bot.TelegramBot.repository;

import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    Optional<Subject> findByLessonName(String lessonName);
}
