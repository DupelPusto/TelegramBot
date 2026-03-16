package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AdminHandler {

    private final List<Handleable> handlers;

    Map<Long, AdminState> adminStates = new ConcurrentHashMap<>();

    public BotApiMethod<?> handle(Update update){
        Long chatId = null;
        String text;
        String safeText;
        Integer messageId;
        HandlerResponseDto dto;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }


        if (chatId == null) {
            return null;
        }

        if (update.hasMessage() && update.getMessage().hasText()){
            text = update.getMessage().getText();
            safeText = (text != null) ? text : "";
            AdminState currentState = adminStates.getOrDefault(chatId, AdminState.FREE);

            if (safeText.equals(AdminCommands.SHOW_COMMANDS)){
                return showCommands(chatId);
            }
            for (Handleable handler : handlers){
                if (handler.canHandle(currentState, safeText)){
                    dto = handler.handle(chatId, safeText, currentState, null);
                    adminStates.put(chatId, dto.state());
                    return dto.response();
                }
            }
        }

        if (update.hasCallbackQuery()){
            text = update.getCallbackQuery().getData();
            messageId = update.getCallbackQuery().getMessage().getMessageId();

            AdminState currentState = adminStates.getOrDefault(chatId, AdminState.FREE);

            for (Handleable handler : handlers){
                if (handler.canHandle(currentState, text)){
                    dto = handler.handle(chatId, text, currentState, messageId);
                    adminStates.put(chatId, dto.state());
                    return dto.response();
                }
            }
        }
        return createMessage(chatId, "Неизвестная команда админа");
    }


    private SendMessage showCommands(Long tgId){

        StringBuilder response = new StringBuilder("Привет, админ!\nДоступные команды:\n");
        response.append(AdminCommands.SHOW_STUDENTS).append(" - Показать всех студентов\n");
        response.append(AdminCommands.ADD_STUDENT).append(" - Добавить студента\n");
        response.append(AdminCommands.DELETE_STUDENT).append(" - Удалить студента\n");
        response.append(AdminCommands.SHOW_SUBJECTS).append(" - Показать список предметов\n");
        response.append(AdminCommands.ADD_SUBJECT).append(" - Добавить предмет\n");
        response.append(AdminCommands.DELETE_SUBJECT).append(" - Удалить предмет\n");
        response.append(AdminCommands.ADD_SCHITEM).append(" - Добавить элемент расписания\n");
        return createMessage(tgId, response.toString());

    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

}
