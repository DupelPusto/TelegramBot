package com.bot.TelegramBot.service;

import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepo;


    public String addStudent(Student student){
        studentRepo.save(student);
        String responce = String.format("Студент добавлен:%nСтудент: %s %s%nИнвайт-код: %s%nВыборочные предметы: %s",
                student.getName(), student.getSurname(), student.getInviteCode(), student.getSubjects());
        return responce;
    }

}
