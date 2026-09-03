package com.bot.TelegramBot.service;

import com.bot.TelegramBot.dto.PageDto;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepo;

    public boolean isRegistered(Long chatId){
        return studentRepo.existsByChatId(chatId);
    }

    @Transactional
    public PageDto showStudent(int pageNumber){
        int pageSize = 5;


        Sort sort = Sort.by(Direction.ASC, "surname");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Student> studentPage = studentRepo.findAll(pageable);
        List<Student> studentList = studentPage.getContent();

        String students = studentList.stream()
                .map(Student::toTelegramFormat)
                .collect(Collectors.joining("\n"));

        if (students.isEmpty()) {
            return new PageDto("На этой странице пока нет студентов",
                    1,
                    0);
        }



        return new PageDto(
                "📋 Список студентов:\n" + students,
                studentPage.getTotalPages(),
                pageNumber);
    }

    public String addStudent(Student student){
        studentRepo.save(student);

        StringBuilder sb = new StringBuilder("Студент добавлен:");
        sb.append("\n").append("Cтудент: ").append(student.getName()).append(" ").append(student.getSurname());
        sb.append("\n").append("Инвайт-код: ").append(student.getInviteCode());
        sb.append("\n").append("Выборочные предметы:");
        for (Subject s : student.getSubjects()){
            if (s != null){
                sb.append("\n").append(s);
            }
        }
        return sb.toString();
    }

    public String registerUser(Long chatId, String inviteCode){
        Optional<Student> student = studentRepo.findByInviteCode(inviteCode);
        if (student.isPresent()){
            Student readyStudent = student.get();
            if (readyStudent.getChatId() != null){
                return "Пользователь с этим инвайт-кодом зарегистрирован!";
            }
            readyStudent.setChatId(chatId);
            studentRepo.save(readyStudent);
            String responce = String.format("Привет, %s %s, ты успешно привязал аккаунт!",
                    student.get().getName(), student.get().getSurname());
            return responce;
        }
        return "Несуществующий инвайт-код. Попробуй снова или обратись к администратору";
    }

    public List<Student> getStudents(){
        return studentRepo.findAll();
    }

    public String removeStudent(Long id){
        Optional<Student> st = studentRepo.findById(id);
        if (st.isEmpty()) return "Студент с таким ID не найден";
        Student student = st.get();
        studentRepo.delete(student);
        return String.format("Студент %s %s успешно удален", student.getName(), student.getSurname());
    }

}
