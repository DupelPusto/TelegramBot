package com.bot.TelegramBot;

import com.bot.TelegramBot.AdminComponents.AdminHandler;
import com.bot.TelegramBot.UserComponents.UserHandler;
import com.bot.TelegramBot.service.ScheduleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class Bot extends TelegramLongPollingBot {
    private final AdminHandler adminHandler;
    private final UserHandler userHandler;

    @Value("${bot.name}")
    private String botName;

    @Value("${admin.id}")
    private Long adminId;

    public Bot(@Value("${bot.token}") String botToken, AdminHandler adminHandler, UserHandler userHandler){
        super(botToken);
        this.adminHandler = adminHandler;
        this.userHandler = userHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {

            if (update.hasCallbackQuery()){
                execute(adminHandler.adminHandler(update));
            }
            if (update.hasMessage() && update.getMessage().hasText()) {
                if (update.getMessage().getChatId().equals(adminId)) {
                    execute(adminHandler.adminHandler(update));
                } else {
                    execute(userHandler.userHandler(update));
                }
            }

        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram API exception");
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

}
