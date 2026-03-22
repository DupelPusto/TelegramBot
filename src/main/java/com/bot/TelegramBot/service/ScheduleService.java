package com.bot.TelegramBot.service;

import com.bot.TelegramBot.dto.LessonTimeDto;
import com.bot.TelegramBot.entities.LessonTime;
import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.ScheduleItemRepository;
import com.bot.TelegramBot.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleItemRepository scheduleRepo;
    private final StudentRepository studentRepo;

    @Transactional
    public String getScheduleForWeek(Long chatId) {
        Optional<Student> optStudent = studentRepo.findByChatId(chatId);

        if (optStudent.isEmpty()) {
            return "Упс...Я тебя пока не знаю. Введи свой инвайт код";
        }

        Student student = optStudent.get();
        StringBuilder weekSchedule = new StringBuilder();
        weekSchedule.append("Розклад на тиждень для ").append(student.getName()).append(" ").append(student.getSurname()).append(":\n\n");

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {

            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) continue;

            List<ScheduleItem> lessons = scheduleRepo.findAllByDayOfWeek(dayOfWeek);

            if (lessons.isEmpty()) continue;

            String dayName = switch (dayOfWeek) {
                case MONDAY -> "ПОНЕДІЛОК";
                case TUESDAY -> "ВІВТОРОК";
                case WEDNESDAY -> "СЕРЕДА";
                case THURSDAY -> "ЧЕТВЕР";
                case FRIDAY -> "П'ЯТНИЦЯ";
                default -> "";
            };

            weekSchedule.append("<b>").append(dayName).append(":</b>\n");
            for (ScheduleItem item : lessons) {
                if (item.getSubject().isSelectiveSub() && !student.getSubjects().contains(item.getSubject())) {
                    continue;
                }
                weekSchedule.append("<b>").append(item.getLessonNumber()).append(".</b> ").append(item.getSubject().getLessonName()).append("\n");

            }
            weekSchedule.append("\n");
        }
        return weekSchedule.toString();
    }

    @Transactional
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

            sb.append("\n<b>").append(item.getLessonNumber()).append(". ").append(item.getSubject().getLessonName()).append("</b>\n");
            sb.append("<a href='").append(item.getSubject().getZoomLink()).append("'>ПОСИЛАННЯ НА ПАРУ</a>\n");
            sb.append("<b>Аудиторія ").append(item.getAuditory()).append("</b>\n");

        }
        return sb.toString();
    }

    @Transactional
    public String getLink(Long chatId){

        Optional<Student> optStudent = studentRepo.findByChatId(chatId);

        if(optStudent.isEmpty()){
            return "Упс...Я тебя пока не знаю. Введи свой инвайт код";
        }

        DayOfWeek today = LocalDate.now().getDayOfWeek();

        if (today == DayOfWeek.SUNDAY || today == DayOfWeek.SATURDAY){
            return "Сегодня выходной, отдыхай";
        }

        List<ScheduleItem> lessons = scheduleRepo.findAllByDayOfWeek(today);
        if (lessons.isEmpty()){
            return "На сегодня пар не найдено";
        }


        LessonTimeDto dto = LessonTime.getNumOfLesson();
        Student student = optStudent.get();



        if (dto.pairNumber() == null) return dto.message();

        Optional<ScheduleItem> optional = scheduleRepo.findByDayOfWeekAndLessonNumber(today, dto.pairNumber());
        if (optional.isEmpty()){
            return "Сейчас окно, отдыхай. Но не забудь за следующую пару!";
        }
        ScheduleItem currentLesson = optional.get();

        boolean hasSubject = false;
        if (student.getSubjects() != null) {
            hasSubject = student.getSubjects().stream()
                    .anyMatch(s -> s.getId().equals(currentLesson.getSubject().getId()));
        }


        if (currentLesson.getSubject().isSelectiveSub() && !hasSubject) {
            return "У тебя сейчас окно, отдыхай. Но не забудь про следующую пару!";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dto.message());

        stringBuilder.append(currentLesson.getSubject().getLessonName()).append("\n");
        stringBuilder.append("Викладач: ").append(currentLesson.getSubject().getTeacher()).append("\n");
        stringBuilder.append("Посилання на пару: <a href='").append(currentLesson.getSubject().getZoomLink()).append("'>НАТИСНИ</a>\n");

        return stringBuilder.toString();
    }

    public String addScheduleItem(ScheduleItem scheduleItem){
        scheduleRepo.save(scheduleItem);
        String response = String.format("Добавлен элемент расписания:%nПредмет: %s%nДень тижня: %s%nНомер пари: %d%nАудиторія: %s",
                scheduleItem.getSubject().getLessonName(), scheduleItem.getDayOfWeek(),
                scheduleItem.getLessonNumber(), scheduleItem.getAuditory());
        return response;
    }
}
