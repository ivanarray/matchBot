package ru.urfu.bot.registration;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.urfu.bot.Bot;
import ru.urfu.bot.IUpdate;
import ru.urfu.profile.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Класс регистратора
 */

public class Registrar {
    final Bot bot;
    final Map<Long, ProfileInRegistration> profilesInRegistration = new ConcurrentHashMap<>();
    final Map<String, Consumer<IUpdate>> handlers = Map.of(
            "Имя", this::nameHandler,
            "Возраст", this::ageHandler,
            "Город", this::cityHandler,
            "Пол", this::genderHandler,
            "Фото", this::photoHandler);


    public Registrar(Bot bot) {
        this.bot = bot;
    }

    /**
     * Сам метод регистрации
     *
     * @param update обрабатывает изменения
     * @throws Exception эксепшны от используемых методов
     */
    public void registerFromUpdate(IUpdate update) throws Exception {
        var id = update.getMessage().getFrom().getId();

        if (!profilesInRegistration.containsKey(id)) {
            var profile = new Profile(id);
            profile.setStatus(ProfileStatus.registration);
            profilesInRegistration.put(id, new ProfileInRegistration(profile));

            profile.setTelegramUserName(update.getMessage().getFrom().getUserName());

            bot.execute(SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Тебя нет в нашей базе, давай зарегистрируемся\n\nНапиши свое имя:)")
                    .build());

            return;
        }

        handlers.get(profilesInRegistration.get(id).getCurrentRegistrationStep()).accept(update);

        if (profilesInRegistration.get(id).isRegistrationCompleted()) {
            ProfileData.addProfile(profilesInRegistration.get(id).getProfile());
            profilesInRegistration.remove(id);
        }
    }

    /**
     * Получаем user id
     *
     * @param update update
     * @return id
     */
    public long getId(IUpdate update) {
        return update.getMessage().getFrom().getId();
    }

    /**
     * Получаем город
     *
     * @param update update
     */

    private void cityHandler(IUpdate update) {
        profilesInRegistration.get(getId(update)).updateProgress();
        profilesInRegistration.get(getId(update)).getProfile().setCity(update.getMessage().getText());

        try {
            bot.execute(SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Отправь свое фото")
                    .build());
        } catch (Exception ignored) {
        }
    }

    /**
     * Получаем фото
     *
     * @param update update
     */

    private void photoHandler(IUpdate update) {
        try {
            var photo = update.getMessage().getPhoto().get(0);

            profilesInRegistration.get(getId(update))
                    .getProfile()
                    .setPhotoLink(photo.getFileId());

            profilesInRegistration.get(getId(update)).updateProgress();

            ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(List.of(new KeyboardRow(
                    List.of(new KeyboardButton("Поехали!\uD83D\uDE40"))
            )), true, false, false, " ");

            bot.execute(SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Готово, теперь ты в базе и можешь знакомиться")
                    .replyMarkup(keyboard)
                    .build());

        } catch (Exception ignored) {
        }
    }

    /**
     * Получаем возраст
     *
     * @param update update
     */

    private void ageHandler(IUpdate update) {
        try {
            var age = Integer.parseInt(update.getMessage().getText());
            var id = getId(update);
            profilesInRegistration.get(id).getProfile().setAge(age);
            profilesInRegistration.get(id).updateProgress();
            bot.execute(SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Напиши свой город")
                    .build());
        } catch (Exception e) {
            try {
                bot.execute(SendMessage.builder()
                        .chatId(update.getMessage().getChatId().toString())
                        .text("Введи возраст еще раз")
                        .build());
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Получаем пол
     *
     * @param update update
     */
    private void genderHandler(IUpdate update) {
        var id = getId(update);

        if (Objects.equals(update.getMessage().getText(), "Женский\uD83D\uDE4B\u200D♀️")) {
            profilesInRegistration.get(id).getProfile().setGender(Gender.female);
        }

        if (Objects.equals(update.getMessage().getText(), "Non-Binary\uD83C\uDFF3️\u200D⚧️")) {
            profilesInRegistration.get(id).getProfile().setGender(Gender.other);
        }

        profilesInRegistration.get(id).getProfile().setGender(Gender.male);
        profilesInRegistration.get(id).updateProgress();

        try {
            bot.execute(SendMessage.builder()
                    .replyMarkup(new ReplyKeyboardRemove(true))
                    .chatId(update.getMessage().getChatId().toString())
                    .text("Напиши свой возраст")
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получаем имя
     *
     * @param update update
     */
    public void nameHandler(IUpdate update) {
        var id = getId(update);

        profilesInRegistration.get(id).getProfile().setName(update.getMessage().getText());
        profilesInRegistration.get(id).updateProgress();
        List<KeyboardButton> buttons = List.of(
                new KeyboardButton("Мужской\uD83D\uDE4B\u200D♂️"),
                new KeyboardButton("Женский\uD83D\uDE4B\u200D♀️"),
                new KeyboardButton("Non-Binary\uD83C\uDFF3️\u200D⚧️"));

        KeyboardRow row = new KeyboardRow(buttons);
        ReplyKeyboard replyKeyboard =
                new ReplyKeyboardMarkup
                        (List.of(row), true, true, false, " ");

        try {
            bot.execute(SendMessage.builder()
                    .chatId(update.getMessage().getChatId().toString())
                    .replyMarkup(replyKeyboard)
                    .text("Выбери свой пол:)")
                    .build()
            );
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public boolean inRegistration(long id) {
        return profilesInRegistration.containsKey(id);
    }


}
