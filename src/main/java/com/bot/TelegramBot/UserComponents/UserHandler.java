package com.bot.TelegramBot.UserComponents;

import com.bot.TelegramBot.repository.StudentRepository;
import com.bot.TelegramBot.service.ScheduleService;
import com.bot.TelegramBot.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.bot.TelegramBot.UserComponents.UserCommands.*;

@Component
@RequiredArgsConstructor
public class UserHandler {

    private final ScheduleService scheduleService;
    private final StudentService studentService;

    Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    public SendMessage userHandler(Update update){

        Long chatId = update.getMessage().getChatId();
        String userText = update.getMessage().getText();
        UserState userState = userStates.getOrDefault(chatId, UserState.FREE);

        if (userState == UserState.WAITING_FOR_INVITE_CODE) {
            return saveInviteCode(update);
        }

        switch (userText){

            case START:
                return createMessage(chatId, startMessage(update.getMessage().getChat().getFirstName()));
            case LOGIN:
                return registerStudent(update);
            case HELP:
                return createMessage(chatId, getHelp());
            case SCHEDULE:
                return createMessage(chatId, scheduleService.getScheduleForToday(chatId));
            case SCHEDULE_FOR_WEEK:
                return createMessage(chatId, scheduleService.getScheduleForWeek(chatId));
            case LINK:
                return getLessonLink(chatId);

        }

        if (chatId == null) return null;

        String response = "Неизвестная команда";
        return createMessage(chatId, response);
    }

    private SendMessage getLessonLink(Long chatId){
        String link = scheduleService.getLink(chatId);
        return createMessage(chatId, link);
    }


    private SendMessage registerStudent(Update update){
        Long chatId = update.getMessage().getChatId();
        if (studentService.isRegistered(chatId)) return createMessage(chatId, "Ты уже зарегистрирован!");
        userStates.put(chatId, UserState.WAITING_FOR_INVITE_CODE);
        String response = "Введи свой инвайт-код";
        return createMessage(chatId, response);
    }

    private SendMessage saveInviteCode(Update update){

        Long chatId = update.getMessage().getChatId();
        String code = update.getMessage().getText().trim();
        String response = studentService.registerUser(chatId, code);
        userStates.remove(chatId);
        return createMessage(chatId, response);
    }

    private String startMessage(String firstName){
        return String.format("Привет, %s!%nЧтобы начать пользоваться ботом введи свой инвайт-код /login",firstName);
    }

    private String getHelp(){
        return String.format("/link - Ссылка на текущую пару%n/schedule - Расписание на сегодня%n/schedule_week - Расписание на неделю");
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setParseMode("HTML");
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }
}
