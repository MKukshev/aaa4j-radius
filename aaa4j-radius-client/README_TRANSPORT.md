# Универсальный RADIUS Клиент с Поддержкой Различных Транспортов

AAA4J-RADIUS теперь поддерживает универсальный клиент, который позволяет выбирать между различными транспортными реализациями через builder pattern.

## Обзор

`UniversalRadiusClient` предоставляет единый API для всех типов транспорта, что упрощает переключение между ними в зависимости от требований к производительности:

- **Socket Transport** - стандартные Java Socket API (по умолчанию)
- **Netty Transport** - высокопроизводительный Netty-based транспорт (требует зависимости Netty)

## Архитектура

```
UniversalRadiusClient
├── RadiusTransport (интерфейс)
│   ├── SocketRadiusTransport (Socket API)
│   └── NettyRadiusTransport (Netty API)
├── TransportConfig (конфигурация)
└── RadiusTransportFactory (фабрика)
```

## Быстрый старт

### Socket Transport (по умолчанию)

```java
import org.aaa4j.radius.client.clients.UniversalRadiusClient;
import org.aaa4j.radius.client.transport.BaseTransportConfig;
import org.aaa4j.radius.client.transport.TransportType;

// Создаем конфигурацию транспорта
BaseTransportConfig transportConfig = new BaseTransportConfig.Builder()
    .serverAddress("radius.example.com")
    .serverPort(1812)
    .connectionTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .maxReconnectAttempts(3)
    .reconnectDelay(Duration.ofSeconds(5))
    .autoReconnectEnabled(true)
    .build();

// Создаем универсальный клиент
UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET) // По умолчанию TCP
    .transportConfig(transportConfig)
    .secret("secret".getBytes())
    .build();

// Используем клиент
Packet request = new AccessRequest();
Packet response = client.send(request);
```

### UDP Transport

```java
// Для UDP используем Socket транспорт с отключенным auto-reconnect
BaseTransportConfig udpConfig = new BaseTransportConfig.Builder()
    .serverAddress("radius.example.com")
    .serverPort(1812)
    .autoReconnectEnabled(false) // UDP не требует переподключения
    .build();

UniversalRadiusClient udpClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET)
    .transportConfig(udpConfig)
    .secret("secret".getBytes())
    .build();
```

### Netty Transport (требует зависимости)

Для использования Netty транспорта добавьте зависимость в `pom.xml`:

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.100.Final</version>
</dependency>
```

Затем используйте:

```java
UniversalRadiusClient nettyClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.NETTY)
    .transportConfig(transportConfig)
    .secret("secret".getBytes())
    .build();
```

## Конфигурация транспорта

### BaseTransportConfig

```java
BaseTransportConfig config = new BaseTransportConfig.Builder()
    .serverAddress("radius.example.com")        // Адрес сервера
    .serverPort(1812)                           // Порт сервера
    .connectionTimeout(Duration.ofSeconds(10))  // Таймаут соединения
    .readTimeout(Duration.ofSeconds(30))        // Таймаут чтения
    .writeTimeout(Duration.ofSeconds(30))       // Таймаут записи
    .maxReconnectAttempts(3)                    // Максимум попыток переподключения
    .reconnectDelay(Duration.ofSeconds(5))      // Задержка между попытками
    .autoReconnectEnabled(true)                 // Автоматическое переподключение
    .build();
```

### Настройки по умолчанию

- `connectionTimeout`: 30 секунд
- `readTimeout`: 30 секунд
- `writeTimeout`: 30 секунд
- `maxReconnectAttempts`: 3
- `reconnectDelay`: 5 секунд
- `autoReconnectEnabled`: true

## Асинхронное использование

```java
// Асинхронная отправка
client.sendAsync(request)
    .thenAccept(response -> {
        System.out.println("Response: " + response.getCode());
    })
    .exceptionally(throwable -> {
        System.err.println("Error: " + throwable.getMessage());
        return null;
    });

// Управление соединением
client.connect().thenRun(() -> {
    System.out.println("Connected successfully");
});

client.close().thenRun(() -> {
    System.out.println("Connection closed");
});
```

## Управление соединением

```java
// Проверка состояния соединения
if (client.isConnected()) {
    System.out.println("Client is connected");
}

// Подключение
client.connect().get(); // Синхронное ожидание

// Закрытие
client.close().get(); // Синхронное ожидание
```

## Выбор транспорта

### Когда использовать Socket Transport

- **Простота**: Минимальная настройка
- **Стандартность**: Использует стандартные Java API
- **Низкая нагрузка**: До 1000 запросов в секунду
- **Прототипирование**: Быстрое создание прототипов

### Когда использовать Netty Transport

- **Высокая нагрузка**: Более 1000 запросов в секунду
- **Множественные соединения**: Тысячи одновременных соединений
- **Производительность**: Критична производительность
- **Event-driven архитектура**: Неблокирующие операции

## Примеры производительности

### Настройка для высокой нагрузки

```java
BaseTransportConfig highPerfConfig = new BaseTransportConfig.Builder()
    .serverAddress("radius.example.com")
    .serverPort(1812)
    .connectionTimeout(Duration.ofSeconds(3))    // Быстрое подключение
    .readTimeout(Duration.ofSeconds(10))         // Короткий таймаут чтения
    .writeTimeout(Duration.ofSeconds(5))         // Короткий таймаут записи
    .maxReconnectAttempts(10)                    // Больше попыток
    .reconnectDelay(Duration.ofMillis(100))     // Быстрое переподключение
    .autoReconnectEnabled(true)
    .build();

UniversalRadiusClient highPerfClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.NETTY)          // Netty для максимальной производительности
    .transportConfig(highPerfConfig)
    .secret("secret".getBytes())
    .build();
```

### Массовая отправка запросов

```java
// Отправка 1000 запросов асинхронно
for (int i = 0; i < 1000; i++) {
    final int requestId = i;
    Packet request = new AccessRequest();
    
    client.sendAsync(request)
        .thenAccept(response -> {
            System.out.println("Response " + requestId + ": " + response.getCode());
        })
        .exceptionally(throwable -> {
            System.err.println("Error " + requestId + ": " + throwable.getMessage());
            return null;
        });
}
```

## Миграция с существующих клиентов

### С UdpRadiusClient

```java
// Старый код
UdpRadiusClient oldClient = UdpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 1812))
    .secret("secret".getBytes())
    .build();

// Новый код
BaseTransportConfig config = new BaseTransportConfig.Builder()
    .serverAddress("radius.example.com")
    .serverPort(1812)
    .autoReconnectEnabled(false)
    .build();

UniversalRadiusClient newClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET)
    .transportConfig(config)
    .secret("secret".getBytes())
    .build();
```

### С TcpRadiusClient

```java
// Старый код
TcpRadiusClient oldClient = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("secret".getBytes())
    .build();

// Новый код
BaseTransportConfig config = new BaseTransportConfig.Builder()
    .serverAddress("radius.example.com")
    .serverPort(2083)
    .autoReconnectEnabled(true)
    .build();

UniversalRadiusClient newClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET)
    .transportConfig(config)
    .secret("secret".getBytes())
    .build();
```

## Обработка ошибок

```java
try {
    Packet response = client.send(request);
    System.out.println("Success: " + response.getCode());
} catch (RadiusClientException e) {
    System.err.println("RADIUS error: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Unexpected error: " + e.getMessage());
}

// Асинхронная обработка ошибок
client.sendAsync(request)
    .thenAccept(response -> {
        System.out.println("Success: " + response.getCode());
    })
    .exceptionally(throwable -> {
        if (throwable.getCause() instanceof RadiusClientException) {
            System.err.println("RADIUS error: " + throwable.getCause().getMessage());
        } else {
            System.err.println("Unexpected error: " + throwable.getMessage());
        }
        return null;
    });
```

## Заключение

Универсальный RADIUS клиент предоставляет гибкий и мощный способ работы с различными транспортными реализациями. Используйте Socket транспорт для простых случаев и Netty транспорт для высоконагруженных сценариев.

Для получения дополнительной информации см. примеры в `UniversalClientExamples.java`. 