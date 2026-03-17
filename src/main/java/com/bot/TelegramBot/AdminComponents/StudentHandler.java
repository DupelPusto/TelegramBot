package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
import com.bot.TelegramBot.dto.PageDto;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import com.bot.TelegramBot.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class StudentHandler implements Handleable{

    private final SubjectRepository subjectRepo;
    private final StudentService studentService;
    private Map<Long, Student> draftStudents = new ConcurrentHashMap<>();

    @Override
    public boolean canHandle(AdminState state, String text) {
        if (state.name().startsWith("STUDENT")) return true;
        if (state == AdminState.FREE && text.startsWith("STUDENT_PAGE_")) return true;
        return state == AdminState.FREE && text.startsWith("/student");
    }

    @Override
    public HandlerResponseDto handle(Long chatId, String text, AdminState state, Integer messageId) {

        if (text.startsWith("STUDENT_PAGE_")){
                int page = Integer.parseInt(text.replace("STUDENT_PAGE_", ""));
                return editStudentsMessage(chatId, messageId, page);
            }
        switch (state) {
            case FREE:
                if (text.equals(AdminCommands.ADD_STUDENT)) {
                    return createStudentStart(chatId);
                }
                if (text.equals(AdminCommands.SHOW_STUDENTS)){
                    return showStudents(chatId);
                }
                break;
            case STUDENT_WAITING_FOR_NAME:
                return createStudentName(chatId, text);
            case STUDENT_WAITING_FOR_SURNAME:
                return createStudentSurname(chatId, text);
            case STUDENT_WAITING_FOR_INVITE_CODE:
                return createStudentInviteCode(chatId, text);
            case STUDENT_WAITING_FOR_SUBJECTS:
                return createStudentFinish(chatId, text);

        }
        return new HandlerResponseDto(createMessage(chatId, "Неизвестная ошибка, попробуй снова"), state);
    }


    private HandlerResponseDto showStudents(Long chatId){

        int initPage = 0;
        PageDto dto = studentService.showStudent(initPage);

        SendMessage sm = new SendMessage();
        sm.setParseMode("HTML");
        sm.setChatId(chatId);
        sm.setText(dto.text());
        InlineKeyboardMarkup keyboard = createInlineMarkupKeyboard(dto);
        if (keyboard != null) sm.setReplyMarkup(keyboard);
        return new HandlerResponseDto(sm, AdminState.FREE);
    }

    private HandlerResponseDto editStudentsMessage(Long tgId, Integer messageId, int page){
        PageDto dto = studentService.showStudent(page);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setParseMode("HTML");
        editMessageText.setMessageId(messageId);
        editMessageText.setChatId(tgId);
        editMessageText.setText(dto.text());

        InlineKeyboardMarkup keyboard = createInlineMarkupKeyboard(dto);
        if (keyboard != null) editMessageText.setReplyMarkup(keyboard);
        return new HandlerResponseDto(editMessageText, AdminState.FREE);
    }

    private HandlerResponseDto createStudentStart(Long chatId){

        Student student = new Student();
        draftStudents.put(chatId, student);
        String response = "Введи Имя студента";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.STUDENT_WAITING_FOR_NAME);
    }

    private HandlerResponseDto createStudentName(Long chatId, String text){

        draftStudents.get(chatId).setName(text);
        String response = "Введи Фамилию студента";
        return new HandlerResponseDto(createMessage(chatId, response), AdminState.STUDENT_WAITING_FOR_SURNAME);
    }

    private HandlerResponseDto createStudentSurname(Long chatId, String text){

        draftStudents.get(chatId).setSurname(text);
        String responce = String.format("Введи инвайт-код для %s %s:", draftStudents.get(chatId).getName(), draftStudents.get(chatId).getSurname());
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.STUDENT_WAITING_FOR_INVITE_CODE);
    }

    private HandlerResponseDto createStudentInviteCode(Long chatId, String text){

        draftStudents.get(chatId).setInviteCode(text);
        String responce = String.format("Введи выборочные предметы для %s %s:", draftStudents.get(chatId).getName(), draftStudents.get(chatId).getSurname());
        return new HandlerResponseDto(createMessage(chatId, responce), AdminState.STUDENT_WAITING_FOR_SUBJECTS);
    }

    private HandlerResponseDto createStudentFinish(Long chatId, String text){

        String[] subjects = text.split(",");
        Set<Subject> drafrStSubjects = new HashSet<>();
        List<String> notFoundSubjects = new ArrayList<>();
        for (String lesson : subjects){
            Optional<Subject> sub = subjectRepo.findByLessonName(lesson.trim());
            if (sub.isPresent()){
                drafrStSubjects.add(sub.get());
            }else {
                notFoundSubjects.add(lesson.trim());
            }


        }
        String response = null;
        if (notFoundSubjects.isEmpty()){
            draftStudents.get(chatId).setSubjects(drafrStSubjects);
            response = studentService.addStudent(draftStudents.get(chatId));
            draftStudents.remove(chatId);
        } else {
            response = String.format("Не найдены следующие предметы: %s. Попробуй ввести снова:", notFoundSubjects);
            return new HandlerResponseDto(createMessage(chatId, response), AdminState.STUDENT_WAITING_FOR_SUBJECTS);
        }


        return new HandlerResponseDto(createMessage(chatId, response), AdminState.FREE);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

    private InlineKeyboardMarkup createInlineMarkupKeyboard(PageDto dto){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        if (dto.currentPage() > 0){
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("<--");
            backButton.setCallbackData("STUDENT_PAGE_" + (dto.currentPage() - 1));
            rowInLine.add(backButton);
        }

        if (dto.currentPage() < dto.totalPages() - 1){
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("-->");
            nextButton.setCallbackData("STUDENT_PAGE_" + (dto.currentPage() + 1));
            rowInLine.add(nextButton);
        }

        if (!rowInLine.isEmpty()){
            rowsInLine.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            return inlineKeyboardMarkup;
        }

        return null;
    }
}
