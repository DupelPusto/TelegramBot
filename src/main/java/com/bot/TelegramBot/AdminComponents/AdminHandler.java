package com.bot.TelegramBot.AdminComponents;

import com.bot.TelegramBot.dto.HandlerResponseDto;
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

    private final List<Handleable> handlers;

    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepo;
    private final ScheduleService scheduleService;
    Map<Long, AdminState> adminStates = new ConcurrentHashMap<>();
    Map<Long, Student> draftStudents = new ConcurrentHashMap<>();
    Map<Long, Subject> draftSubjects = new ConcurrentHashMap<>();
    Map<Long, ScheduleItem> draftSchItems = new ConcurrentHashMap<>();

    public BotApiMethod<?> handle(Update update){
        Long chatId = null;
        String text = null;
        String safeText;
        Integer messageId;
        HandlerResponseDto dto = null;

        if (update.hasMessage() && update.getMessage().hasText()){
            chatId = update.getMessage().getChatId();
            text = update.getMessage().getText();
            safeText = (text != null) ? text : "";
            adminStates.getOrDefault(chatId, AdminState.FREE);

            for (Handleable handler : handlers){
                if (handler.canHandle(adminStates.get(chatId), safeText)){
                    dto = handler.handle(chatId, safeText, adminStates.get(chatId));
                    adminStates.put(chatId, dto.state());
                    return dto.response();
                }
            }
        }
        return createMessage(chatId, "Неизвестная команда админа");
    }

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
                    return createStudentStart(tgId);
                }
                if (text.equals(AdminCommands.ADD_SUBJECT)){
                    return createSubjectStart(tgId);
                }
//                if (text.equals(AdminCommands.ADD_SCHITEM)){
//                    return createSchItemStart(tgId);
//                }
                if (text.equals(AdminCommands.SHOW_STUDENTS)){
                    return showStudents(tgId);
                }
                if (text.equals(AdminCommands.SHOW_COMMANDS)){
                    return showCommands(tgId);
                }
                break;
            case STUDENT_WAITING_FOR_NAME:
                return createStudentName(tgId, text);
            case STUDENT_WAITING_FOR_SURNAME:
                return createStudentSurname(tgId,text);
            case STUDENT_WAITING_FOR_INVITE_CODE:
                return createStudentInviteCode(tgId, text);
            case STUDENT_WAITING_FOR_SUBJECTS:
                return createStudentFinish(tgId, text);
            case LESSON_WAITING_FOR_NAME:
                return createSubjectName(tgId, text);
            case LESSON_WAITING_FOR_LINK:
                return createSubjectLink(tgId, text);
            case LESSON_WAITING_FOR_TEACHER:
                return createSubjectTeacher(tgId, text);
            case LESSON_WAITING_FOR_SELECTIVE:
                return createSubjectFinish(tgId, text);
//            case SCHITEM_WAITING_FOR_LESSON_NAME:
//                return createSchItemName(tgId, text);
//            case SCHITEM_WAITING_FOR_LESSON_NUMBER:
//                return createSchItemNumber(tgId, text);
//            case SCHITEM_WAITING_FOR_LESSON_AUDITORY:
//                return createSchItemAuditory(tgId, text);
//            case SCHITEM_WAITING_FOR_DAY:
//                return createSchItemDay(tgId, text);
        }

        String responce = "Неизвестная команда админа";
        return createMessage(tgId, responce);
    }

    private SendMessage showCommands(Long tgId){

        StringBuilder response = new StringBuilder("Привет, админ!\nДоступные команды:\n");
        response.append(AdminCommands.SHOW_STUDENTS).append(" - Показать всех студентов\n");
        response.append(AdminCommands.ADD_STUDENT).append(" - Добавить студента\n");
        response.append(AdminCommands.DELETE_STUDENT).append(" - Удалить студента\n");
        response.append(AdminCommands.SHOW_SUBJECTS).append(" - Показать список предметов\n");
        response.append(AdminCommands.ADD_SUBJECT).append(" - Добавить предмет\n");
        response.append(AdminCommands.DELETE_SUBJECT).append(" - Удалить предмет\n");
        response.append(AdminCommands.ADD_SCHITEM).append(" - Добавить элемент расписания\n");
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

//    private SendMessage createSchItemStart(Long tgId){
//
//        ScheduleItem scheduleItem = new ScheduleItem();
//        draftSchItems.put(tgId, scheduleItem);
//        adminStates.put(tgId,AdminState.SCHITEM_WAITING_FOR_LESSON_NAME);
//        String response = "Введи название предмета: ";
//        return createMessage(tgId, response);
//    }
//
//    private SendMessage createSchItemName(Long tgId, String text){
//
//        Optional<Subject> optionalSubject = subjectRepo.findByLessonName(text);
//
//        if (optionalSubject.isPresent()){
//            Subject subject = optionalSubject.get();
//            draftSchItems.get(tgId).setSubject(subject);
//        } else {
//            return createMessage(tgId, "Предмет не найден, попробуй еще раз:");
//        }
//        adminStates.put(tgId, AdminState.SCHITEM_WAITING_FOR_LESSON_NUMBER);
//        String response = "Введи номер пары: ";
//        return createMessage(tgId, response);
//    }
//
//    private SendMessage createSchItemNumber(Long tgId, String text){
//
//        Integer lessonNumber;
//        try {
//            lessonNumber = Integer.parseInt(text);
//        } catch (NumberFormatException e) {
//            String response = "Неверный формат номера, попробуй снова:";
//            return createMessage(tgId, response);
//        }
//
//        draftSchItems.get(tgId).setLessonNumber(lessonNumber);
//        adminStates.put(tgId, AdminState.SCHITEM_WAITING_FOR_LESSON_AUDITORY);
//
//
//        String response = "Введи номер аудитории: ";
//
//        return createMessage(tgId, response);
//    }
//
//    private SendMessage createSchItemAuditory(Long tgId, String text){
//
//        draftSchItems.get(tgId).setAuditory(text);
//        adminStates.put(tgId, AdminState.SCHITEM_WAITING_FOR_DAY);
//        String response = "Введи день недели:";
//        return createMessage(tgId, response);
//    }
//
//    private SendMessage createSchItemDay(Long tgId, String text){
//
//        DayOfWeek day = DateUtil.parseDayOfWeek(text);
//        if (day == null) {
//            return createMessage(tgId, "Неверный формат дня. Попробуй снова в формате 'Пн' или 'пн':");
//        }
//        draftSchItems.get(tgId).setDayOfWeek(day);
//        String response = scheduleService.addScheduleItem(draftSchItems.get(tgId));
//        adminStates.put(tgId, AdminState.FREE);
//        draftSchItems.remove(tgId);
//        return createMessage(tgId, response);
//    }

    private SendMessage createSubjectStart(Long tgId){

        Subject subject = new Subject();
        draftSubjects.put(tgId, subject);
        adminStates.put(tgId, AdminState.LESSON_WAITING_FOR_NAME);
        String responce = "Введи название предмета:";
        return createMessage(tgId, responce);
    }

    private SendMessage createSubjectName(Long tgId, String text){

        draftSubjects.get(tgId).setLessonName(text);
        adminStates.put(tgId, AdminState.LESSON_WAITING_FOR_LINK);
        String responce = "Введи ссылку на предмет:";
        return createMessage(tgId,responce);
    }

    private SendMessage createSubjectLink(Long tgId, String text){

        draftSubjects.get(tgId).setZoomLink(text);
        adminStates.put(tgId, AdminState.LESSON_WAITING_FOR_TEACHER);
        String responce = "Введи имя преподователя:";
        return createMessage(tgId, responce);
    }

    private SendMessage createSubjectTeacher(Long tgId, String text){

        draftSubjects.get(tgId).setTeacher(text);
        adminStates.put(tgId, AdminState.LESSON_WAITING_FOR_SELECTIVE);
        String responce = "Это выборочный предмет?(+,-):";
        return createMessage(tgId, responce);
    }

    private SendMessage createSubjectFinish(Long tgId, String text){

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

    private SendMessage createStudentStart(Long tgId){

        Student student = new Student();
        draftStudents.put(tgId, student);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_NAME);
        String response = "Введи Имя студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentName(Long tgId, String text){

        draftStudents.get(tgId).setName(text);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_SURNAME);
        String response = "Введи Фамилию студента";
        return createMessage(tgId, response);
    }

    private SendMessage createStudentSurname(Long tgId, String text){

        draftStudents.get(tgId).setSurname(text);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_INVITE_CODE);
        String responce = String.format("Введи инвайт-код для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentInviteCode(Long tgId, String text){

        draftStudents.get(tgId).setInviteCode(text);
        adminStates.put(tgId, AdminState.STUDENT_WAITING_FOR_SUBJECTS);
        String responce = String.format("Введи выборочные предметы для %s %s:", draftStudents.get(tgId).getName(), draftStudents.get(tgId).getSurname());
        return createMessage(tgId, responce);
    }

    private SendMessage createStudentFinish(Long tgId, String text){

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
            draftStudents.get(tgId).setSubjects(drafrStSubjects);
            adminStates.put(tgId, AdminState.FREE);
            response = studentService.addStudent(draftStudents.get(tgId));
            draftStudents.remove(tgId);
        } else {
            response = String.format("Не найдены следующие предметы: %s. Попробуй ввести снова:", notFoundSubjects);
        }


        return createMessage(tgId, response);
    }

    private SendMessage createMessage(Long id, String text){
        SendMessage sm = new SendMessage();
        sm.setChatId(id);
        sm.setText(text);
        return sm;
    }

}
