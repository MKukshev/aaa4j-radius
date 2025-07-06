# RetransmissionStrategy Support for TCP Clients

## Overview

Successfully added support for `RetransmissionStrategy` in TCP-based RADIUS clients (`TcpRadiusClient` and `RadSecRadiusClient`). This enhancement provides configurable retransmission behavior for handling network failures and temporary connectivity issues.

## Changes Made

### 1. BaseTcpRadiusClient Enhancements

- **Added RetransmissionStrategy field**: Integrated `RetransmissionStrategy` support in the base TCP client
- **Default strategy**: Uses `IntervalRetransmissionStrategy(3, Duration.ofSeconds(5))` as default
- **Builder support**: Added `retransmissionStrategy()` method to the builder pattern
- **Enhanced sendAsync method**: Implemented retransmission logic with connection recovery

### 2. Retransmission Logic

The enhanced `sendAsync` method now:
- Attempts up to `maxAttempts` times as defined by the strategy
- Uses `timeoutForAttempt(attempt)` for each attempt's timeout
- Automatically reconnects on connection failures
- Provides detailed error information when all attempts fail

### 3. Constructor Visibility Fix

- **Made RadiusClientException constructor public**: Fixed visibility issue for the two-parameter constructor to support proper error handling

### 4. Updated Examples

- **ClientExamples.java**: Added `RetransmissionStrategy` usage examples for both TCP and RadSec clients
- **README.md**: Added comprehensive documentation and examples

## Usage Examples

### Basic Usage with Default Strategy

```java
TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .retransmissionStrategy(new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5)))
    .build();
```

### Custom Retransmission Strategy

```java
RetransmissionStrategy exponentialBackoff = new RetransmissionStrategy() {
    @Override
    public int getMaxAttempts() {
        return 5;
    }
    
    @Override
    public Duration timeoutForAttempt(int attempt) {
        return Duration.ofSeconds((long) Math.pow(2, attempt)); // 1s, 2s, 4s, 8s, 16s
    }
};

TcpRadiusClient client = TcpRadiusClient.newBuilder()
    .address(new InetSocketAddress("radius.example.com", 2083))
    .secret("shared-secret".getBytes())
    .retransmissionStrategy(exponentialBackoff)
    .build();
```

## Benefits

1. **Improved Reliability**: TCP clients now handle temporary network failures gracefully
2. **Configurable Behavior**: Developers can customize retransmission strategies based on their needs
3. **Automatic Recovery**: Failed connections are automatically reconnected during retransmission
4. **Consistent API**: All client types (UDP, TCP, RadSec) now support retransmission strategies
5. **Backward Compatibility**: Existing code continues to work with default retransmission behavior

## Testing

- ✅ All tests pass
- ✅ Project compiles successfully
- ✅ JAR files generated correctly
- ✅ Documentation updated

## Files Modified

1. `aaa4j-radius-client/src/main/java/org/aaa4j/radius/client/clients/BaseTcpRadiusClient.java`
2. `aaa4j-radius-client/src/main/java/org/aaa4j/radius/client/RadiusClientException.java`
3. `aaa4j-radius-client/src/main/java/org/aaa4j/radius/client/examples/ClientExamples.java`
4. `README.md`

The implementation is now ready for production use with enhanced reliability and configurable retransmission behavior for TCP-based RADIUS clients. 