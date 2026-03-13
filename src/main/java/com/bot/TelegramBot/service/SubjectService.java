package com.bot.TelegramBot.service;

import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepo;

    public String addSubject(Subject subject){
        subjectRepo.save(subject);
        String response = String.format("Предмет добавлен:%nНазва предмету: %s%nВикладач: %s%nПосилання: %s%nВибірковий: %b",
                subject.getLessonName(), subject.getTeacher(), subject.getZoomLink(), subject.isSelectiveSub());
        return response;
    }
}
