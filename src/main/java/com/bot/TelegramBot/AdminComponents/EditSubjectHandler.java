package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EditSubjectHandler implements Handleable{

    private final SubjectRepository subjectRepo;
    private static Map<Long, Subject> editableSubjects = new ConcurrentHashMap<>();

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state == AdminState.FREE && text.equals(AdminCommands.EDIT_SUBJECT)) return true;
        if (state == AdminState.FREE && text.startsWith("EDIT_SUBJECT_")) return true;
        return state.name().startsWith("EDIT_SUBJECT");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {


        if (text.equals(AdminCommands.EDIT_SUBJECT)){
            return new HandlerResponseDto(createMessage(chatId, "Введи ID предмета для редактирования: "), AdminState.EDIT_SUBJECT_WAITING_FOR_ID);
        }

        if (text.equals("EDIT_SUBJECT_EXIT")) {
            editableSubjects.remove(chatId);
            return new HandlerResponseDto(createMessage(chatId, "Ты отменил редактирование"), AdminState.FREE);
        }

        if (state == AdminState.EDIT_SUBJECT_WAITING_FOR_ID) return showSubjectForEdit(chatId, text);

        if (state == AdminState.EDIT_SUBJECT_NAME ||
                state == AdminState.EDIT_SUBJECT_TEACHER ||
                state == AdminState.EDIT_SUBJECT_LINK ||
                state == AdminState.EDIT_SUBJECT_SELECTIVE) {

            if (!editableSubjects.containsKey(chatId)) {
                return new HandlerResponseDto(createMessage(chatId, "Время сессии истекло или бот был перезагружен. Начни редактирование заново: /edit_subject"), AdminState.FREE);
            }
        }

        switch (state){

            case EDIT_SUBJECT_NAME:
                return editSubjectName(chatId, text);
            case EDIT_SUBJECT_TEACHER:
                return editSubjectTeacher(chatId, text);
            case EDIT_SUBJECT_LINK:
                return editSubjectLink(chatId, text);
            case EDIT_SUBJECT_SELECTIVE:
                return editSubjectSelective(chatId, text);
        }

        String[] elements = text.split("_");
        int subId = Integer.parseInt(elements[3]);
        String adState = elements[0] + "_" + elements[1] + "_" + elements[2];

        switch (adState){

            case "EDIT_SUBJECT_NAME":
                editableSubjects.put(chatId, subjectRepo.findById(subId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новое название предмета:"), AdminState.EDIT_SUBJECT_NAME);
            case "EDIT_SUBJECT_TEACHER":
                editableSubjects.put(chatId, subjectRepo.findById(subId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи нового преподователя:"), AdminState.EDIT_SUBJECT_TEACHER);
            case "EDIT_SUBJECT_LINK":
                editableSubjects.put(chatId, subjectRepo.findById(subId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новую ссылку для предмета"), AdminState.EDIT_SUBJECT_LINK);
            case "EDIT_SUBJECT_SELECTIVE":
                editableSubjects.put(chatId, subjectRepo.findById(subId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новый статус предмета(+,-):"), AdminState.EDIT_SUBJECT_SELECTIVE);
        }

        return new HandlerResponseDto(createMessage(chatId, "Неизвестная ошибка"), state);
    }

    private HandlerResponseDto editSubjectSelective(Long chatId, String text){

        boolean isSelective = text.equals("+");

        editableSubjects.get(chatId).setSelectiveSub(isSelective);
        subjectRepo.save(editableSubjects.get(chatId));
        editableSubjects.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Предмет сохранен"), AdminState.FREE);
    }

    private HandlerResponseDto editSubjectLink(Long chatId, String text){

        editableSubjects.get(chatId).setZoomLink(text);
        subjectRepo.save(editableSubjects.get(chatId));
        editableSubjects.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Предмет сохранен"), AdminState.FREE);
    }

    private HandlerResponseDto editSubjectTeacher(Long chatId, String text){

        editableSubjects.get(chatId).setTeacher(text);
        subjectRepo.save(editableSubjects.get(chatId));
        editableSubjects.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Предмет сохранен"), AdminState.FREE);
    }

    private HandlerResponseDto editSubjectName(Long chatId, String text){

        editableSubjects.get(chatId).setLessonName(text);
        subjectRepo.save(editableSubjects.get(chatId));
        editableSubjects.remove(chatId);
        return new HandlerResponseDto(createMessage(chatId, "Предмет сохранен"), AdminState.FREE);

    }

    private HandlerResponseDto showSubjectForEdit(Long chatId, String text){

        int id;
        try {
            id = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return new HandlerResponseDto(createMessage(chatId, "Опечатка при вводе ID предмета! Введи целое число!"), AdminState.EDIT_SUBJECT_WAITING_FOR_ID);
        }

        Optional<Subject> editableSubject = subjectRepo.findById(id);

        if (editableSubject.isEmpty()){
            return new HandlerResponseDto(createMessage(chatId, "Предмет с таким ID не найден, попробуй ввести снова:"), AdminState.EDIT_SUBJECT_WAITING_FOR_ID);
        }

        Subject s = editableSubject.get();
        editableSubjects.put(chatId, s);

        String response = String.format("Предмет: %s%nВыбери для редактирования", s.getLessonName());
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(response);
        sm.setReplyMarkup(createMarkupKeyboard(id));

        return new HandlerResponseDto(sm, AdminState.FREE);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

    private InlineKeyboardMarkup createMarkupKeyboard(int id){

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        List<InlineKeyboardButton> row5 = new ArrayList<>();

        InlineKeyboardButton editNameButton = new InlineKeyboardButton();
        InlineKeyboardButton editTeacherButton = new InlineKeyboardButton();
        InlineKeyboardButton editLinkButton = new InlineKeyboardButton();
        InlineKeyboardButton editSelectiveButton = new InlineKeyboardButton();
        InlineKeyboardButton exitButton = new InlineKeyboardButton();

        editNameButton.setText("Назва");
        editTeacherButton.setText("Викладач");
        editLinkButton.setText("Посилання");
        editSelectiveButton.setText("Вибірковість");
        exitButton.setText("Вихід");

        editNameButton.setCallbackData("EDIT_SUBJECT_NAME_" + id);
        editTeacherButton.setCallbackData("EDIT_SUBJECT_TEACHER_" + id);
        editLinkButton.setCallbackData("EDIT_SUBJECT_LINK_" + id);
        editSelectiveButton.setCallbackData("EDIT_SUBJECT_SELECTIVE_" + id);
        exitButton.setCallbackData("EDIT_SUBJECT_EXIT");

        row1.add(editNameButton);
        row2.add(editTeacherButton);
        row3.add(editLinkButton);
        row4.add(editSelectiveButton);
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
