package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.ScheduleItemRepository;
import com.bot.TelegramBot.repository.SubjectRepository;
import com.bot.TelegramBot.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EditScheduleItemHandler implements Handleable{

    private final ScheduleItemRepository scheduleRepo;
    private final SubjectRepository subjectRepo;
    private static Map<Long, ScheduleItem> editableItems = new ConcurrentHashMap<>();

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state == AdminState.FREE && text.equals(AdminCommands.EDIT_SCHITEM)) return true;
        if (state == AdminState.FREE && text.startsWith("EDIT_SCHITEM_")) return true;
        return state.name().startsWith("EDIT_SCHITEM");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {

        if (text.equals(AdminCommands.EDIT_SCHITEM)){
            return new HandlerResponseDto(createMessage(chatId, "Введи ID элемента расписания для редактирования: "), AdminState.EDIT_SCHITEM_WAITING_FOR_ID);
        }

        if (text.equals("EDIT_SCHITEM_EXIT")) {
            editableItems.remove(chatId);
            return new HandlerResponseDto(createMessage(chatId, "Ты отменил редактирование"), AdminState.FREE);
        }

        if (state == AdminState.EDIT_SCHITEM_WAITING_FOR_ID) return showItemForEdit(chatId, text);

        if (state == AdminState.EDIT_SCHITEM_DAY ||
                state == AdminState.EDIT_SCHITEM_AUDITORY ||
                state == AdminState.EDIT_SCHITEM_LESNUMBER ||
                state == AdminState.EDIT_SCHITEM_SUBJECT) {

            if (!editableItems.containsKey(chatId)) {
                return new HandlerResponseDto(createMessage(chatId, "Время сессии истекло или бот был перезагружен. Начни редактирование заново: /edit_schedule"), AdminState.FREE);
            }
        }


        switch (state){

            case EDIT_SCHITEM_LESNUMBER:
                return editSchitemLessonNumber(chatId, text);
            case EDIT_SCHITEM_DAY:
                return editSchitemDay(chatId, text);
            case EDIT_SCHITEM_SUBJECT:
                return editSchitemSubject(chatId, text);
            case EDIT_SCHITEM_AUDITORY:
                return editSchitemAuditory(chatId, text);
        }

        String[] elements = text.split("_");
        Long scheduleId = Long.parseLong(elements[3]);
        String adState = elements[0] + "_" + elements[1] + "_" + elements[2];

        switch (adState){

            case "EDIT_SCHITEM_LESNUMBER":
                editableItems.put(chatId, scheduleRepo.findById(scheduleId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новый номер пары для лемента расписания:"), AdminState.EDIT_SCHITEM_LESNUMBER);
            case "EDIT_SCHITEM_DAY":
                editableItems.put(chatId, scheduleRepo.findById(scheduleId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новый день недели для элемента расписания: "), AdminState.EDIT_SCHITEM_DAY);
            case "EDIT_SCHITEM_SUBJECT":
                editableItems.put(chatId, scheduleRepo.findById(scheduleId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новый предмет для элемента расписания:"), AdminState.EDIT_SCHITEM_SUBJECT);
            case "EDIT_SCHITEM_AUDITORY":
                editableItems.put(chatId, scheduleRepo.findById(scheduleId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новую аудиторию для элемента расписания: "), AdminState.EDIT_SCHITEM_AUDITORY);
        }

        return new HandlerResponseDto(createMessage(chatId, "Неизвестная ошибка"), state);
    }

    private HandlerResponseDto editSchitemSubject(Long chatId, String text){

        Optional<Subject> subject = subjectRepo.findByLessonName(text);
        if (subject.isEmpty()) return new HandlerResponseDto(createMessage(chatId, "Предмет не найден!"), AdminState.FREE);
        Subject s = subject.get();

        editableItems.get(chatId).setSubject(s);
        scheduleRepo.save(editableItems.get(chatId));
        editableItems.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Элемент расписания сохранен!"), AdminState.FREE);
    }

    private HandlerResponseDto editSchitemLessonNumber(Long chatId, String text){
        try {
            editableItems.get(chatId).setLessonNumber(Integer.parseInt(text));
        } catch (NumberFormatException e){
            return new HandlerResponseDto(createMessage(chatId, "Опечатка при вводе номера пары! Введи целое число!"), AdminState.FREE);
        }

        scheduleRepo.save(editableItems.get(chatId));
        editableItems.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Элемент расписания сохранен!"), AdminState.FREE);
    }

    private HandlerResponseDto editSchitemAuditory(Long chatId, String text){

        editableItems.get(chatId).setAuditory(text);
        scheduleRepo.save(editableItems.get(chatId));
        editableItems.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Элемент расписания сохранен!"), AdminState.FREE);
    }

    private HandlerResponseDto editSchitemDay(Long chatId, String text){

        DayOfWeek day = DateUtil.parseDayOfWeek(text);
        if (day == null) return new HandlerResponseDto(createMessage(chatId, "Такого дня недели не существует! Проверь и попробуй снова!"), AdminState.FREE);
        editableItems.get(chatId).setDayOfWeek(day);
        scheduleRepo.save(editableItems.get(chatId));
        editableItems.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Элемент расписания сохранен!"), AdminState.FREE);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

    private HandlerResponseDto showItemForEdit(Long chatId, String text){

        int id;
        try {
            id = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return new HandlerResponseDto(createMessage(chatId, "Опечатка при вводе ID элемента расписания! Введи целое число!"), AdminState.EDIT_SCHITEM_WAITING_FOR_ID);
        }


        Optional<ScheduleItem> editableItem = scheduleRepo.findById(Long.parseLong(String.valueOf(id)));

        if (editableItem.isEmpty()){
            return new HandlerResponseDto(createMessage(chatId, "Элемент расписания с таким ID не найден, попробуй ввести снова:"), AdminState.EDIT_SCHITEM_WAITING_FOR_ID);
        }

        ScheduleItem s = editableItem.get();
        editableItems.put(chatId, s);

        String response = String.format("Элемент расписания: %s%nВыбери для редактирования", s.toTelegramFormat());
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(response);
        sm.setParseMode("HTML");
        sm.setReplyMarkup(createMarkupKeyboard(id));

        return new HandlerResponseDto(sm, AdminState.FREE);
    }

    private InlineKeyboardMarkup createMarkupKeyboard(int id){

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        List<InlineKeyboardButton> row5 = new ArrayList<>();

        InlineKeyboardButton editDayOfWeekButton = new InlineKeyboardButton();
        InlineKeyboardButton editLessonNumberButton = new InlineKeyboardButton();
        InlineKeyboardButton editAuditoryButton = new InlineKeyboardButton();
        InlineKeyboardButton editSubjectButton = new InlineKeyboardButton();
        InlineKeyboardButton exitButton = new InlineKeyboardButton();

        editDayOfWeekButton.setText("День тижня");
        editLessonNumberButton.setText("Номер пари");
        editAuditoryButton.setText("Аудиторія");
        editSubjectButton.setText("Предмет");
        exitButton.setText("Вихід");

        editDayOfWeekButton.setCallbackData("EDIT_SCHITEM_DAY_" + id);
        editLessonNumberButton.setCallbackData("EDIT_SCHITEM_LESNUMBER_" + id);
        editAuditoryButton.setCallbackData("EDIT_SCHITEM_AUDITORY_" + id);
        editSubjectButton.setCallbackData("EDIT_SCHITEM_SUBJECT_" + id);
        exitButton.setCallbackData("EDIT_SCHITEM_EXIT");

        row1.add(editDayOfWeekButton);
        row2.add(editLessonNumberButton);
        row3.add(editAuditoryButton);
        row4.add(editSubjectButton);
        row5.add(exitButton);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);
        keyboard.setKeyboard(rows);

        return keyboard;
    }
}
