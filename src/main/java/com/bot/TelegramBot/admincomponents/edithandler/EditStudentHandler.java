package com.bot.TelegramBot.admincomponents.edithandler;

import com.bot.TelegramBot.admincomponents.AdminCommands;
import com.bot.TelegramBot.admincomponents.AdminState;
import com.bot.TelegramBot.admincomponents.Handleable;
import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.StudentRepository;
import com.bot.TelegramBot.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EditStudentHandler implements Handleable {

    private StudentRepository studentRepo;
    private SubjectRepository subjectRepo;
    private Map<Long, Student> editableStudents = new ConcurrentHashMap<>();

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state == AdminState.FREE && text.equals(AdminCommands.EDIT_STUDENT)) return true;
        if (state == AdminState.FREE && text.startsWith("EDIT_STUDENT_")) return true;
        return state.name().startsWith("EDIT_STUDENT");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {
        if (text.equals(AdminCommands.EDIT_STUDENT)){
            return new HandlerResponseDto(createMessage(chatId, "Введи ID студента для редактирования: "), AdminState.EDIT_STUDENT_WAITING_FOR_ID);
        }

        if (text.equals("EDIT_STUDENT_EXIT")) {
            editableStudents.remove(chatId);
            return new HandlerResponseDto(createMessage(chatId, "Ты отменил редактирование"), AdminState.FREE);
        }

        if (state == AdminState.EDIT_STUDENT_WAITING_FOR_ID) return showStudentForEdit(chatId, text);

        if (state == AdminState.EDIT_STUDENT_NAME ||
                state == AdminState.EDIT_STUDENT_SURNAME ||
                state == AdminState.EDIT_STUDENT_INVCODE ||
                state == AdminState.EDIT_STUDENT_SUBJECTS) {

            if (!editableStudents.containsKey(chatId)) {
                return new HandlerResponseDto(createMessage(chatId, "Время сессии истекло или бот был перезагружен. Начни редактирование заново: /edit_subject"), AdminState.FREE);
            }
        }

        switch (state){

            case EDIT_STUDENT_NAME:
                return editStudentName(chatId, text);
            case EDIT_STUDENT_SURNAME:
                return editStudentSurname(chatId, text);
            case EDIT_STUDENT_INVCODE:
                return editStudentInviteCode(chatId, text);
            case EDIT_STUDENT_SUBJECTS:
                return editStudentSubjects(chatId, text);
        }

        String[] elements = text.split("_");
        Long studentId = Long.parseLong(elements[3]);
        String adState = elements[0] + "_" + elements[1] + "_" + elements[2];

        switch (adState){

            case "EDIT_STUDENT_NAME":
                editableStudents.put(chatId, studentRepo.findById(studentId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новое имя студента:"), AdminState.EDIT_STUDENT_NAME);
            case "EDIT_STUDENT_SURNAME":
                editableStudents.put(chatId, studentRepo.findById(studentId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новую фамилию студента:"), AdminState.EDIT_STUDENT_SURNAME);
            case "EDIT_STUDENT_INVCODE":
                editableStudents.put(chatId, studentRepo.findById(studentId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новый инвайт-код студента"), AdminState.EDIT_STUDENT_INVCODE);
            case "EDIT_STUDENT_SUBJECTS":
                editableStudents.put(chatId, studentRepo.findById(studentId).orElseThrow());
                return new HandlerResponseDto(createMessage(chatId, "Введи новые выборочные предметы студента:"), AdminState.EDIT_STUDENT_SUBJECTS);
        }

        return new HandlerResponseDto(createMessage(chatId, "Неизвестная ошибка"), state);
    }

    private HandlerResponseDto showStudentForEdit(Long chatId, String text){

        int id;
        try {
            id = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return new HandlerResponseDto(createMessage(chatId, "Опечатка при вводе ID студента! Введи целое число!"), AdminState.EDIT_STUDENT_WAITING_FOR_ID);
        }

        Optional<Student> editableStudent = studentRepo.findById(Long.parseLong(String.valueOf(id)));

        if (editableStudent.isEmpty()){
            return new HandlerResponseDto(createMessage(chatId, "Студент с таким ID не найден, попробуй ввести снова:"), AdminState.EDIT_STUDENT_WAITING_FOR_ID);
        }

        Student s = editableStudent.get();
        editableStudents.put(chatId, s);

        String response = String.format("%s%nВыбери для редактирования", s.toEditFormat());
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(response);
        sm.setReplyMarkup(createMarkupKeyboard(id));

        return new HandlerResponseDto(sm, AdminState.FREE);
    }

    private HandlerResponseDto editStudentName(Long chatId, String text){
        editableStudents.get(chatId).setName(text);
        studentRepo.save(editableStudents.remove(chatId));
        String response = "Имя студента успешно обновлено!";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
    }

    private HandlerResponseDto editStudentSurname(Long chatId, String text){
        editableStudents.get(chatId).setSurname(text);
        studentRepo.save(editableStudents.remove(chatId));
        String response = "Фамилия студента успешно обновлена!";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
    }

    private HandlerResponseDto editStudentSubjects(Long chatId, String text){

        String[] subjects = text.split(",");
        Set<Subject> draftStSubjects = new HashSet<>();
        List<String> notFoundSubjects = new ArrayList<>();

        for (String lesson : subjects){
            Optional<Subject> sub = subjectRepo.findByLessonName(lesson.trim());
            if (sub.isPresent()){
                draftStSubjects.add(sub.get());
            }else {
                notFoundSubjects.add(lesson.trim());
            }
        }
        String response = null;
        if (notFoundSubjects.isEmpty()){
            editableStudents.get(chatId).setSubjects(draftStSubjects);
            response = "Выборочные предметы обновлены!";
            editableStudents.remove(chatId);
        } else {
            response = String.format("Не найдены следующие предметы: %s. Обновление предметов отменено", notFoundSubjects);
            return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
        }


        return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
    }

    private HandlerResponseDto editStudentInviteCode(Long chatId, String text){
        editableStudents.get(chatId).setInviteCode(text);
        studentRepo.save(editableStudents.remove(chatId));
        String response = "Инвайт-код студента успешно обновлен!";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

    private InlineKeyboardMarkup createMarkupKeyboard(int id){

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        List<InlineKeyboardButton> row5 = new ArrayList<>();

        InlineKeyboardButton editNameButton = new InlineKeyboardButton();
        InlineKeyboardButton editSurnameButton = new InlineKeyboardButton();
        InlineKeyboardButton editInviteCodeButton = new InlineKeyboardButton();
        InlineKeyboardButton editSubjectsButton = new InlineKeyboardButton();
        InlineKeyboardButton exitButton = new InlineKeyboardButton();

        editNameButton.setText("Ім'я");
        editSurnameButton.setText("Прізвище");
        editInviteCodeButton.setText("Інвайт-код");
        editSubjectsButton.setText("Предмети");
        exitButton.setText("Вихід");

        editNameButton.setCallbackData("EDIT_STUDENT_NAME_" + id);
        editSurnameButton.setCallbackData("EDIT_STUDENT_SURNAME_" + id);
        editInviteCodeButton.setCallbackData("EDIT_STUDENT_INVCODE_" + id);
        editSubjectsButton.setCallbackData("EDIT_STUDENT_SUBJECTS_" + id);
        exitButton.setCallbackData("EDIT_STUDENT_EXIT");

        row1.add(editNameButton);
        row2.add(editSurnameButton);
        row3.add(editInviteCodeButton);
        row4.add(editSubjectsButton);
        row5.add(exitButton);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);
        keyboard.setKeyboard(rows);

        return keyboard;
    }
}
