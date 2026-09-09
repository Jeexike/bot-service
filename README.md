# 🤖 bot-service

Пограничный (edge) сервис поверх `task-service`: REST-прокси с отказоустойчивым HTTP-клиентом, Telegram-бот для управления заказами и партнёрами в чате, и потребитель Kafka-событий об изменениях в связанных GitHub-репозиториях.

> Часть платформы из двух сервисов. Второй репозиторий — **[task-service](https://github.com/<org>/task-service)**: ядро с заказами, партнёрами, PostgreSQL и трекингом GitHub-репозиториев, который этот сервис проксирует.

## Как это работает вместе

```
Telegram ──▶ bot-service ──REST(resilient)──▶ task-service ──▶ PostgreSQL
                  ▲                                 │
                  │                                 ▼
                  └──────── Kafka (order.link.changed) ◀── планировщик трекинга
                                                             GitHub-репозиториев
```

1. Клиент (REST или Telegram) создаёт заказ здесь, в `bot-service`, указывая ссылку на GitHub-репозиторий.
2. `bot-service` резилиентно проксирует запрос в `task-service`, где заказ сохраняется в PostgreSQL.
3. Фоновый планировщик `task-service` периодически опрашивает GitHub API по всем заказам и сравнивает состояние репозитория со снапшотом.
4. При обнаружении изменений событие пишется в транзакционный outbox и асинхронно публикуется в Kafka-топик `order.link.changed`.
5. Этот сервис потребляет это событие как консьюмер того же топика.

## Что делает сервис

- **REST-прокси к task-service** — повторяет контракт `task-service` (`/orders`, `/partners`), но каждый вызов обёрнут в Resilience4j: rate limiter, retry с экспоненциальным backoff и circuit breaker с осмысленным фолбэком (различает бизнес-ошибки 4xx, превышение лимита и реальную недоступность бэкенда).
- **Telegram-бот** — полноценный интерфейс поверх того же API: пошаговые диалоги (FSM по `chatId`) для создания партнёра и заказа, списки, удаление, help. Работает через long polling к Telegram Bot API.
- **Kafka-consumer** — слушает топик `order.link.changed`, который наполняет `task-service` при обнаружении изменений в отслеживаемом GitHub-репозитории заказа (пока — логирует событие; точка расширения для уведомлений/интеграций).
- **OpenAPI/Swagger** — самодокументируемый REST-слой прокси.

## Технологический стек

| Категория | Технологии |
|---|---|
| Язык / платформа | Java, Spring Boot 4.0.6 |
| Web | Spring Web MVC, Spring RestClient, springdoc-openapi |
| Асинхронность | Apache Kafka consumer (`spring-kafka`) |
| Отказоустойчивость | Resilience4j (CircuitBreaker, RateLimiter, Retry), Spring Retry, Spring AOP |
| Интеграции | Telegram Bot API (long polling, собственный клиент на RestClient) |
| Тестирование | JUnit 5, Spring Boot Test |
| Прочее | Lombok, Spotless (palantir-java-format) |

## Архитектура и ключевые модули

```
controller/   REST-контроллеры прокси (Orders, Partners) + OpenAPI-интерфейсы
client/       OrderClient / PartnerClient — типобезопасные обёртки над RestClient
              с аннотациями @RateLimiter/@Retry/@CircuitBreaker и общей логикой
              фолбэков (ClientResilienceSupport)
dto/          Request/Response модели, зеркалящие контракт task-service
kafka/        Consumer события OrderLinkChangedEvent из топика order.link.changed
telegram/     Long-polling бот: TelegramLongPollingBot, TelegramApiClient (sendMessage/getUpdates),
              OrderTelegramHandler (роутинг команд и диалоги), сессии создания
              заказа/партнёра (CreateOrderSession, CreatePartnerSession)
exception/    GlobalExceptionHandler, OrderServiceUnavailableException
config/       RestClient(ы) к task-service, RestClientProperties, OpenAPI
```

### Слой отказоустойчивости клиента

`OrderClient`/`PartnerClient` оборачивают каждый вызов к `task-service` в связку `@RateLimiter` → `@Retry` → `@CircuitBreaker`. При открытом circuit breaker или исчерпании retry срабатывает fallback-метод: бизнес-исключения и превышение rate limit пробрасываются как есть (`ClientResilienceSupport.rethrowBusinessOrRateLimit`), а инфраструктурные сбои превращаются в единообразный `OrderServiceUnavailableException` (для `getOrders()` — деградация до пустого списка вместо ошибки).

### Telegram-бот как второй UI

Бот не дублирует бизнес-логику — он вызывает те же `OrderClient`/`PartnerClient`, что и REST-контроллеры, т.е. пользуется той же отказоустойчивостью. Диалоги создания заказа/партнёра реализованы как простые конечные автоматы (`Step`-энам + сессия в `ConcurrentHashMap<chatId, Session>`), сброс — по `/cancel` или при ошибке.

## API

Базовый путь: `/orders`, `/partners` — контракт идентичен `task-service`, но с добавленной устойчивостью к сбоям бэкенда. Полная спецификация — Swagger UI прокси.

## Telegram-бот: команды

| Команда | Действие |
|---|---|
| `/start`, `/help` | Приветствие / справка |
| `/partner` | Создать партнёра (диалог: имя → email) |
| `/partners` | Список партнёров |
| `/deletepartner <uuid>` | Удалить партнёра (каскадно удаляет его заказы) |
| `/create` | Создать заказ (диалог: name → source → destination → GitHub link → partnerId) |
| `/orders` / `/orders <partnerId>` | Все заказы / заказы конкретного партнёра |
| `/delete <uuid>` | Удалить заказ |
| `/cancel` | Отменить текущий диалог |

## Запуск локально

Сервис использует Kafka из `task-service/compose.yaml` и обращается к `task-service` по REST — оба сервиса должны быть подняты вместе.

```bash
./mvnw spring-boot:run
```

По умолчанию слушает порт `8080` и ждёт `task-service` на `localhost:8081`.

### Переменные окружения

| Переменная | Назначение | По умолчанию |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | Адрес брокера Kafka | `localhost:9092` |
| `TELEGRAM_BOT_TOKEN` | Токен Telegram-бота (BotFather) | пусто — бот не запустится без него |
| `TELEGRAM_BOT_USERNAME` | Username бота | пусто |

### Основные настраиваемые параметры (`application.yaml`)

- `rest-client.base-url` / `partners-base-url` — адреса `task-service`
- `rest-client.connect-timeout` / `read-timeout` / `max-attempts` / `backoff-delay`
- `telegram.bot.enabled` — включение/выключение бота
- `telegram.bot.poll-timeout-seconds` — таймаут long polling
- `resilience4j.*` — тюнинг устойчивости вызовов к `task-service` (отдельный профиль — в `application-resilience.yaml`)
- `tracking.outbox.topic` — топик Kafka, из которого потребляются события об изменении репозиториев

## Тестирование

```bash
./mvnw test
```

Покрытие включает тесты контроллеров (Orders/Partners), проверку резилиенс-обёрток клиентов (`ClientResilienceSupportTest`, `OrderClientResilienceTest`) и корректность конфигурации RestClient-бинов (`RestClientWiringTest`).