package io.github.werlickgit;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    static void main() {
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            String token = System.getenv("BOT_TOKEN");
            botsApplication.registerBot(token, new NoteBot(token));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
