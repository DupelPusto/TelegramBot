package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;

public interface Handleable {

    boolean canHandle(AdminState state, String text);
    HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId);

}
