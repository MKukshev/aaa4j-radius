# AAA4J-RADIUS Client

AAA4J-RADIUS Client provides multiple RADIUS client implementations supporting different transport protocols and connection modes.

## Supported Transports

- **UDP** — классический RADIUS по UDP (UdpRadiusClient)
- **TCP** — надёжный RADIUS по TCP (TcpRadiusClient)
- **RadSec** — защищённый RADIUS по TLS (RadSecRadiusClient)
- **Универсальный клиент** — UniversalRadiusClient с выбором транспорта через TransportType (SOCKET, SOCKET_TCP, SOCKET_RADSEC, NETTY и др.)

## UniversalRadiusClient Initialization Examples

```java
import org.aaa4j.radius.client.clients.UniversalRadiusClient;
import org.aaa4j.radius.client.transport.BaseTransportConfig;
import org.aaa4j.radius.client.transport.TransportType;

// UDP Transport
UniversalRadiusClient udpClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET)
    .transportConfig(new BaseTransportConfig.Builder()
        .serverAddress("radius.example.com")
        .serverPort(1812)
        .build())
    .secret("shared-secret".getBytes())
    .build();

// TCP Transport
UniversalRadiusClient tcpClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET_TCP)
    .transportConfig(new BaseTransportConfig.Builder()
        .serverAddress("radius.example.com")
        .serverPort(2083)
        .autoReconnectEnabled(true)
        .build())
    .secret("shared-secret".getBytes())
    .build();

// RadSec Transport
UniversalRadiusClient radSecClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET_RADSEC)
    .transportConfig(new BaseTransportConfig.Builder()
        .serverAddress("radius.example.com")
        .serverPort(2083)
        .autoReconnectEnabled(true)
        .build())
    .secret("shared-secret".getBytes())
    .build();
```

### Transport Configuration Options

#### Socket-based Transport (Default)
```java
// Socket UDP
TransportType.SOCKET

// Socket TCP
TransportType.SOCKET_TCP

// Socket RadSec
TransportType.SOCKET_RADSEC
```

#### Netty-based Transport (Requires Netty dependency)
```java
// Netty UDP
TransportType.NETTY

// Netty TCP
TransportType.NETTY_TCP

// Netty RadSec
TransportType.NETTY_RADSEC
```

### Advanced Transport Configuration

```java
import java.time.Duration;

// Socket TCP with advanced configuration
UniversalRadiusClient socketTcpClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.SOCKET_TCP)
    .transportConfig(new BaseTransportConfig.Builder()
        .serverAddress("radius.example.com")
        .serverPort(2083)
        .autoReconnectEnabled(true)
        .connectionTimeout(Duration.ofSeconds(30))
        .maxReconnectAttempts(3)
        .reconnectDelay(Duration.ofSeconds(5))
        .build())
    .secret("shared-secret".getBytes())
    .build();

// Netty TCP with advanced configuration
UniversalRadiusClient nettyTcpClient = UniversalRadiusClient.newBuilder()
    .transportType(TransportType.NETTY_TCP)
    .transportConfig(new BaseTransportConfig.Builder()
        .serverAddress("radius.example.com")
        .serverPort(2083)
        .autoReconnectEnabled(true)
        .connectionTimeout(Duration.ofSeconds(30))
        .maxReconnectAttempts(3)
        .reconnectDelay(Duration.ofSeconds(5))
        .build())
    .secret("shared-secret".getBytes())
    .build();
```

### Transport Selection Guidelines

- **Socket Transport**: Использует стандартные Java Socket API, подходит для большинства случаев
- **Netty Transport**: Использует Netty framework, обеспечивает лучшую производительность для высоконагруженных приложений

### Dependencies for Netty Transport

Для использования Netty транспорта добавьте зависимость в pom.xml:

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.100.Final</version>
</dependency>
```

---

## Supported Clients

### 1. UDP Client (`UdpRadiusClient`)
- **Transport**: UDP
- **Mode**: Stateless (connectionless)
- **Features**: Retransmission strategy, synchronous operations
- **Use Case**: Simple RADIUS authentication, legacy systems

### 2. TCP Client (`TcpRadiusClient`)
- **Transport**: TCP
- **Mode**: Stateful (persistent connections)
- **Features**: 
  - Synchronous and asynchronous operations
  - Connection management (keep-alive, auto-reconnect)
  - Configurable connection settings
- **Use Case**: High-performance applications, reliable connections

### 3. RadSec Client (`RadSecRadiusClient`)
- **Transport**: TLS over TCP (RadSec protocol)
- **Mode**: Stateful (persistent secure connections)
- **Features**:
  - All TCP client features
  - TLS encryption and security
  - Configurable TLS settings
- **Use Case**: Secure RADIUS communication, enterprise environments

## Quick Start

### UDP Client Example

```java
import org.aaa4j.radius.client.clients.UdpRadiusClient;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import java.net.InetSocketAddress;

UdpRadiusClient client = UdpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 1812))
    .secret("shared-secret".getBytes())
    .build();

Packet request = new AccessRequest.Builder()
    .userName("testuser")
    .build();

Packet response = client.send(request);
```

### TCP Client Example

```java
import org.aaa4j.radius.client.clients.TcpRadiusClient;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import java.net.InetSocketAddress;
import java.time.Duration;

TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .connectionConfig(
        TcpRadiusClient.ConnectionConfig.newBuilder()
            .keepAliveInterval(Duration.ofMinutes(5))
            .connectionTimeout(Duration.ofSeconds(30))
            .autoReconnectEnabled(true)
            .maxReconnectAttempts(3)
            .reconnectDelay(Duration.ofSeconds(5))
            .build()
    )
    .build();

// Connect to server
client.connect().get();

// Send request
Packet request = new AccessRequest.Builder()
    .userName("testuser")
    .build();
Packet response = client.send(request);

// Close connection
client.close().get();
```

### RadSec Client Example

```java
import org.aaa4j.radius.client.clients.RadSecRadiusClient;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import java.net.InetSocketAddress;
import java.time.Duration;

RadSecRadiusClient client = RadSecRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .enabledProtocols("TLSv1.2", "TLSv1.3")
    .connectionConfig(
        RadSecRadiusClient.ConnectionConfig.newBuilder()
            .keepAliveInterval(Duration.ofMinutes(5))
            .connectionTimeout(Duration.ofSeconds(30))
            .autoReconnectEnabled(true)
            .maxReconnectAttempts(3)
            .reconnectDelay(Duration.ofSeconds(5))
            .build()
    )
    .build();

// Connect to server
client.connect().get();

// Send request
Packet request = new AccessRequest.Builder()
    .userName("testuser")
    .build();
Packet response = client.send(request);

// Close connection
client.close().get();
```

## Asynchronous Operations

All TCP-based clients support asynchronous operations using `CompletableFuture`:

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

ExecutorService executor = Executors.newFixedThreadPool(4);

TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .executorService(executor)
    .build();

client.connect().get();

// Send request asynchronously
CompletableFuture<Packet> future = client.sendAsync(request);

future.thenAccept(response -> {
    System.out.println("Response received: " + response.getCode());
}).exceptionally(throwable -> {
    System.err.println("Error: " + throwable.getMessage());
    return null;
});

// Don't forget to close
client.close().get();
executor.shutdown();
```

## Connection Management

TCP-based clients provide comprehensive connection management:

### Connection Configuration

```java
TcpRadiusClient.ConnectionConfig config = TcpRadiusClient.ConnectionConfig.newBuilder()
    .keepAliveInterval(Duration.ofMinutes(5))        // Keep-alive interval
    .connectionTimeout(Duration.ofSeconds(30))       // Connection timeout
    .autoReconnectEnabled(true)                      // Enable auto-reconnect
    .maxReconnectAttempts(3)                         // Max reconnection attempts
    .reconnectDelay(Duration.ofSeconds(5))           // Delay between attempts
    .build();
```

### Manual Connection Control

```java
TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .build();

// Connect manually
client.connect().get();

// Check connection status
if (client.isConnected()) {
    // Send requests
}

// Reconnect if needed
if (!client.isConnected()) {
    client.reconnect().get();
}

// Disconnect manually
client.disconnect().get();
```

## TLS Configuration (RadSec)

RadSec clients support custom TLS configuration:

```java
import javax.net.ssl.SSLContext;

// Custom SSL context
SSLContext sslContext = SSLContext.getInstance("TLS");
// Configure SSL context...

RadSecRadiusClient client = RadSecRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .sslContext(sslContext)
    .enabledProtocols("TLSv1.2", "TLSv1.3")
    .enabledCipherSuites("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")
    .build();
```

## Error Handling

All clients throw `RadiusClientException` for RADIUS-specific errors:

```java
try {
    Packet response = client.send(request);
    // Handle successful response
} catch (RadiusClientException e) {
    // Handle RADIUS-specific errors
    System.err.println("RADIUS error: " + e.getMessage());
} catch (Exception e) {
    // Handle other errors
    System.err.println("General error: " + e.getMessage());
}
```

## Best Practices

1. **Resource Management**: Always close TCP-based clients when done
2. **Connection Pooling**: Use connection pooling for high-throughput applications
3. **Error Handling**: Implement proper error handling and retry logic
4. **TLS Security**: Use proper certificate validation in production RadSec deployments
5. **Async Operations**: Use async operations for better performance in multi-threaded applications

## Port Numbers

- **UDP RADIUS**: 1812 (authentication), 1813 (accounting)
- **TCP RADIUS**: 2083 (authentication), 2084 (accounting)
- **RadSec**: 2083 (authentication), 2084 (accounting)

## Thread Safety

- **UDP Client**: Thread-safe for concurrent requests
- **TCP/RadSec Clients**: Thread-safe for concurrent requests when using async operations
- **Connection Management**: Thread-safe for connection operations 