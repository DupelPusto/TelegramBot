package com.bot.TelegramBot.service;

import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.ScheduleItemRepository;
import com.bot.TelegramBot.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleItemRepository scheduleRepo;
    private final StudentRepository studentRepo;

    public String getScheduleForToday(Long chatId){

        Optional<Student> optStudent = studentRepo.findByChatId(chatId);

        if(optStudent.isEmpty()){
            return "Упс...Я тебя пока не знаю. Введи свой инвайт код";
        }

        Student student = optStudent.get();

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if (today == DayOfWeek.SUNDAY || today == DayOfWeek.SATURDAY){
            return "Сегодня выходной, отдыхай";
        }

        List<ScheduleItem> lessons = scheduleRepo.findAllByDayOfWeek(today);
        if (lessons.isEmpty()){
            return "На сегодня пар не найдено";
        }

        StringBuilder sb = new StringBuilder("Расписание на сегодня для ");
        sb.append(student.getName()).append(" ").append(student.getSurname()).append(": \n");
        for (ScheduleItem item : lessons){
            if (item.getSubject().isSelectiveSub() && !student.getSubjects().contains(item.getSubject())){
                    continue;
            }
            sb.append(item.getLessonNumber()).append(". ").append(item.getSubject().getLessonName()).append(", ").append(item.getSubject().getZoomLink()).append("\n");
        }
        return sb.toString();
    }
}
