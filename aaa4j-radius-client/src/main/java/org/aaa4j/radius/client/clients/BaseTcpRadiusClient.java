/*
 * Copyright 2020 The AAA4J-RADIUS Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aaa4j.radius.client.clients;

import org.aaa4j.radius.client.AsyncRadiusClient;
import org.aaa4j.radius.client.ConnectionManager;
import org.aaa4j.radius.client.RadiusClient;
import org.aaa4j.radius.client.RadiusClientException;
import org.aaa4j.radius.client.RetransmissionStrategy;
import org.aaa4j.radius.client.IntervalRetransmissionStrategy;
import org.aaa4j.radius.core.dictionary.Dictionary;
import org.aaa4j.radius.core.dictionary.dictionaries.StandardDictionary;
import org.aaa4j.radius.core.packet.IncrementingPacketIdGenerator;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.PacketCodec;
import org.aaa4j.radius.core.packet.PacketCodecException;
import org.aaa4j.radius.core.packet.PacketIdGenerator;
import org.aaa4j.radius.core.util.RandomProvider;
import org.aaa4j.radius.core.util.SecureRandomProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Базовый абстрактный TCP-клиент для RADIUS. Предоставляет общую логику управления соединением, поддержки асинхронных запросов,
 * повторных попыток (retransmission), keep-alive и автоматического переподключения. Используется как основа для TcpRadiusClient и RadSecRadiusClient.
 */
public abstract class BaseTcpRadiusClient implements RadiusClient, AsyncRadiusClient, ConnectionManager {

    private static final int MAX_PACKET_SIZE = 4096;
    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_KEEP_ALIVE_INTERVAL = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 3;
    private static final Duration DEFAULT_RECONNECT_DELAY = Duration.ofSeconds(5);
    /**
     * Default retransmission strategy for TCP clients.
     * Uses 3 attempts with 5-second intervals between attempts.
     */
    private static final RetransmissionStrategy DEFAULT_RETRANSMISSION_STRATEGY = new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5));

    protected final InetSocketAddress address;
    protected final byte[] secret;
    protected final Dictionary dictionary;
    protected final PacketCodec packetCodec;
    protected final RandomProvider randomProvider;
    protected final ConnectionConfig connectionConfig;
    /**
     * Strategy for handling retransmissions when network failures occur.
     * Determines the number of attempts and timeout for each attempt.
     */
    protected final RetransmissionStrategy retransmissionStrategy;
    protected final ScheduledExecutorService executorService;

    protected Socket socket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected final AtomicBoolean connected = new AtomicBoolean(false);
    protected final AtomicBoolean closing = new AtomicBoolean(false);
    protected final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    protected BaseTcpRadiusClient(Builder<?> builder) {
        this.address = Objects.requireNonNull(builder.address);
        this.secret = Objects.requireNonNull(builder.secret);
        this.dictionary = builder.dictionary == null ? new StandardDictionary() : builder.dictionary;
        this.randomProvider = builder.randomProvider == null ? new SecureRandomProvider() : builder.randomProvider;
        
        PacketIdGenerator packetIdGenerator = builder.packetIdGenerator == null 
            ? new IncrementingPacketIdGenerator(0) 
            : builder.packetIdGenerator;
        
        this.packetCodec = new PacketCodec(dictionary, randomProvider, packetIdGenerator);
        this.connectionConfig = builder.connectionConfig == null 
            ? new ConnectionConfig.Builder().build() 
            : builder.connectionConfig;
        // Initialize retransmission strategy with default if not provided
        this.retransmissionStrategy = builder.retransmissionStrategy == null
            ? DEFAULT_RETRANSMISSION_STRATEGY
            : builder.retransmissionStrategy;
        this.executorService = builder.executorService == null 
            ? Executors.newScheduledThreadPool(4) 
            : (ScheduledExecutorService) builder.executorService;
    }

    @Override
    public Packet send(Packet requestPacket) throws RadiusClientException {
        try {
            // Calculate total timeout based on retransmission strategy
            Duration totalTimeout = calculateTotalTimeout();
            return sendAsync(requestPacket).get(totalTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RadiusClientException("Запрос превысил таймаут после " + calculateTotalTimeout());
        } catch (Exception e) {
            if (e instanceof RadiusClientException) {
                throw (RadiusClientException) e;
            }
            throw new RadiusClientException(e);
        }
    }

    @Override
    public CompletableFuture<Packet> sendAsync(Packet requestPacket) {
        return sendAsync(requestPacket, calculateTotalTimeout());
    }

    /**
     * Отправляет RADIUS-пакет асинхронно с заданным таймаутом.
     *
     * @param requestPacket пакет для отправки
     * @param timeout общий таймаут на весь запрос
     * @return CompletableFuture с ответным пакетом
     */
    public CompletableFuture<Packet> sendAsync(Packet requestPacket, Duration timeout) {
        CompletableFuture<Packet> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Ensure connection is established before sending
                if (!isConnected()) {
                    try {
                        connect().get();
                    } catch (Exception e) {
                        throw new RuntimeException(new RadiusClientException(e));
                    }
                }
                
                // Generate authenticator for request
                byte[] authenticatorBytes = new byte[16];
                randomProvider.nextBytes(authenticatorBytes);
                
                // Encode the request packet
                byte[] outBytes;
                try {
                    outBytes = packetCodec.encodeRequest(requestPacket, secret, authenticatorBytes);
                } catch (PacketCodecException e) {
                    throw new RuntimeException(new RadiusClientException(e));
                }
                
                // Get retransmission strategy parameters
                int maxAttempts = retransmissionStrategy.getMaxAttempts();
                Exception lastException = null;
                
                // Attempt to send the packet with retransmission logic
                for (int attempt = 0; attempt < maxAttempts; attempt++) {
                    try {
                        // Get timeout for current attempt from strategy
                        Duration timeoutDuration = retransmissionStrategy.timeoutForAttempt(attempt);
                        
                        // Set socket timeout for this attempt
                        if (socket != null && !socket.isClosed()) {
                            socket.setSoTimeout(Math.toIntExact(timeoutDuration.toMillis()));
                        }
                        
                        // Send packet length first (4 bytes, big-endian)
                        outputStream.write((outBytes.length >> 24) & 0xFF);
                        outputStream.write((outBytes.length >> 16) & 0xFF);
                        outputStream.write((outBytes.length >> 8) & 0xFF);
                        outputStream.write(outBytes.length & 0xFF);
                        
                        // Send packet data
                        outputStream.write(outBytes);
                        outputStream.flush();
                        
                        // Read response length (4 bytes, big-endian)
                        int responseLength = 0;
                        for (int i = 0; i < 4; i++) {
                            int b = inputStream.read();
                            if (b == -1) {
                                throw new IOException("Соединение закрыто сервером");
                            }
                            responseLength = (responseLength << 8) | b;
                        }
                        
                        // Validate response packet size
                        if (responseLength > MAX_PACKET_SIZE) {
                            throw new IOException("Ответный пакет слишком большой: " + responseLength);
                        }
                        
                        // Read complete response data
                        byte[] inBytes = new byte[responseLength];
                        int bytesRead = 0;
                        while (bytesRead < responseLength) {
                            int count = inputStream.read(inBytes, bytesRead, responseLength - bytesRead);
                            if (count == -1) {
                                throw new IOException("Соединение закрыто сервером");
                            }
                            bytesRead += count;
                        }
                        
                        // Decode and return the response
                        try {
                            return packetCodec.decodeResponse(inBytes, secret, authenticatorBytes);
                        } catch (PacketCodecException e) {
                            throw new IOException("Не удалось декодировать ответ", e);
                        }
                        
                    } catch (IOException e) {
                        // Store the exception for final error reporting
                        lastException = e;
                        
                        // If this is not the last attempt, try to reconnect and continue
                        if (attempt < maxAttempts - 1) {
                            try {
                                // Close current connection to force reconnection
                                if (socket != null && !socket.isClosed()) {
                                    socket.close();
                                }
                                connected.set(false);
                                
                                // Attempt to establish new connection
                                connect().get();
                            } catch (Exception reconnectException) {
                                // If reconnection fails, continue to next attempt
                                // The connection will be attempted again in the next loop iteration
                                continue;
                            }
                        }
                    }
                }
                
                // If we get here, all transmission attempts have failed
                if (lastException != null) {
                    throw new RuntimeException(new RadiusClientException("Все попытки передачи не увенчались успехом: " + lastException.getMessage(), lastException));
                } else {
                    throw new RuntimeException(new RadiusClientException("Все попытки передачи не увенчались успехом"));
                }
                
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw e;
                }
                throw new RuntimeException(new RadiusClientException(e));
            }
        }, executorService);
        
        // Apply timeout to the future using a separate thread
        CompletableFuture<Packet> timeoutFuture = new CompletableFuture<>();
        
        // Schedule timeout
        executorService.schedule(() -> {
            if (!future.isDone()) {
                timeoutFuture.completeExceptionally(
                    new RadiusClientException("Запрос превысил таймаут после " + timeout)
                );
            }
        }, timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // Handle completion
        future.thenAccept(timeoutFuture::complete)
              .exceptionally(throwable -> {
                  timeoutFuture.completeExceptionally(throwable);
                  return null;
              });
        
        return timeoutFuture;
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (isConnected()) {
                    return;
                }
                
                socket = createSocket();
                socket.setSoTimeout(Math.toIntExact(connectionConfig.getConnectionTimeout().toMillis()));
                socket.connect(address, Math.toIntExact(connectionConfig.getConnectionTimeout().toMillis()));
                
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                connected.set(true);
                reconnectAttempts.set(0);
                
                if (connectionConfig.getKeepAliveInterval() != null) {
                    startKeepAlive();
                }
                
            } catch (IOException e) {
                throw new RuntimeException(new RadiusClientException(e));
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                closing.set(true);
                connected.set(false);
                
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                
            } catch (IOException e) {
                // Ignore errors during disconnect
            }
        }, executorService);
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Вычисляет общий таймаут для запроса на основе стратегии повторных попыток.
     * Включает все таймауты попыток и небольшой запас на накладные расходы.
     *
     * @return итоговый таймаут
     */
    protected Duration calculateTotalTimeout() {
        int maxAttempts = retransmissionStrategy.getMaxAttempts();
        Duration totalTimeout = Duration.ZERO;
        
        // Sum up all attempt timeouts
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            totalTimeout = totalTimeout.plus(retransmissionStrategy.timeoutForAttempt(attempt));
        }
        
        // Add buffer for connection overhead and processing time
        // This accounts for connection establishment, reconnection delays, and processing overhead
        Duration overhead = Duration.ofSeconds(5);
        
        return totalTimeout.plus(overhead);
    }

    @Override
    public CompletableFuture<Void> reconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (reconnectAttempts.get() >= connectionConfig.getMaxReconnectAttempts()) {
                    throw new RuntimeException(new RadiusClientException("Превышено максимальное количество попыток переподключения"));
                }
                
                disconnect().get();
                Thread.sleep(connectionConfig.getReconnectDelay().toMillis());
                
                reconnectAttempts.incrementAndGet();
                connect().get();
                
            } catch (Exception e) {
                throw new RuntimeException(new RadiusClientException(e));
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> close() {
        return disconnect().thenRun(() -> {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        });
    }

    /**
     * Создает новый экземпляр сокета. Подклассы могут переопределить этот метод для предоставления пользовательского создания сокета.
     * 
     * @return новый экземпляр Socket
     * @throws IOException если создание сокета не удалось
     */
    protected Socket createSocket() throws IOException {
        return new Socket();
    }

    /**
     * Запускает механизм keep-alive.
     */
    protected void startKeepAlive() {
        CompletableFuture.runAsync(() -> {
            while (isConnected() && !closing.get()) {
                try {
                    Thread.sleep(connectionConfig.getKeepAliveInterval().toMillis());
                    
                    if (isConnected() && !closing.get()) {
                        // Send a keep-alive packet or ping
                        // For now, we'll just check if the socket is still alive
                        if (socket.getInputStream().available() == -1) {
                            // Connection lost, try to reconnect
                            if (connectionConfig.isAutoReconnectEnabled()) {
                                reconnect();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Connection lost, try to reconnect
                    if (connectionConfig.isAutoReconnectEnabled()) {
                        reconnect();
                    }
                }
            }
        }, executorService);
    }

    /**
     * Конфигурация управления соединением.
     */
    public static class ConnectionConfig implements ConnectionManager.Config {
        private final Duration keepAliveInterval;
        private final Duration connectionTimeout;
        private final int maxReconnectAttempts;
        private final Duration reconnectDelay;
        private final boolean autoReconnectEnabled;

        private ConnectionConfig(Builder builder) {
            this.keepAliveInterval = builder.keepAliveInterval;
            this.connectionTimeout = builder.connectionTimeout;
            this.maxReconnectAttempts = builder.maxReconnectAttempts;
            this.reconnectDelay = builder.reconnectDelay;
            this.autoReconnectEnabled = builder.autoReconnectEnabled;
        }

        @Override
        public Duration getKeepAliveInterval() {
            return keepAliveInterval;
        }

        @Override
        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        @Override
        public int getMaxReconnectAttempts() {
            return maxReconnectAttempts;
        }

        @Override
        public Duration getReconnectDelay() {
            return reconnectDelay;
        }

        @Override
        public boolean isAutoReconnectEnabled() {
            return autoReconnectEnabled;
        }

        public static class Builder {
            private Duration keepAliveInterval = DEFAULT_KEEP_ALIVE_INTERVAL;
            private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
            private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
            private Duration reconnectDelay = DEFAULT_RECONNECT_DELAY;
            private boolean autoReconnectEnabled = true;

            public Builder keepAliveInterval(Duration keepAliveInterval) {
                this.keepAliveInterval = keepAliveInterval;
                return this;
            }

            public Builder connectionTimeout(Duration connectionTimeout) {
                this.connectionTimeout = connectionTimeout;
                return this;
            }

            public Builder maxReconnectAttempts(int maxReconnectAttempts) {
                this.maxReconnectAttempts = maxReconnectAttempts;
                return this;
            }

            public Builder reconnectDelay(Duration reconnectDelay) {
                this.reconnectDelay = reconnectDelay;
                return this;
            }

            public Builder autoReconnectEnabled(boolean autoReconnectEnabled) {
                this.autoReconnectEnabled = autoReconnectEnabled;
                return this;
            }

            public ConnectionConfig build() {
                return new ConnectionConfig(this);
            }
        }
    }

    /**
     * Интерфейс билдера для TCP-клиентов.
     */
    public abstract static class Builder<T extends Builder<T>> {
        protected InetSocketAddress address;
        protected byte[] secret;
        protected Dictionary dictionary;
        protected PacketIdGenerator packetIdGenerator;
        protected RandomProvider randomProvider;
        protected ConnectionConfig connectionConfig;
        /**
         * Strategy for handling retransmissions when network failures occur.
         * If not set, a default strategy will be used.
         */
        protected RetransmissionStrategy retransmissionStrategy;
        protected ScheduledExecutorService executorService;

        public T address(InetSocketAddress address) {
            this.address = address;
            return self();
        }

        public T secret(byte[] secret) {
            this.secret = secret;
            return self();
        }

        public T dictionary(Dictionary dictionary) {
            this.dictionary = dictionary;
            return self();
        }

        public T packetIdGenerator(PacketIdGenerator packetIdGenerator) {
            this.packetIdGenerator = packetIdGenerator;
            return self();
        }

        public T randomProvider(RandomProvider randomProvider) {
            this.randomProvider = randomProvider;
            return self();
        }

        public T connectionConfig(ConnectionConfig connectionConfig) {
            this.connectionConfig = connectionConfig;
            return self();
        }

        /**
         * Sets the retransmission strategy for handling network failures.
         * The strategy determines how many times to retry and what timeout to use for each attempt.
         * If not set, a default strategy will be used (3 attempts with 5-second intervals).
         *
         * @param retransmissionStrategy the retransmission strategy to use
         * @return this builder for method chaining
         */
        public T retransmissionStrategy(RetransmissionStrategy retransmissionStrategy) {
            this.retransmissionStrategy = retransmissionStrategy;
            return self();
        }

        public T executorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return self();
        }

        protected abstract T self();
    }
} 