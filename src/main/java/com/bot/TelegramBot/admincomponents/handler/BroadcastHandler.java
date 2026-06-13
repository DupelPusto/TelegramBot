package com.bot.TelegramBot.admincomponents.handler;

import com.bot.TelegramBot.admincomponents.AdminCommands;
import com.bot.TelegramBot.admincomponents.AdminState;
import com.bot.TelegramBot.admincomponents.Handleable;
import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
public class BroadcastHandler implements Handleable {

    private final StudentRepository studentRepo;


    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state == AdminState.BROADCAST_WAITING_FOR_MESSAGE) return true;
        return (state == AdminState.FREE && text.equals(AdminCommands.BROADCAST));
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {

        switch (state){

            case FREE:
                return startBroadcast(chatId, text);
            case BROADCAST_WAITING_FOR_MESSAGE:
                return sendBroadcast(chatId, text);

        }
        return new HandlerResponseDto(createMessage(chatId, "Неизвестная ошибка"), state);
    }


    private HandlerResponseDto startBroadcast(Long chatId, String text){

        return new HandlerResponseDto(createMessage(chatId, "Введи сообщение для рассылки:"), AdminState.BROADCAST_WAITING_FOR_MESSAGE);
    }

    private HandlerResponseDto sendBroadcast(Long chatId, String text){
        List<Student> students = studentRepo.findAllByChatIdIsNotNull();
        List<SendMessage> messagesList = new ArrayList<>();
        for (Student st : students){
            SendMessage sm = new SendMessage();
            sm.setChatId(st.getChatId());
            sm.setText("CyberBot інформує:\n" + text);
            messagesList.add(sm);
        }

        SendMessage report = new SendMessage();
        report.setChatId(chatId);
        report.setText("Розсилка успішно завершена. Отримали: " + students.size() + " студентів.");
        messagesList.add(report);

        return new HandlerResponseDto(messagesList, AdminState.FREE);
    }


    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }
}
