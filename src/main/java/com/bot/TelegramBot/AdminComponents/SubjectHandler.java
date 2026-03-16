package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SubjectHandler implements Handleable{

    private final SubjectService subjectService;
    private Map<Long, Subject> draftSubjects = new ConcurrentHashMap<>();

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state.name().startsWith("LESSON")) return true;
        if (state == AdminState.FREE && text.startsWith("SUBJECT_PAGE_")) return true;
        return state == AdminState.FREE && text.startsWith("/subject");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {


        switch (state){

            case FREE:
                if (text.equals(AdminCommands.ADD_SUBJECT)){
                    return createSubjectStart(chatId);
                }
                break;
            case LESSON_WAITING_FOR_NAME:
                return createSubjectName(chatId, text);
            case LESSON_WAITING_FOR_LINK:
                return createSubjectLink(chatId, text);
            case LESSON_WAITING_FOR_TEACHER:
                return createSubjectTeacher(chatId, text);
            case LESSON_WAITING_FOR_SELECTIVE:
                return createSubjectFinish(chatId, text);

        }

        return new HandlerResponseDto(createMessage(chatId,"Неизвестная команда, попробуй еще раз"), state);
    }

    private HandlerResponseDto createSubjectStart(Long chatId){

        Subject subject = new Subject();
        draftSubjects.put(chatId, subject);
        String responce = "Введи название предмета:";
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.LESSON_WAITING_FOR_NAME);
    }

    private HandlerResponseDto createSubjectName(Long chatId, String text){

        draftSubjects.get(chatId).setLessonName(text);
        String responce = "Введи ссылку на предмет:";
        return new HandlerResponseDto(createMessage(chatId,responce), AdminState.LESSON_WAITING_FOR_LINK);
    }

    private HandlerResponseDto createSubjectLink(Long chatId, String text){

        draftSubjects.get(chatId).setZoomLink(text);
        String responce = "Введи имя преподователя:";
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.LESSON_WAITING_FOR_TEACHER);
    }

    private HandlerResponseDto createSubjectTeacher(Long chatId, String text){

        draftSubjects.get(chatId).setTeacher(text);
        String responce = "Это выборочный предмет?(+,-):";
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.LESSON_WAITING_FOR_SELECTIVE);
    }

    private HandlerResponseDto createSubjectFinish(Long chatId, String text){

        if (text.equals("+")){
            draftSubjects.get(chatId).setSelectiveSub(true);
        } else if (text.equals("-")){
            draftSubjects.get(chatId).setSelectiveSub(false);
        }else {
            return new HandlerResponseDto(createMessage(chatId, "Неверный параметр выборочного предмета. Повторите попытку"),
                    AdminState.LESSON_WAITING_FOR_SELECTIVE);
        }

        String responce = subjectService.addSubject(draftSubjects.get(chatId));
        draftSubjects.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.FREE);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }
}
