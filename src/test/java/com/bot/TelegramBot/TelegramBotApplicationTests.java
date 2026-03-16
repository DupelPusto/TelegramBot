package com.bot.TelegramBot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "BOT_TOKEN=test_token",
        "DB_PASSWORD=QWER2404ty",
        "spring.autoconfigure.exclude=org.telegram.telegrambots.starter.TelegramBotStarterConfiguration"
})
class TelegramBotApplicationTests {

	@Test
	void contextLoads() {
	}

}
