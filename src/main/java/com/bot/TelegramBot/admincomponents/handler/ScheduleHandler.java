package com.bot.TelegramBot.admincomponents.handler;

import com.bot.TelegramBot.admincomponents.AdminCommands;
import com.bot.TelegramBot.admincomponents.AdminState;
import com.bot.TelegramBot.admincomponents.Handleable;
import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.dto.PageDto;
import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import com.bot.TelegramBot.service.ScheduleService;
import com.bot.TelegramBot.util.DateUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ScheduleHandler implements Handleable {

    private Map<Long, ScheduleItem> draftSItems = new ConcurrentHashMap<>();
    private final SubjectRepository subjectRepo;
    private final ScheduleService scheduleService;

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state.name().startsWith("SCHITEM")) return true;
        if (state == AdminState.FREE && text.startsWith("SCHEDULE_PAGE_")) return true;
        return state == AdminState.FREE && text.startsWith("/schedule");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {

        if (text.startsWith("SCHEDULE_PAGE_")){
            int page = Integer.parseInt(text.replace("SCHEDULE_PAGE_", ""));
            return editScheduleMessage(chatId, messageId, page);
        }

        switch (state) {
            case FREE:
                if (text.equals(AdminCommands.ADD_SCHITEM)){
                    return createSchItemStart(chatId);
                }
                if (text.equals(AdminCommands.SHOW_SCHEDULE)){
                    return showScheduleItem(chatId);
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

    private HandlerResponseDto showScheduleItem(Long chatId){

        int initPage = 0;
        PageDto dto = scheduleService.showScheduleItems(initPage);

        SendMessage sm = new SendMessage();
        sm.setParseMode("HTML");
        sm.setChatId(chatId);
        sm.setText(dto.text());
        InlineKeyboardMarkup keyboard = createInlineMarkupKeyboard(dto);
        if (keyboard != null) sm.setReplyMarkup(keyboard);
        return new HandlerResponseDto(sm, AdminState.FREE);
    }

    private HandlerResponseDto editScheduleMessage(Long tgId, Integer messageId, int page){
        PageDto dto = scheduleService.showScheduleItems(page);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setParseMode("HTML");
        editMessageText.setMessageId(messageId);
        editMessageText.setChatId(tgId);
        editMessageText.setText(dto.text());

        InlineKeyboardMarkup keyboard = createInlineMarkupKeyboard(dto);
        if (keyboard != null) editMessageText.setReplyMarkup(keyboard);
        return new HandlerResponseDto(editMessageText, AdminState.FREE);
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

    private InlineKeyboardMarkup createInlineMarkupKeyboard(PageDto dto){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        if (dto.currentPage() > 0){
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("<--");
            backButton.setCallbackData("SCHEDULE_PAGE_" + (dto.currentPage() - 1));
            rowInLine.add(backButton);
        }

        if (dto.currentPage() < dto.totalPages() - 1){
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("-->");
            nextButton.setCallbackData("SCHEDULE_PAGE_" + (dto.currentPage() + 1));
            rowInLine.add(nextButton);
        }

        if (!rowInLine.isEmpty()){
            rowsInLine.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            return inlineKeyboardMarkup;
        }

        return null;
    }

}






