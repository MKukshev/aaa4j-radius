# GitLab CI/CD Configuration

Этот проект настроен для автоматической сборки и деплоя через GitLab CI/CD.

## Структура Pipeline

### Этапы (Stages)

1. **validate** - Проверка качества кода
   - Checkstyle проверка
   - SpotBugs анализ
   - Валидация Maven проекта

2. **test** - Выполнение тестов
   - Unit тесты (JUnit 5)
   - Покрытие кода
   - Сканирование безопасности зависимостей

3. **build** - Компиляция
   - Компиляция всех модулей
   - Создание классов

4. **package** - Создание артефактов
   - Создание JAR файлов
   - Генерация документации

5. **deploy** - Деплой
   - GitLab Package Registry (snapshots)
   - Maven Central (releases)

## Переменные окружения

### Обязательные переменные

Для деплоя в Maven Central:
- `OSSRH_USERNAME` - Username для OSSRH
- `OSSRH_PASSWORD` - Password для OSSRH
- `GPG_PASSPHRASE` - Passphrase для GPG ключа

### Автоматические переменные GitLab

- `CI_JOB_TOKEN` - Токен для доступа к GitLab Package Registry
- `CI_API_V4_URL` - URL GitLab API
- `CI_PROJECT_ID` - ID проекта
- `CI` - Флаг, указывающий что выполняется в CI

## Настройка в GitLab

### 1. Переменные окружения

Перейдите в **Settings > CI/CD > Variables** и добавьте:

```
OSSRH_USERNAME = your_ossrh_username
OSSRH_PASSWORD = your_ossrh_password
GPG_PASSPHRASE = your_gpg_passphrase
```

### 2. GPG ключ

Для подписи артефактов добавьте GPG ключ:

1. Экспортируйте ваш GPG ключ:
   ```bash
   gpg --export-secret-keys --armor your_email@example.com
   ```

2. Добавьте переменную `GPG_PRIVATE_KEY` с содержимым ключа

### 3. Настройка Package Registry

GitLab Package Registry автоматически доступен через `CI_JOB_TOKEN`.

## Использование

### Автоматические триггеры

- **Merge Request** - запускает validate, test, build, package
- **Push в master** - запускает все этапы + deploy-snapshot (ручной)
- **Tag** - запускает все этапы + deploy-release (ручной)

### Ручной запуск

```bash
# Локальная сборка
mvn clean package

# С тестами
mvn clean verify

# С деплоем в GitLab Package Registry
mvn clean deploy -DskipTests
```

## Артефакты

### Создаваемые артефакты

- `aaa4j-radius-core-*.jar` - Основная библиотека
- `aaa4j-radius-client-*.jar` - Клиентская библиотека
- `aaa4j-radius-dictionary-freeradius-*.jar` - Словарь FreeRADIUS
- `aaa4j-radius-server-*.jar` - Серверная библиотека

### Отчеты

- JUnit отчеты тестов
- Checkstyle отчеты
- SpotBugs отчеты
- Отчеты покрытия кода
- Отчеты безопасности зависимостей

## Troubleshooting

### Проблемы с GPG

Если возникают проблемы с подписью:

1. Проверьте переменную `GPG_PRIVATE_KEY`
2. Убедитесь что `GPG_PASSPHRASE` корректна
3. В CI/CD GPG подпись автоматически пропускается (`gpg.skip=${env.CI}`)

### Проблемы с тестами

Если тесты падают в CI:

1. Проверьте логи в GitLab CI/CD
2. Убедитесь что все зависимости доступны
3. Проверьте настройки JVM памяти

### Проблемы с деплоем

1. Проверьте переменные окружения
2. Убедитесь что у вас есть права на деплой
3. Проверьте настройки Maven settings

## Локальная разработка

Для локальной разработки используйте:

```bash
# Сборка без тестов
mvn clean package -DskipTests

# Сборка с тестами
mvn clean verify

# Проверка качества кода
mvn checkstyle:check
mvn spotbugs:check

# Генерация документации
mvn javadoc:javadoc
```

## Конфигурация Maven

### Основные настройки

- Java 9+ (source/target)
- UTF-8 кодировка
- JUnit 5 для тестов
- Checkstyle для качества кода
- SpotBugs для поиска багов

### Плагины

- `maven-compiler-plugin` - компиляция
- `maven-surefire-plugin` - unit тесты
- `maven-failsafe-plugin` - integration тесты
- `maven-checkstyle-plugin` - проверка стиля
- `spotbugs-maven-plugin` - анализ кода
- `maven-gpg-plugin` - подпись артефактов
- `maven-javadoc-plugin` - документация
- `maven-source-plugin` - исходный код 