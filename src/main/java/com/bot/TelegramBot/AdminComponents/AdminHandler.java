package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.repository.StudentRepository;
import com.bot.TelegramBot.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AdminHandler {

    private final StudentService studentService;
    Map<Long, AdminState> adminStates = new ConcurrentHashMap<>();
    Map<Long, Student> draftStudents = new ConcurrentHashMap<>();


    public SendMessage adminHandler(Update update){
        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        AdminState currentState = adminStates.getOrDefault(tgId, AdminState.FREE);

        switch (currentState){
            case FREE:
                if (text.equals("/add_student")){
                    return createStudentStart(update);
                }
                break;
            case WAITING_FOR_NAME:
                return createStudentName(update);
            case WAITING_FOR_SURNAME:
                return createStudentSurname(update);
            case WAITING_FOR_INVITE_CODE:
                return createStudentInviteCode(update);
            case WAITING_FOR_SUBJECTS:
                return createStudentFinish(update);

        }

        String responce = "Неизвестная команда админа";
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentStart(Update update){

        Long tgId = update.getMessage().getChatId();
        Student student = new Student();
        draftStudents.put(tgId, student);
        adminStates.put(tgId, AdminState.WAITING_FOR_NAME);
        String response = "Введи Имя студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentName(Update update){

        Long tgId = update.getMessage().getChatId();
        String studentName = update.getMessage().getText();
        draftStudents.get(tgId).setName(studentName);
        adminStates.put(tgId, AdminState.WAITING_FOR_SURNAME);
        String response = "Введи Фамилию студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentSurname(Update update){

        Long tgId = update.getMessage().getChatId();
        String studentSurname = update.getMessage().getText();
        draftStudents.get(tgId).setSurname(studentSurname);
        adminStates.put(tgId, AdminState.WAITING_FOR_INVITE_CODE);
        String responce = String.format("Введи инвайт-код для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentInviteCode(Update update){

        Long tgId = update.getMessage().getChatId();
        String studentInviteCode = update.getMessage().getText();
        draftStudents.get(tgId).setInviteCode(studentInviteCode);
        adminStates.put(tgId, AdminState.WAITING_FOR_SUBJECTS);
        String responce = String.format("Введи выборочные предметы для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentFinish(Update update){
        Long tgId = update.getMessage().getChatId();
        String studentSubjects = update.getMessage().getText();
//        String[] Subjects = studentSubjects.split(",");
//        for (String subject : Subjects){
//            draftStudents.get(tgId).getSubjects().add(subject);
//        }
        adminStates.put(tgId, AdminState.FREE);
        String responce = studentService.addStudent(draftStudents.get(tgId));
        draftStudents.remove(tgId);

        return createMessage(tgId, responce);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

}
