package com.bot.TelegramBot.repository;

import com.bot.TelegramBot.entities.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem,Long> {

    List<ScheduleItem> findAllByDayOfWeek(DayOfWeek dayOfWeek);
}
