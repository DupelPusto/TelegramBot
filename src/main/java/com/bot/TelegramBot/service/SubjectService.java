package com.bot.TelegramBot.service;

import com.bot.TelegramBot.dto.PageDto;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepo;
    private final StudentService studentService;

    public String addSubject(Subject subject){
        subjectRepo.save(subject);
        String response = String.format("Предмет добавлен:%nНазва предмету: %s%nВикладач: %s%nПосилання: %s%nВибірковий: %b",
                subject.getLessonName(), subject.getTeacher(), subject.getZoomLink(), subject.isSelectiveSub());
        return response;
    }

    public PageDto showSubject(int pageNumber){
        int pageSize = 5;


        Sort sort = Sort.by(Sort.Direction.ASC, "lessonName");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Subject> subjectPage = subjectRepo.findAll(pageable);
        List<Subject> subjectList = subjectPage.getContent();

        String subjects = subjectList.stream()
                .map(Subject::toTelegramFormat)
                .collect(Collectors.joining("\n"));

        if (subjects.isEmpty()) {
            return new PageDto("На этой странице пока нет предметов",
                    1,
                    0);
        }



        return new PageDto(
                "📋 Список предметов:\n" + subjects,
                subjectPage.getTotalPages(),
                pageNumber);
    }

    @Transactional
    public String removeSubject(String subjectName){
        Optional<Subject> sub = subjectRepo.findByLessonName(subjectName);
        if (sub.isEmpty()){
            return "Предмет с таким названием не найден!";
        }
        Subject subject = sub.get();
        List<Student> students = studentService.getStudents();
        for (Student s : students){
            Set<Subject> subjects = s.getSubjects();
            for (Subject subj : subjects){
                if (subjectName.equals(subj.getLessonName())){
                    subjects.remove(subj);
                }
            }
        }
        subjectRepo.delete(subject);
        return String.format("Предмет %s успешно удален!", subject.getLessonName());
    }
}
