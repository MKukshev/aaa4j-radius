# Комментарии к новому коду в package org.aaa4j.radius.client.clients

## Обзор

Добавлены подробные JavaDoc комментарии ко всему новому коду, связанному с поддержкой `RetransmissionStrategy` в TCP клиентах AAA4J-RADIUS библиотеки.

## Добавленные комментарии

### 1. BaseTcpRadiusClient.java

#### Константы
```java
/**
 * Default retransmission strategy for TCP clients.
 * Uses 3 attempts with 5-second intervals between attempts.
 */
private static final RetransmissionStrategy DEFAULT_RETRANSMISSION_STRATEGY = 
    new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5));
```

#### Поля класса
```java
/**
 * Strategy for handling retransmissions when network failures occur.
 * Determines the number of attempts and timeout for each attempt.
 */
protected final RetransmissionStrategy retransmissionStrategy;
```

#### Конструктор
```java
// Initialize retransmission strategy with default if not provided
this.retransmissionStrategy = builder.retransmissionStrategy == null
    ? DEFAULT_RETRANSMISSION_STRATEGY
    : builder.retransmissionStrategy;
```

#### Метод sendAsync
```java
// Ensure connection is established before sending
if (!isConnected()) { ... }

// Generate authenticator for request
byte[] authenticatorBytes = new byte[16];

// Encode the request packet
byte[] outBytes = packetCodec.encodeRequest(requestPacket, secret, authenticatorBytes);

// Get retransmission strategy parameters
int maxAttempts = retransmissionStrategy.getMaxAttempts();

// Attempt to send the packet with retransmission logic
for (int attempt = 0; attempt < maxAttempts; attempt++) {
    // Get timeout for current attempt from strategy
    Duration timeoutDuration = retransmissionStrategy.timeoutForAttempt(attempt);
    
    // Set socket timeout for this attempt
    if (socket != null && !socket.isClosed()) { ... }
    
    // Validate response packet size
    if (responseLength > MAX_PACKET_SIZE) { ... }
    
    // Read complete response data
    byte[] inBytes = new byte[responseLength];
    
    // Decode and return the response
    return packetCodec.decodeResponse(inBytes, secret, authenticatorBytes);
}

// Store the exception for final error reporting
lastException = e;

// Close current connection to force reconnection
if (socket != null && !socket.isClosed()) { ... }

// Attempt to establish new connection
connect().get();

// If reconnection fails, continue to next attempt
// The connection will be attempted again in the next loop iteration
continue;

// If we get here, all transmission attempts have failed
if (lastException != null) { ... }
```

#### Builder класс
```java
/**
 * Strategy for handling retransmissions when network failures occur.
 * If not set, a default strategy will be used.
 */
protected RetransmissionStrategy retransmissionStrategy;

/**
 * Sets the retransmission strategy for handling network failures.
 * The strategy determines how many times to retry and what timeout to use for each attempt.
 * If not set, a default strategy will be used (3 attempts with 5-second intervals).
 *
 * @param retransmissionStrategy the retransmission strategy to use
 * @return this builder for method chaining
 */
public T retransmissionStrategy(RetransmissionStrategy retransmissionStrategy) { ... }
```

### 2. TcpRadiusClient.java

#### Класс
```java
/**
 * A RADIUS client using TCP as the underlying transport layer. Create an instance using {@link Builder}.
 * 
 * <p>This client supports both synchronous and asynchronous operations, connection management,
 * keep-alive, automatic reconnection, and configurable retransmission strategies for handling
 * network failures.</p>
 */
```

### 3. RadSecRadiusClient.java

#### Класс
```java
/**
 * A RADIUS client using RadSec (TLS over TCP) as the underlying transport layer. Create an instance using {@link Builder}.
 * 
 * <p>This client supports both synchronous and asynchronous operations, connection management,
 * keep-alive, automatic reconnection, and configurable retransmission strategies for handling
 * network failures over a secure TLS connection.</p>
 */
```

### 4. RadiusClientException.java

#### Конструктор
```java
/**
 * Creates a new RadiusClientException with the specified message and cause.
 * This constructor is used for wrapping other exceptions with additional context.
 *
 * @param message the detail message
 * @param cause the cause of the exception
 */
public RadiusClientException(String message, Throwable cause) { ... }
```

### 5. ClientExamples.java

#### TCP Client Example
```java
/**
 * Example of using the TCP RADIUS client with retransmission strategy.
 * This demonstrates how to configure retransmission behavior for handling network failures.
 */
public static void tcpClientExample() {
    // Configure retransmission strategy: 3 attempts with 5-second intervals
    .retransmissionStrategy(new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5)))
}
```

#### RadSec Client Example
```java
/**
 * Example of using the RadSec RADIUS client with retransmission strategy.
 * This demonstrates how to configure retransmission behavior for secure TLS connections.
 */
public static void radSecClientExample() {
    // Configure retransmission strategy: 3 attempts with 5-second intervals
    .retransmissionStrategy(new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5)))
}
```

## Типы комментариев

### 1. JavaDoc комментарии
- **Классы**: Описание назначения и возможностей
- **Методы**: Описание параметров, возвращаемых значений и поведения
- **Поля**: Описание назначения и использования
- **Конструкторы**: Описание параметров и инициализации

### 2. Inline комментарии
- **Логические блоки**: Объяснение последовательности операций
- **Обработка ошибок**: Пояснение стратегии обработки исключений
- **Retransmission логика**: Детальное описание алгоритма повторных попыток
- **Сетевое взаимодействие**: Пояснение протокола TCP/TLS

## Преимущества добавленных комментариев

1. **Понятность кода**: Каждый блок кода имеет четкое объяснение назначения
2. **Документация API**: JavaDoc комментарии генерируют автоматическую документацию
3. **Обучение**: Комментарии помогают новым разработчикам понять архитектуру
4. **Отладка**: Подробные комментарии упрощают поиск и исправление ошибок
5. **Сопровождение**: Четкое понимание логики упрощает будущие изменения

## Проверка качества

- ✅ Все комментарии соответствуют JavaDoc стандартам
- ✅ Проект успешно компилируется
- ✅ Комментарии покрывают все новые функции
- ✅ Примеры использования документированы
- ✅ Архитектурные решения объяснены

Код теперь полностью документирован и готов для production использования с понятной и поддерживаемой кодовой базой. 