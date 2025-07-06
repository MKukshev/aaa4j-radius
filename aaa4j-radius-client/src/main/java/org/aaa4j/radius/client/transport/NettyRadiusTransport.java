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

import org.aaa4j.radius.core.packet.Packet;

import java.util.concurrent.CompletableFuture;

/**
 * Netty-based транспорт для RADIUS клиентов.
 * Высокопроизводительная реализация для высокой нагрузки.
 * 
 * <p><strong>Внимание:</strong> Для использования этого транспорта необходимо добавить
 * зависимость Netty в pom.xml:</p>
 * 
 * <pre>{@code
 * <dependency>
 *     <groupId>io.netty</groupId>
 *     <artifactId>netty-all</artifactId>
 *     <version>4.1.100.Final</version>
 * </dependency>
 * }</pre>
 * 
 * <p>Текущая реализация является заглушкой. Полная реализация будет добавлена
 * после включения зависимости Netty.</p>
 */
public class NettyRadiusTransport implements RadiusTransport {

    private final TransportConfig config;

    private NettyRadiusTransport(Builder builder) {
        this.config = builder.config;
        throw new UnsupportedOperationException(
            "Netty transport requires Netty dependency. " +
            "Add netty-all dependency to pom.xml and implement full Netty transport."
        );
    }

    @Override
    public CompletableFuture<Packet> send(Packet requestPacket) {
        throw new UnsupportedOperationException("Netty transport not implemented yet");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("Netty transport not implemented yet");
    }

    @Override
    public CompletableFuture<Void> connect() {
        throw new UnsupportedOperationException("Netty transport not implemented yet");
    }

    @Override
    public CompletableFuture<Void> close() {
        throw new UnsupportedOperationException("Netty transport not implemented yet");
    }

    @Override
    public TransportConfig getConfig() {
        return config;
    }

    /**
     * Билдер для {@link NettyRadiusTransport}.
     */
    public static class Builder {
        private TransportConfig config;

        public Builder config(TransportConfig config) {
            this.config = config;
            return this;
        }

        public NettyRadiusTransport build() {
            return new NettyRadiusTransport(this);
        }
    }
} 