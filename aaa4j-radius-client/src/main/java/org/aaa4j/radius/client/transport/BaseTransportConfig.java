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

package org.aaa4j.radius.client.transport;

import java.time.Duration;

/**
 * Базовая реализация конфигурации транспорта.
 */
public class BaseTransportConfig implements TransportConfig {

    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 3;
    private static final Duration DEFAULT_RECONNECT_DELAY = Duration.ofSeconds(5);

    private final String serverAddress;
    private final int serverPort;
    private final Duration connectionTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;
    private final int maxReconnectAttempts;
    private final Duration reconnectDelay;
    private final boolean autoReconnectEnabled;

    protected BaseTransportConfig(Builder builder) {
        this.serverAddress = builder.serverAddress;
        this.serverPort = builder.serverPort;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.writeTimeout = builder.writeTimeout;
        this.maxReconnectAttempts = builder.maxReconnectAttempts;
        this.reconnectDelay = builder.reconnectDelay;
        this.autoReconnectEnabled = builder.autoReconnectEnabled;
    }

    @Override
    public String getServerAddress() {
        return serverAddress;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public Duration getReadTimeout() {
        return readTimeout;
    }

    @Override
    public Duration getWriteTimeout() {
        return writeTimeout;
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

    /**
     * Билдер для {@link BaseTransportConfig}.
     */
    public static class Builder {
        private String serverAddress;
        private int serverPort;
        private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private Duration writeTimeout = DEFAULT_WRITE_TIMEOUT;
        private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
        private Duration reconnectDelay = DEFAULT_RECONNECT_DELAY;
        private boolean autoReconnectEnabled = true;

        public Builder serverAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder writeTimeout(Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
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

        public BaseTransportConfig build() {
            return new BaseTransportConfig(this);
        }
    }
} 