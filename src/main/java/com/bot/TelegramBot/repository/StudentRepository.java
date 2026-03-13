package com.bot.TelegramBot.repository;

import com.bot.TelegramBot.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByChatId(Long chatId);

    Optional<Student> findByInviteCode(String inviteCode);

}
