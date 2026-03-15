package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class StudentHandler implements Handleable{

    private Map<Long, Student> draftStudents = new ConcurrentHashMap<>();

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state.name().startsWith("STUDENT")) return true;
        return state == AdminState.FREE && text.startsWith("/student");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state) {
        return null;
    }


    private HandlerResponseDto createStudentStart(Long chatId){

        Student student = new Student();
        draftStudents.put(chatId, student);
        String response = "Введи Имя студента";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.STUDENT_WAITING_FOR_NAME);
    }

    private SendMessage createStudentName(Long tgId, String text){

        draftStudents.get(tgId).setName(text);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_SURNAME);
        String response = "Введи Фамилию студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentSurname(Long tgId, String text){

        draftStudents.get(tgId).setSurname(text);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_INVITE_CODE);
        String responce = String.format("Введи инвайт-код для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentInviteCode(Long tgId, String text){

        draftStudents.get(tgId).setInviteCode(text);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_SUBJECTS);
        String responce = String.format("Введи выборочные предметы для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentFinish(Long tgId, String text){

        String[] subjects = text.split(",");
        Set<Subject> drafrStSubjects = new HashSet<>();
        List<String> notFoundSubjects = new ArrayList<>();
        for (String lesson : subjects){
            Optional<Subject> sub = subjectRepo.findByLessonName(lesson.trim());
            if (sub.isPresent()){
                drafrStSubjects.add(sub.get());
            }else {
                notFoundSubjects.add(lesson.trim());
            }


        }
        String response = null;
        if (notFoundSubjects.isEmpty()){
            draftStudents.get(tgId).setSubjects(drafrStSubjects);
            adminStates.put(tgId, AdminState.FREE);
            response = studentService.addStudent(draftStudents.get(tgId));
            draftStudents.remove(tgId);
        } else {
            response = String.format("Не найдены следующие предметы: %s. Попробуй ввести снова:", notFoundSubjects);
        }


        return createMessage(tgId, response);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }
}
