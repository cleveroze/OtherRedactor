<p align="center">
  <img src="docs/logo.webp" width="128" alt="OtherRedactor logo">
</p>

<h1 align="center">OtherRedactor</h1>

<p align="center">
  HTML, CSS & JavaScript editor for Android in Material 3 Expressive style.<br>
  Редактор HTML, CSS и JavaScript для Android в стиле Material 3 Expressive.
</p>

<p align="center">
  <a href="../../releases/latest"><b>⬇ Download APK</b></a> ·
  <a href="https://t.me/OtherGramWorks">Telegram</a>
</p>

## Возможности / Features

- **Редактор кода**: подсветка HTML/CSS/JS, номера строк, автозакрытие тегов, скобок и кавычек, автоотступ, поиск и замена, переход к строке, комментирование, отмена и повтор, панель символов и сниппетов над клавиатурой
- **Вкладки как в браузере**: переключение между файлами одним касанием
- **Предпросмотр на разных устройствах**: это устройство, iPhone, Pixel, Galaxy, складной смартфон, iPad, ноутбук, Full HD монитор или своё разрешение; есть поворот экрана
- **JS-консоль**: `console.log`, ошибки страницы и выполнение своего JavaScript
- **Файлы и папки**: проекты внутри приложения, импорт и экспорт, открытие и сохранение файлов на устройстве, открытие `.html` из других приложений
- **Оформление**: Material 3 Expressive, морфинг фигур, пружинные анимации; ванильная палитра под логотип, Material You и ещё 5 цветов; светлая, тёмная и AMOLED тема
- **Русский и English**, язык переключается прямо в приложении

## Сборка / Build

APK собирается в GitHub Actions при каждом пуше. Готовые сборки лежат в [Releases](../../releases).

```bash
./gradlew assembleRelease
```

Требуются JDK 17+ и Android SDK 37.
