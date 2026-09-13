package io.github.werlickgit;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
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


    private InlineKeyboardMarkup buildMainMenuKeyboard() {
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

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(keyboardButton, keyboardButton1),
                        new InlineKeyboardRow(keyboardButton2)
                ))
                .build();
    }

    private void send(SendMessage sendMessage) {
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup buildBackKeyboard() {
        InlineKeyboardButton keyboardButton3 = InlineKeyboardButton.builder()
                .text("❌ Назад")
                .callbackData("back")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(keyboardButton3)
                ))
                .build();
    }

    private InlineKeyboardMarkup buildRemoveNoteKeyboard() {
        InlineKeyboardButton keyboardButton4 = InlineKeyboardButton.builder()
                .text("🛑 Удалить заметку")
                .callbackData("remove_note")
                .build();

        InlineKeyboardButton keyboardButton3 = InlineKeyboardButton.builder()
                .text("❌ Назад")
                .callbackData("back")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(keyboardButton4, keyboardButton3)
                ))
                .build();
    }

    private void handleAwaitingTitle(long chatId, String messageText) {
        drafts.get(chatId).setTitle(messageText);
        states.put(chatId, DialogueStatus.AWAITING_TAG);
        SendMessage sendMessage = SendMessage
                .builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text("""
                        <b>🔖 Процесс создания заметки (2/3)</b>
                        
                        <i>Тег обязательно должен начинаться с символа </i> <code>#</code>
                        <i>Например:</i> <code>#учёба</code>, <code>#идеи</code>, <code>#важное</code>
                        """)
                .replyMarkup(buildMainMenuKeyboard())
                .build();
        send(sendMessage);
    }

    private void handleAwaitingTag(long chatId, String messageText) {
        if (messageText.startsWith("#")) {
            drafts.get(chatId).setTag(messageText);
            states.put(chatId, DialogueStatus.AWAITING_TEXT);
            SendMessage sendMessage = SendMessage
                    .builder()
                    .chatId(chatId)
                    .parseMode("HTML")
                    .text("""
                            <b>🔖 Процесс создания заметки (3/3)</b>
                            
                            <i>Введите текст для заметки</i>
                            """)
                    .replyMarkup(buildBackKeyboard())
                    .build();
            send(sendMessage);
        } else {
            SendMessage sendMessage = SendMessage
                    .builder()
                    .chatId(chatId)
                    .parseMode("HTML")
                    .text("""
                            <i>❗ Вы не указали</i> <code>#</code> <i> в начале, попробуйте указать корректный тэг для вашей заметки</i>
                            """)
                    .replyMarkup(buildBackKeyboard())
                    .build();
            send(sendMessage);
        }
    }

    private void handleAwaitingText(long chatId, String messageText) {
        drafts.get(chatId).setText(messageText);
        String title = drafts.get(chatId).getTitle();
        String tag = drafts.get(chatId).getTag();
        String text = drafts.get(chatId).getText();
        SendMessage sendMessage = SendMessage
                .builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text("""
                        <b>✅ Заметка успешно создана</b>
                        
                        <i>🏷️ Тег:</i> <code>%s</code>
                        <i>📝 Название: %s</i>
                        
                        <b>Содержание заметки:</b>
                        
                        <blockquote>%s</blockquote> 
                        """.formatted(tag, title, text))
                .replyMarkup(buildMainMenuKeyboard())
                .build();
        states.put(chatId, DialogueStatus.NONE);
        List<NoteDraft> userNotes = savedDrafts.computeIfAbsent(chatId, k -> new ArrayList<>());
        userNotes.add(drafts.get(chatId));
        send(sendMessage);
    }

    private void handleStartCommand(long chatId, String firstName) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text("""
                        <b>Приветствую, %s!</b>
                        
                        <i>Я - Meta, твой небольшой помощник для работы с заметками.</i>
                        
                        <blockquote>📝 Ты можешь отправлять мне свои мысли, задачи, идеи или любую другую информацию, которую хочешь сохранить. Я помогу организовать их с помощью уникальных тегов #, чтобы к заметкам было проще возвращаться и находить нужное.</blockquote>
                        
                        <i>📚 Все сохранённые заметки можно будет просмотреть в любой момент с помощью специальной команды.</i>
                        """.formatted(firstName))
                .replyMarkup(buildMainMenuKeyboard())
                .build();
        send(message);
    }

    private void handleNoteCommand(long chatId) {
        drafts.put(chatId, new NoteDraft());
        states.put(chatId, DialogueStatus.AWAITING_TITLE);

        SendMessage note = SendMessage
                .builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text("""
                        <b>🔖 Процесс создания заметки (1/3)</b>
                        
                        <i>Укажите название для заметки</i>
                        """)
                .replyMarkup(buildBackKeyboard())
                .build();
        send(note);
    }

    private void handleNotesCommand(long chatId) {
        List<NoteDraft> userNotes = savedDrafts.get(chatId);
        if (userNotes != null) {
            for (NoteDraft draft : userNotes) {
                SendMessage sendMessage = SendMessage
                        .builder()
                        .chatId(chatId)
                        .parseMode("HTML")
                        .text("""
                                %s
                                """.formatted(draft))
                        .replyMarkup(buildRemoveNoteKeyboard())
                        .build();
                send(sendMessage);
            }
        } else {
            SendMessage sendMessage = SendMessage
                    .builder()
                    .chatId(chatId)
                    .parseMode("HTML")
                    .text("""
                            <b>📭 Список ваших заметок пока пуст</b>
                            
                            <i>У вас ещё нет сохранённых заметок. Создайте первую и она появится здесь.</i>
                            """)
                    .replyMarkup(buildBackKeyboard())
                    .build();
            send(sendMessage);
        }
    }

    private void handleRemoveCommand(long chatId) {
        states.put(chatId, DialogueStatus.AWAITING_REMOVENOTE);
        SendMessage sendMessage = SendMessage
                .builder()
                .chatId(chatId)
                .text("""
                        <i>🗑 Введите название заметки, которую хотите удалить</i>
                        """)
                .replyMarkup(buildBackKeyboard())
                .build();
        send(sendMessage);
    }

    private void handleAwaitingRemoveNote(long chatId, String messageText) {
        List<NoteDraft> userNotes = savedDrafts.get(chatId);
        boolean removed = userNotes != null && userNotes.removeIf(note -> !note.equals(null) && note.getTitle().equals(messageText));
        if (removed) {
            SendMessage sendMessage = SendMessage
                    .builder()
                    .chatId(chatId)
                    .text("""
                            <i>🗑 Заметка %s успешно удалена</i>
                            """.formatted(messageText))
                    .replyMarkup(buildBackKeyboard())
                    .build();
            states.put(chatId, DialogueStatus.NONE);
            send(sendMessage);
        } else {
            SendMessage sendMessage = SendMessage
                    .builder()
                    .chatId(chatId)
                    .text("""
                            <b>❌ Заметки с таким названием не существует</b>
                            
                            <i>Проверьте название и попробуйте ещё раз</i>
                            """)
                    .replyMarkup(buildBackKeyboard())
                    .build();
            send(sendMessage);
        }
    }

    private void handleCallbackQuery(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            String firstName = callbackQuery.getMessage().getChat().getFirstName();
            long chat_id = callbackQuery.getMessage().getChatId();

            try {
                telegramClient.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

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
                            .replyMarkup(buildBackKeyboard())
                            .build();
                    send(note);
                }
                case "notes_list" -> {
                    List<NoteDraft> userNotes = savedDrafts.get(chat_id);
                    if (userNotes != null) {
                        for (NoteDraft draft : userNotes) {
                            SendMessage sendMessage = SendMessage
                                    .builder()
                                    .chatId(chat_id)
                                    .parseMode("HTML")
                                    .text("%s".formatted(draft))
                                    .replyMarkup(buildRemoveNoteKeyboard())
                                    .build();
                            send(sendMessage);
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
                                .replyMarkup(buildRemoveNoteKeyboard())
                                .build();
                        send(sendMessage);
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
                            .replyMarkup(buildMainMenuKeyboard())
                            .build();
                    send(message);
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
                            .replyMarkup(buildBackKeyboard())
                            .build();
                    send(message);
                }
                case "remove_note" -> {
                    states.put(chat_id, DialogueStatus.AWAITING_REMOVENOTE);
                    SendMessage sendMessage = SendMessage
                            .builder()
                            .chatId(chat_id)
                            .text("""
                        <i>🗑 Введите название заметки, которую хотите удалить</i>
                        """)
                            .replyMarkup(buildBackKeyboard())
                            .build();
                    send(sendMessage);
                }
            }
        }
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            User user = update.getMessage().getFrom();
            long chat_id = update.getMessage().getChatId();
            String firstName = user.getFirstName();
            DialogueStatus currentStatus = states.get(chat_id);

            if (currentStatus == DialogueStatus.AWAITING_TITLE) {
                handleAwaitingTitle(chat_id, message_text);
            } else if (currentStatus == DialogueStatus.AWAITING_TAG) {
                handleAwaitingTag(chat_id, message_text);
            } else if (currentStatus == DialogueStatus.AWAITING_TEXT) {
                handleAwaitingText(chat_id, message_text);
            } else if (currentStatus == DialogueStatus.AWAITING_REMOVENOTE) {
                handleAwaitingRemoveNote(chat_id, message_text);
            } else if (message_text.equals("/start")) {
                handleStartCommand(chat_id, user.getFirstName());
            } else if (message_text.equals("/note")) {
                handleNoteCommand(chat_id);
            } else if (message_text.equals("/notes")) {
                handleNotesCommand(chat_id);
            } else if (message_text.equals("/remove")) {
                handleRemoveCommand(chat_id);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }
}