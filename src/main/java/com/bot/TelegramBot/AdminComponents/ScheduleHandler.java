package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import com.bot.TelegramBot.service.ScheduleService;
import com.bot.TelegramBot.util.DateUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ScheduleHandler implements Handleable{

    private Map<Long, ScheduleItem> draftSItems = new ConcurrentHashMap<>();
    private final SubjectRepository subjectRepo;
    private final ScheduleService scheduleService;

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state.name().startsWith("SCHITEM")) return true;
        return state == AdminState.FREE && text.startsWith("/schedule");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {

        switch (state) {
            case FREE:
                if (text.equals(AdminCommands.ADD_SCHITEM)){
                    return createSchItemStart(chatId);
                }
                break;
            case SCHITEM_WAITING_FOR_LESSON_NAME:
                return createSchItemName(chatId, text);
            case SCHITEM_WAITING_FOR_LESSON_NUMBER:
                return createSchItemNumber(chatId, text);
            case SCHITEM_WAITING_FOR_LESSON_AUDITORY:
                return createSchItemAuditory(chatId, text);
            case SCHITEM_WAITING_FOR_DAY:
                return createSchItemDay(chatId, text);
        }
        return new HandlerResponseDto(createMessage(chatId, "Неизвестная ошибка, попробуй снова"), state);
    }


    private HandlerResponseDto createSchItemStart(Long chatId){

        ScheduleItem scheduleItem = new ScheduleItem();
        draftSItems.put(chatId, scheduleItem);
        String response = "Введи название предмета: ";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.SCHITEM_WAITING_FOR_LESSON_NAME);
    }

    private HandlerResponseDto createSchItemName(Long chatId, String text){

        Optional<Subject> optionalSubject = subjectRepo.findByLessonName(text);

        if (optionalSubject.isPresent()){
            Subject subject = optionalSubject.get();
            draftSItems.get(chatId).setSubject(subject);
        } else {
            return new HandlerResponseDto(createMessage(chatId, "Предмет не найден, попробуй еще раз:"), AdminState.SCHITEM_WAITING_FOR_LESSON_NAME);
        }
        String response = "Введи номер пары: ";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.SCHITEM_WAITING_FOR_LESSON_NUMBER);
    }

    private HandlerResponseDto createSchItemNumber(Long chatId, String text){

        int lessonNumber;
        try {
            lessonNumber = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            String response = "Неверный формат номера, попробуй снова:";
            return new HandlerResponseDto(createMessage(chatId, response), AdminState.SCHITEM_WAITING_FOR_LESSON_NUMBER);
        }

        draftSItems.get(chatId).setLessonNumber(lessonNumber);
        String response = "Введи номер аудитории: ";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.SCHITEM_WAITING_FOR_LESSON_AUDITORY);
    }

    private HandlerResponseDto createSchItemAuditory(Long chatId, String text){

        draftSItems.get(chatId).setAuditory(text);
        String response = "Введи день недели:";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.SCHITEM_WAITING_FOR_DAY);
    }

    private HandlerResponseDto createSchItemDay(Long chatId, String text){

        DayOfWeek day = DateUtil.parseDayOfWeek(text);
        if (day == null) {
            return new HandlerResponseDto(createMessage(chatId,
                    "Неверный формат дня. Попробуй снова в формате 'Пн' или 'пн':"),
                    AdminState.SCHITEM_WAITING_FOR_DAY);
        }
        draftSItems.get(chatId).setDayOfWeek(day);
        String response = scheduleService.addScheduleItem(draftSItems.get(chatId));
        draftSItems.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

}






