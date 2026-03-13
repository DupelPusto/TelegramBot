package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.StudentPageDto;
import com.bot.TelegramBot.entities.ScheduleItem;
import com.bot.TelegramBot.entities.Student;
import com.bot.TelegramBot.entities.Subject;
import com.bot.TelegramBot.repository.SubjectRepository;
import com.bot.TelegramBot.service.ScheduleService;
import com.bot.TelegramBot.service.StudentService;
import com.bot.TelegramBot.service.SubjectService;
import com.bot.TelegramBot.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AdminHandler {

    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepo;
    private final ScheduleService scheduleService;
    Map<Long, AdminState> adminStates = new ConcurrentHashMap<>();
    Map<Long, Student> draftStudents = new ConcurrentHashMap<>();
    Map<Long, Subject> draftSubjects = new ConcurrentHashMap<>();
    Map<Long, ScheduleItem> draftSchItems = new ConcurrentHashMap<>();


    public BotApiMethod<?> adminHandler(Update update){
        Long tgId = null;
        String text = null;
        Integer messageId;

        if (update.hasMessage() && update.getMessage().hasText()) {
            tgId = update.getMessage().getChatId();
            text = update.getMessage().getText();
        }

        else if (update.hasCallbackQuery()) {
            tgId = update.getCallbackQuery().getMessage().getChatId();
            text = update.getCallbackQuery().getData();
            messageId = update.getCallbackQuery().getMessage().getMessageId();


            if (text.startsWith("PAGE_")) {
                int page = Integer.parseInt(text.replace("PAGE_", ""));
                return editStudentsMessage(tgId, messageId, page);
            }
        }

        if (tgId == null){
            return null;
        }

        AdminState currentState = adminStates.getOrDefault(tgId, AdminState.FREE);

        switch (currentState){
            case FREE:
                if (text.equals(AdminCommands.ADD_STUDENT)){
                    return createStudentStart(update);
                }
                if (text.equals(AdminCommands.ADD_SUBJECT)){
                    return createSubjectStart(update);
                }
                if (text.equals(AdminCommands.ADD_SCHITEM)){
                    return createSchItemStart(tgId);
                }
                if (text.equals(AdminCommands.SHOW_STUDENTS)){
                    return showStudents(tgId);
                }
                if (text.equals(AdminCommands.SHOW_COMMANDS)){
                    return showCommands(tgId);
                }
                break;
            case WAITING_FOR_STUDENT_NAME:
                return createStudentName(update);
            case WAITING_FOR_STUDENT_SURNAME:
                return createStudentSurname(update);
            case WAITING_FOR_STUDENT_INVITE_CODE:
                return createStudentInviteCode(update);
            case WAITING_FOR_STUDENT_SUBJECTS:
                return createStudentFinish(update);
            case WAITING_FOR_LESSON_NAME:
                return createSubjectName(update);
            case WAITING_FOR_LESSON_LINK:
                return createSubjectLink(update);
            case WAITING_FOR_LESSON_TEACHER:
                return createSubjectTeacher(update);
            case WAITING_FOR_LESSON_SELECTIVE:
                return createSubjectFinish(update);
            case WAITING_FOR_SCHITEM_LESSON_NAME:
                return createSchItemName(update);
            case WAITING_FOR_SCHITEM_LESSON_NUMBER:
                return createSchItemNumber(update);
            case WAITING_FOR_SCHITEM_DAY:
                return createSchItemDay(update);
        }

        String responce = "Неизвестная команда админа";
        return createMessage(tgId, responce);
    }

    private SendMessage showCommands(Long tgId){

        StringBuilder response = new StringBuilder("Привет, админ!\nДоступные команды:\n");
        response.append(AdminCommands.SHOW_STUDENTS).append(" - Показать всех студентов");
        response.append(AdminCommands.ADD_STUDENT).append(" - Добавить студента");
        response.append(AdminCommands.DELETE_STUDENT).append(" - Удалить студента");
        response.append(AdminCommands.SHOW_SUBJECTS).append(" - Показать список предметов");
        response.append(AdminCommands.ADD_SUBJECT).append(" - Добавить предмет");
        response.append(AdminCommands.DELETE_SUBJECT).append(" - Удалить предмет");
        response.append(AdminCommands.ADD_SCHITEM).append(" - Добавить элемент расписания");
        return createMessage(tgId, response.toString());

    }

    private SendMessage showStudents(Long tgId){

        int initPage = 0;
        StudentPageDto dto = studentService.showStudent(initPage);

        SendMessage sm = new SendMessage();
        sm.setChatId(tgId);
        sm.setText(dto.text());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        if (dto.currentPage() > 0){
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("<--");
            backButton.setCallbackData("PAGE_" + (dto.currentPage() - 1));
            rowInLine.add(backButton);
        }

        if (dto.currentPage() < dto.totalPages() - 1){
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("-->");
            nextButton.setCallbackData("PAGE_" + (dto.currentPage() + 1));
            rowInLine.add(nextButton);
        }

        if (!rowInLine.isEmpty()){
            rowsInLine.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            sm.setReplyMarkup(inlineKeyboardMarkup);
        }

        return sm;
    }

    private EditMessageText editStudentsMessage(Long tgId, Integer messageId, int page){
        StudentPageDto dto = studentService.showStudent(page);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setMessageId(messageId);
        editMessageText.setChatId(tgId);
        editMessageText.setText(dto.text());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        if (dto.currentPage() > 0){
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("<--");
            backButton.setCallbackData("PAGE_" + (dto.currentPage() - 1));
            rowInLine.add(backButton);
        }

        if (dto.currentPage() < dto.totalPages() - 1){
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("-->");
            nextButton.setCallbackData("PAGE_" + (dto.currentPage() + 1));
            rowInLine.add(nextButton);
        }

        if (!rowInLine.isEmpty()){
            rowsInLine.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        }

        return editMessageText;
    }

    private SendMessage createSchItemStart(Long tgId){

        ScheduleItem scheduleItem = new ScheduleItem();
        draftSchItems.put(tgId, scheduleItem);
        adminStates.put(tgId,AdminState.WAITING_FOR_SCHITEM_LESSON_NAME);
        String response = "Введи название предмета: ";
        return createMessage(tgId, response);
    }

    private SendMessage createSchItemName(Update update){

        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        Optional<Subject> optionalSubject = subjectRepo.findByLessonName(text);

        if (optionalSubject.isPresent()){
            Subject subject = optionalSubject.get();
            draftSchItems.get(tgId).setSubject(subject);
        } else {
            return createMessage(tgId, "Предмет не найден, попробуй еще раз:");
        }
        adminStates.put(tgId, AdminState.WAITING_FOR_SCHITEM_LESSON_NUMBER);
        String response = "Введи номер пары: ";
        return createMessage(tgId, response);
    }

    private SendMessage createSchItemNumber(Update update){

        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        Integer lessonNumber;
        try {
            lessonNumber = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            String response = "Неверный формат номера, попробуй снова:";
            return createMessage(tgId, response);
        }

        draftSchItems.get(tgId).setLessonNumber(lessonNumber);
        adminStates.put(tgId, AdminState.WAITING_FOR_SCHITEM_DAY);

        String response = "Введи день недели:";
        return createMessage(tgId, response);
    }

    private SendMessage createSchItemDay(Update update){

        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        DayOfWeek day = DateUtil.parseDayOfWeek(text);
        if (day == null) {
            return createMessage(tgId, "Неверный формат дня. Попробуй снова в формате 'Пн' или 'пн':");
        }
        draftSchItems.get(tgId).setDayOfWeek(day);
        String response = scheduleService.addScheduleItem(draftSchItems.get(tgId));
        adminStates.put(tgId, AdminState.FREE);
        draftSchItems.remove(tgId);
        return createMessage(tgId, response);
    }

    private SendMessage createSubjectStart(Update update){

        Long tgId = update.getMessage().getChatId();
        Subject subject = new Subject();
        draftSubjects.put(tgId, subject);
        adminStates.put(tgId, AdminState.WAITING_FOR_LESSON_NAME);
        String responce = "Введи название предмета:";
        return createMessage(tgId, responce);
    }

    private SendMessage createSubjectName(Update update){

        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        draftSubjects.get(tgId).setLessonName(text);
        adminStates.put(tgId, AdminState.WAITING_FOR_LESSON_LINK);
        String responce = "Введи ссылку на предмет:";
        return createMessage(tgId,responce);
    }

    private SendMessage createSubjectLink(Update update){

        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        draftSubjects.get(tgId).setZoomLink(text);
        adminStates.put(tgId, AdminState.WAITING_FOR_LESSON_TEACHER);
        String responce = "Введи имя преподователя:";
        return createMessage(tgId, responce);
    }

    private SendMessage createSubjectTeacher(Update update){
        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        draftSubjects.get(tgId).setTeacher(text);
        adminStates.put(tgId, AdminState.WAITING_FOR_LESSON_SELECTIVE);
        String responce = "Это выборочный предмет?(+,-)";
        return createMessage(tgId, responce);
    }

    private SendMessage createSubjectFinish(Update update){

        Long tgId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        if (text.equals("+")){
            draftSubjects.get(tgId).setSelectiveSub(true);
        } else if (text.equals("-")){
            draftSubjects.get(tgId).setSelectiveSub(false);
        }else {
            return createMessage(tgId, "Неверный параметр выборочного предмета. Повторите попытку");
        }
        adminStates.put(tgId, AdminState.FREE);
        String responce = subjectService.addSubject(draftSubjects.get(tgId));
        draftSubjects.remove(tgId);
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentStart(Update update){

        Long tgId = update.getMessage().getChatId();
        Student student = new Student();
        draftStudents.put(tgId, student);
        adminStates.put(tgId, AdminState.WAITING_FOR_STUDENT_NAME);
        String response = "Введи Имя студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentName(Update update){

        Long tgId = update.getMessage().getChatId();
        String studentName = update.getMessage().getText();
        draftStudents.get(tgId).setName(studentName);
        adminStates.put(tgId, AdminState.WAITING_FOR_STUDENT_SURNAME);
        String response = "Введи Фамилию студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentSurname(Update update){

        Long tgId = update.getMessage().getChatId();
        String studentSurname = update.getMessage().getText();
        draftStudents.get(tgId).setSurname(studentSurname);
        adminStates.put(tgId, AdminState.WAITING_FOR_STUDENT_INVITE_CODE);
        String responce = String.format("Введи инвайт-код для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentInviteCode(Update update){

        Long tgId = update.getMessage().getChatId();
        String studentInviteCode = update.getMessage().getText();
        draftStudents.get(tgId).setInviteCode(studentInviteCode);
        adminStates.put(tgId, AdminState.WAITING_FOR_STUDENT_SUBJECTS);
        String responce = String.format("Введи выборочные предметы для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentFinish(Update update){
        Long tgId = update.getMessage().getChatId();
        String studentSubjects = update.getMessage().getText();
        String[] subjects = studentSubjects.split(",");
        Set<Subject> drafrStSubjects = new HashSet<>();
        for (String lesson : subjects){
            Optional<Subject> sub = subjectRepo.findByLessonName(lesson.trim());
            if (sub.isPresent()){
                drafrStSubjects.add(sub.get());
            }
        }
        draftStudents.get(tgId).setSubjects(drafrStSubjects);
        adminStates.put(tgId, AdminState.FREE);
        String responce = studentService.addStudent(draftStudents.get(tgId));
        draftStudents.remove(tgId);

        return createMessage(tgId, responce);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

}
