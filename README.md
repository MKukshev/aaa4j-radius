# AAA4J-RADIUS

AAA4J-RADIUS is a comprehensive Java library for RADIUS (Remote Authentication Dial-In User Service) protocol implementation. It provides both client and server implementations with support for multiple transport protocols.

## Features

### Client Implementations

- **UDP Client** (`UdpRadiusClient`): Traditional UDP-based RADIUS client
- **TCP Client** (`TcpRadiusClient`): TCP-based RADIUS client with persistent connections
- **RadSec Client** (`RadSecRadiusClient`): TLS-secured RADIUS client (RadSec protocol)

### Key Features

- **Multiple Transport Protocols**: UDP, TCP, and TLS (RadSec)
- **Synchronous and Asynchronous Operations**: Support for both blocking and non-blocking operations
- **Connection Management**: Keep-alive, auto-reconnect, and connection pooling
- **Retransmission Strategy**: Configurable retransmission strategies for handling network failures
- **Configurable Settings**: Timeouts, retry strategies, and TLS configuration
- **Thread Safety**: Safe for concurrent use in multi-threaded applications
- **Extensible Architecture**: Easy to extend with custom implementations

## Quick Start

### UDP Client (Traditional)

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

### TCP Client (Persistent Connections)

```java
import org.aaa4j.radius.client.clients.TcpRadiusClient;
import org.aaa4j.radius.client.IntervalRetransmissionStrategy;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import java.net.InetSocketAddress;
import java.time.Duration;

TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .retransmissionStrategy(new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5)))
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
Packet response = client.send(request);

// Close connection
client.close().get();
```

### RadSec Client (TLS Secured)

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
Packet response = client.send(request);

// Close connection
client.close().get();
```

### Asynchronous Operations

All TCP-based clients support asynchronous operations:

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

## Architecture

### Client Hierarchy

```
RadiusClient (interface)
├── UdpRadiusClient
└── BaseTcpRadiusClient (abstract)
    ├── TcpRadiusClient
    └── RadSecRadiusClient
```

### Key Interfaces

- **`RadiusClient`**: Basic synchronous RADIUS client interface
- **`AsyncRadiusClient`**: Asynchronous RADIUS client interface
- **`ConnectionManager`**: Connection lifecycle management interface

## Transport Protocols

### UDP (Port 1812/1813)
- **Use Case**: Simple authentication, legacy systems
- **Features**: Stateless, retransmission strategy
- **Pros**: Simple, widely supported
- **Cons**: No connection management, unreliable

### TCP (Port 2083/2084)
- **Use Case**: High-performance applications, reliable connections
- **Features**: Persistent connections, keep-alive, auto-reconnect
- **Pros**: Reliable, connection management, async support
- **Cons**: More complex, requires connection management

### RadSec (Port 2083/2084)
- **Use Case**: Secure RADIUS communication, enterprise environments
- **Features**: TLS encryption, all TCP features
- **Pros**: Secure, reliable, enterprise-ready
- **Cons**: Most complex, requires TLS configuration

## Configuration

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

### TLS Configuration (RadSec)

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

### Retransmission Strategy

All clients support configurable retransmission strategies for handling network failures:

```java
import org.aaa4j.radius.client.IntervalRetransmissionStrategy;
import org.aaa4j.radius.client.RetransmissionStrategy;
import java.time.Duration;

// Default strategy: 3 attempts with 5-second intervals
RetransmissionStrategy defaultStrategy = new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5));

// Custom strategy: 5 attempts with exponential backoff
RetransmissionStrategy customStrategy = new RetransmissionStrategy() {
    @Override
    public int getMaxAttempts() {
        return 5;
    }
    
    @Override
    public Duration timeoutForAttempt(int attempt) {
        return Duration.ofSeconds((long) Math.pow(2, attempt)); // 1s, 2s, 4s, 8s, 16s
    }
};

// Use with any client
TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .retransmissionStrategy(customStrategy)
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
6. **Thread Safety**: TCP/RadSec clients are thread-safe for concurrent requests

## Port Numbers

- **UDP RADIUS**: 1812 (authentication), 1813 (accounting)
- **TCP RADIUS**: 2083 (authentication), 2084 (accounting)
- **RadSec**: 2083 (authentication), 2084 (accounting)

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## Examples

See the `examples` package for complete usage examples:

- `ClientExamples.java`: Comprehensive examples for all client types
- Various integration examples demonstrating real-world usage

## License

Apache License 2.0 - see LICENSE.txt for details.

## Contributing

Contributions are welcome! Please see the contributing guidelines for more information.
