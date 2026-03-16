package com.bot.TelegramBot.dto;

public record PageDto(
    String text,
    int totalPages,
    int currentPage
){}
