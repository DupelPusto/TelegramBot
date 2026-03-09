package com.bot.TelegramBot;

import com.bot.TelegramBot.service.ScheduleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class Bot extends TelegramLongPollingBot {

    private static final String START = "/start";
    private static final String HELP = "/help";
    private static final String LINK = "/link";
    private static final String SCHEDULE = "/schedule";

    private final ScheduleService scheduleService;

    @Value("${bot.name}")
    private String botName;

    @Value("${admin.id}")
    private Long adminId;

    public Bot(@Value("${bot.token}") String botToken, ScheduleService scheduleService){
        super(botToken);
        this.scheduleService = scheduleService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {

                Long chatId = update.getMessage().getChatId();
                switch (update.getMessage().getText()) {
                    case START:
                        execute(sendMessage(chatId, startMessage(update.getMessage().getChat().getFirstName())));
                        break;
                    case HELP:
                        execute(sendMessage(chatId, getHelp()));
                        break;
                    case SCHEDULE:
                        String response = scheduleService.getScheduleForToday(chatId);
                        execute(sendMessage(chatId, response));
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


    private static String startMessage(String firstName){
        return String.format("Привет, %s! Выбери что хочешь сделать:%n/link - ссылка на текущую пару%n/schedule - посмотреть расписание на сегодня%nНапиши /help на случай если забудешь команды",firstName);
    }

    private static String getHelp(){
        return String.format("Выбери что хочешь сделать:%n/link - ссылка на текущую пару%n/schedule - расписание на сегодня");
    }

    private static SendMessage sendMessage(long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }
}
