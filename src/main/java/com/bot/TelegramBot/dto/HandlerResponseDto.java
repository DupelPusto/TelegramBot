package com.bot.TelegramBot.dto;

import com.bot.TelegramBot.AdminComponents.AdminState;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;


public record HandlerResponseDto(
        BotApiMethod<?> response,
        AdminState state
){}
