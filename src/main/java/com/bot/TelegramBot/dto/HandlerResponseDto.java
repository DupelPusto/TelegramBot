package com.bot.TelegramBot.dto;

import com.bot.TelegramBot.AdminComponents.AdminState;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;


public record HandlerResponseDto(
        BotApiMethod<?> response,
        List<SendMessage> messages,
        AdminState state
){

    public HandlerResponseDto(BotApiMethod<?> response, AdminState state) {
        this(response, null, state);
    }

    public HandlerResponseDto(List<SendMessage> messages, AdminState state) {
        this(null, messages, state);
    }
}
