package com.bot.TelegramBot.repository;

import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
}
