package com.bot.TelegramBot;

import com.bot.TelegramBot.admincomponents.handler.AdminHandler;
import com.bot.TelegramBot.usercomponents.UserHandler;
import com.bot.TelegramBot.dto.HandlerResponseDto;
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
                executeAdminDto(adminHandler.handle(update));
            }
            if (update.hasMessage() && update.getMessage().hasText()) {
                if (update.getMessage().getChatId().equals(adminId)) {


                    executeAdminDto(adminHandler.handle(update));
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

    private void executeAdminDto(HandlerResponseDto dto) throws TelegramApiException {
        if (dto == null) return;

        if (dto.messages() != null && !dto.messages().isEmpty()) {
            for (SendMessage msg : dto.messages()) {
                execute(msg);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        else if (dto.response() != null) {
            execute(dto.response());
        }
    }

}
