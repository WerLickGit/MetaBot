package io.github.werlickgit;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    Map<Long, DialogueStatus> states = new HashMap<>();
    Map<Long, NoteDraft> drafts = new HashMap<>();
    Map<Long, List<NoteDraft>> savedDrafts = new HashMap<>();

    public NoteBot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }


    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            User user = update.getMessage().getFrom();
            long chat_id = update.getMessage().getChatId();
            String firstName = user.getFirstName();
            DialogueStatus currentStatus = states.get(chat_id);

            InlineKeyboardButton keyboardButton = InlineKeyboardButton.builder()
                    .text("🏷️ Создать заметку")
                    .callbackData("create_note")
                    .build();

            InlineKeyboardButton keyboardButton1 = InlineKeyboardButton.builder()
                    .text("📕 Мои заметки")
                    .callbackData("notes_list")
                    .build();

            InlineKeyboardButton keyboardButton2 = InlineKeyboardButton.builder()
                    .text("🔍 Подробнее...")
                    .callbackData("faq")
                    .build();

            InlineKeyboardButton keyboardButton3 = InlineKeyboardButton.builder()
                    .text("❌ Назад")
                    .callbackData("back")
                    .build();

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            new InlineKeyboardRow(keyboardButton, keyboardButton1),
                            new InlineKeyboardRow(keyboardButton2)
                    ))
                    .build();

            InlineKeyboardMarkup keyboard2 = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            new InlineKeyboardRow(keyboardButton3)
                    ))
                    .build();

            if (currentStatus == DialogueStatus.AWAITING_TITLE) {
                drafts.get(chat_id).setTitle(message_text);
                states.put(chat_id, DialogueStatus.AWAITING_TAG);
                currentStatus = DialogueStatus.AWAITING_TAG;
                SendMessage sendMessage = SendMessage
                        .builder()
                        .chatId(chat_id)
                        .parseMode("HTML")
                        .text("""
                                <b>🔖 Процесс создания заметки (2/3)</b>
                                
                                <i>Тег обязательно должен начинаться с символа </i> <code>#</code>
                                <i>Например:</i> <code>#учёба</code>, <code>#идеи</code>, <code>#важное</code>
                                """)
                        .replyMarkup(keyboard2)
                        .build();
                try {
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (currentStatus == DialogueStatus.AWAITING_TAG) {
                if (message_text.startsWith("#")) {
                    drafts.get(chat_id).setTag(message_text);
                    states.put(chat_id, DialogueStatus.AWAITING_TEXT);
                    currentStatus = DialogueStatus.NONE;
                    SendMessage sendMessage = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .parseMode("HTML")
                            .text("""
                                <b>🔖 Процесс создания заметки (3/3)</b>
                                
                                <i>Введите текст для заметки</i>
                                """)
                            .replyMarkup(keyboard2)
                            .build();
                    try {
                        telegramClient.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else {
                    SendMessage sendMessage = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .parseMode("HTML")
                            .text("""
                                    <i>❗ Вы не указали</i> <code>#</code> <i> в начале, попробуйте указать корректный тэг для вашей заметки</i>
                                    """)
                            .replyMarkup(keyboard2)
                            .build();
                    try {
                        telegramClient.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (currentStatus == DialogueStatus.AWAITING_TEXT) {
                drafts.get(chat_id).setText(message_text);
                String title = drafts.get(chat_id).getTitle();
                String tag = drafts.get(chat_id).getTag();
                String text = drafts.get(chat_id).getText();
                SendMessage sendMessage = SendMessage
                        .builder()
                        .chatId(chat_id)
                        .parseMode("HTML")
                        .text("""
                                <b>✅ Заметка успешно создана</b>
                                
                                <i>🏷️ Тег:</i> <code>%s</code>
                                <i>📝 Название: %s</i>
                                
                                <b>Содержание заметки:</b>
                                
                                <blockquote>%s</blockquote> 
                                """.formatted(tag, title, text))
                        .replyMarkup(keyboard)
                        .build();
                states.put(chat_id, DialogueStatus.NONE);
                List<NoteDraft> userNotes = savedDrafts.computeIfAbsent(chat_id, k -> new ArrayList<>());
                userNotes.add(drafts.get(chat_id));
                try {
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals("/start")) {
                SendMessage message = SendMessage
                        .builder()
                        .chatId(chat_id)
                        .parseMode("HTML")
                        .text("""
                                <b>Приветствую, %s!</b>
                                
                                <i>Я - Meta, твой небольшой помощник для работы с заметками.</i>
                                
                                <blockquote>📝 Ты можешь отправлять мне свои мысли, задачи, идеи или любую другую информацию, которую хочешь сохранить. Я помогу организовать их с помощью уникальных тегов #, чтобы к заметкам было проще возвращаться и находить нужное.</blockquote>
                                
                                <i>📚 Все сохранённые заметки можно будет просмотреть в любой момент с помощью специальной команды.</i>
                                """.formatted(firstName))
                        .replyMarkup(keyboard)
                        .build();
                try {
                    telegramClient.execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals("/note")) {
                drafts.put(chat_id, new NoteDraft());
                states.put(chat_id, DialogueStatus.AWAITING_TITLE);

                SendMessage note = SendMessage
                        .builder()
                        .chatId(chat_id)
                        .parseMode("HTML")
                        .text("""
                                <b>🔖 Процесс создания заметки (1/3)</b>
                                
                                <i>Укажите название для заметки</i>
                                """)
                        .replyMarkup(keyboard2)
                        .build();

                try {
                    telegramClient.execute(note);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            } else if (message_text.equals("/notes")) {
                List<NoteDraft> userNotes = savedDrafts.get(chat_id);
                if (userNotes != null) {
                    for (NoteDraft draft : userNotes) {
                        SendMessage sendMessage = SendMessage
                                .builder()
                                .chatId(chat_id)
                                .parseMode("HTML")
                                .text("""
                                        <b>📚 Ваши заметки</b>
                                        
                                        🏷️ <code>%s</code> <b>| %s</b>
                                        <blockquote>%s</blockquote>
                                        """.formatted(draft))
                                .replyMarkup(keyboard2)
                                .build();
                        try {
                            telegramClient.execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    SendMessage sendMessage = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .parseMode("HTML")
                            .text("""
                                    <b>📭 Список ваших заметок пока пуст</b>
                                        
                                    <i>У вас ещё нет сохранённых заметок. Создайте первую и она появится здесь.</i>
                                    """)
                            .replyMarkup(keyboard2)
                            .build();
                    try {
                        telegramClient.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            String firstName = callbackQuery.getMessage().getChat().getFirstName();
            long chat_id = callbackQuery.getMessage().getChatId();

            InlineKeyboardButton keyboardButton = InlineKeyboardButton.builder()
                    .text("🏷️ Создать заметку")
                    .callbackData("create_note")
                    .build();

            InlineKeyboardButton keyboardButton1 = InlineKeyboardButton.builder()
                    .text("📕 Мои заметки")
                    .callbackData("notes_list")
                    .build();

            InlineKeyboardButton keyboardButton2 = InlineKeyboardButton.builder()
                    .text("🔍 Подробнее...")
                    .callbackData("faq")
                    .build();

            InlineKeyboardButton keyboardButton3 = InlineKeyboardButton.builder()
                    .text("❌ Назад")
                    .callbackData("back")
                    .build();

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            new InlineKeyboardRow(keyboardButton, keyboardButton1),
                            new InlineKeyboardRow(keyboardButton2)
                    ))
                    .build();

            InlineKeyboardMarkup keyboard2 = InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            new InlineKeyboardRow(keyboardButton3)
                    ))
                    .build();

            switch (data) {
                case "create_note" -> {
                    states.put(chat_id, DialogueStatus.AWAITING_TITLE);
                    drafts.put(chat_id, new NoteDraft());
                    SendMessage note = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .parseMode("HTML")
                            .text("""
                                    <b>🔖 Процесс создания заметки (1/3)</b>
                                    
                                    <i>Укажите название для заметки</i>
                                    """)
                            .replyMarkup(keyboard2)
                            .build();
                    try {
                        telegramClient.execute(note);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
                case "notes_list" -> {
                    List<NoteDraft> userNotes = savedDrafts.get(chat_id);
                    if (userNotes != null) {
                        for (NoteDraft draft : userNotes) {
                            SendMessage sendMessage = SendMessage
                                    .builder()
                                    .chatId(chat_id)
                                    .text("%s".formatted(draft))
                                    .replyMarkup(keyboard2)
                                    .build();
                            try {
                                telegramClient.execute(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        SendMessage sendMessage = SendMessage
                                .builder()
                                .chatId(chat_id)
                                .parseMode("HTML")
                                .text("""
                                        <b>📭 Список ваших заметок пока пуст</b>
                                        
                                        <i>У вас ещё нет сохранённых заметок. Создайте первую и она появится здесь.</i>
                                        """)
                                .replyMarkup(keyboard2)
                                .build();
                        try {
                            telegramClient.execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
                case "back" -> {
                    SendMessage message = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .parseMode("HTML")
                            .text("""
                                    <b>Приветствую, %s!</b>
                                    
                                    <i>Я - Meta, твой небольшой помощник для работы с заметками.</i>
                                    
                                    <blockquote>📝 Ты можешь отправлять мне свои мысли, задачи, идеи или любую другую информацию, которую хочешь сохранить. Я помогу организовать их с помощью уникальных тегов #, чтобы к заметкам было проще возвращаться и находить нужное.</blockquote>
                                    
                                    <i>📚 Все сохранённые заметки можно будет просмотреть в любой момент с помощью специальной команды.</i>
                                    """.formatted(firstName))
                            .replyMarkup(keyboard)
                            .build();
                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
                case "faq" -> {
                    SendMessage message = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .parseMode("HTML")
                            .text("""
                                    <b>Подробнее о Meta 💻</b>
                                    
                                    <i>Meta-бот для создания, хранения и удобного управления заметками
                                    Бот был создан в ходе практики Java Core, Stream API и SQL.</i>
                                    
                                    <blockquote>Проект имеет открытый исходный код и доступен на GitHub: https://github.com/WerLickGit/MetaBot</blockquote>
                                    """)
                            .replyMarkup(keyboard2)
                            .build();
                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}