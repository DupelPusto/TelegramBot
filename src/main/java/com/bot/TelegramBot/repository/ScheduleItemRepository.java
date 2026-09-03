package com.bot.TelegramBot.repository;

import com.bot.TelegramBot.entities.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem,Long> {

    List<ScheduleItem> findAllByDayOfWeekOrderByLessonNumberAsc(DayOfWeek dayOfWeek);

    Optional<ScheduleItem> findByDayOfWeekAndLessonNumber(DayOfWeek dayOfWeek, Integer lessonNumber);

    Optional<ScheduleItem> findById(Long id);
}
