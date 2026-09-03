package com.bot.TelegramBot.admincomponents.handler;

import com.bot.TelegramBot.admincomponents.AdminCommands;
import com.bot.TelegramBot.admincomponents.AdminState;
import com.bot.TelegramBot.admincomponents.Handleable;
import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.dto.PageDto;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SubjectHandler implements Handleable {

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

        if (text.startsWith("SUBJECT_PAGE_")){
            int page = Integer.parseInt(text.replace("SUBJECT_PAGE_", ""));
            return editSubjectMessage(chatId, messageId, page);
        }

        switch (state){

            case FREE:
                if (text.equals(AdminCommands.ADD_SUBJECT)){
                    return createSubjectStart(chatId);
                }
                if (text.equals(AdminCommands.SHOW_SUBJECTS)){
                    return showSubject(chatId);
                }
                if (text.equals(AdminCommands.DELETE_SUBJECT)){
                    return removeSubjectStart(chatId);
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
            case LESSON_WAITING_FOR_NAME_DELETING:
                return removeSubjectFinish(chatId, text);

        }

        return new HandlerResponseDto(createMessage(chatId,"Неизвестная команда, попробуй еще раз"), state);
    }

    private HandlerResponseDto showSubject(Long chatId){

        int initPage = 0;
        PageDto dto = subjectService.showSubject(initPage);

        SendMessage sm = new SendMessage();
        sm.setParseMode("HTML");
        sm.setChatId(chatId);
        sm.setText(dto.text());
        InlineKeyboardMarkup keyboard = createInlineMarkupKeyboard(dto);
        if (keyboard != null) sm.setReplyMarkup(keyboard);
        return new HandlerResponseDto(sm, AdminState.FREE);
    }

    private HandlerResponseDto editSubjectMessage(Long tgId, Integer messageId, int page){
        PageDto dto = subjectService.showSubject(page);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setParseMode("HTML");
        editMessageText.setMessageId(messageId);
        editMessageText.setChatId(tgId);
        editMessageText.setText(dto.text());

        InlineKeyboardMarkup keyboard = createInlineMarkupKeyboard(dto);
        if (keyboard != null) editMessageText.setReplyMarkup(keyboard);
        return new HandlerResponseDto(editMessageText, AdminState.FREE);
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

    private HandlerResponseDto removeSubjectStart(Long chatId){
        String responce = "Введи название предмета для удаления:";
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.LESSON_WAITING_FOR_NAME_DELETING);
    }

    private HandlerResponseDto removeSubjectFinish(Long chatId, String text) {
        String responce = subjectService.removeSubject(text);
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.FREE);
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
            backButton.setCallbackData("SUBJECT_PAGE_" + (dto.currentPage() - 1));
            rowInLine.add(backButton);
        }

        if (dto.currentPage() < dto.totalPages() - 1){
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("-->");
            nextButton.setCallbackData("SUBJECT_PAGE_" + (dto.currentPage() + 1));
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
