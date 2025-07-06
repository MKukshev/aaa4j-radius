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

import org.aaa4j.radius.client.RadiusClient;
import org.aaa4j.radius.client.RadiusClientException;
import org.aaa4j.radius.client.clients.TcpRadiusClient;
import org.aaa4j.radius.client.clients.UdpRadiusClient;
import org.aaa4j.radius.client.clients.RadSecRadiusClient;
import org.aaa4j.radius.core.packet.Packet;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Socket-based транспорт для RADIUS клиентов.
 * Использует стандартные Java Socket API.
 */
public class SocketRadiusTransport implements RadiusTransport {

    private final TransportConfig config;
    private final RadiusClient client;
    private final boolean isTcp;
    private final boolean isRadSec;

    private SocketRadiusTransport(Builder builder) {
        this.config = builder.config;
        this.isTcp = builder.isTcp;
        this.isRadSec = builder.isRadSec;
        
        InetSocketAddress address = new InetSocketAddress(config.getServerAddress(), config.getServerPort());
        
        if (isRadSec) {
            this.client = RadSecRadiusClient.newBuilder()
                    .address(address)
                    .secret(builder.secret)
                    .connectionConfig(new RadSecRadiusClient.ConnectionConfig.Builder()
                            .connectionTimeout(config.getConnectionTimeout())
                            .maxReconnectAttempts(config.getMaxReconnectAttempts())
                            .reconnectDelay(config.getReconnectDelay())
                            .autoReconnectEnabled(config.isAutoReconnectEnabled())
                            .build())
                    .build();
        } else if (isTcp) {
            this.client = TcpRadiusClient.newBuilder()
                    .address(address)
                    .secret(builder.secret)
                    .connectionConfig(new TcpRadiusClient.ConnectionConfig.Builder()
                            .connectionTimeout(config.getConnectionTimeout())
                            .maxReconnectAttempts(config.getMaxReconnectAttempts())
                            .reconnectDelay(config.getReconnectDelay())
                            .autoReconnectEnabled(config.isAutoReconnectEnabled())
                            .build())
                    .build();
        } else {
            this.client = UdpRadiusClient.newBuilder()
                    .address(address)
                    .secret(builder.secret)
                    .build();
        }
    }

    @Override
    public CompletableFuture<Packet> send(Packet requestPacket) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.send(requestPacket);
            } catch (RadiusClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean isConnected() {
        if ((isTcp || isRadSec) && client instanceof org.aaa4j.radius.client.ConnectionManager) {
            return ((org.aaa4j.radius.client.ConnectionManager) client).isConnected();
        }
        return true; // UDP всегда "подключен"
    }

    @Override
    public CompletableFuture<Void> connect() {
        if ((isTcp || isRadSec) && client instanceof org.aaa4j.radius.client.ConnectionManager) {
            return ((org.aaa4j.radius.client.ConnectionManager) client).connect();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        if ((isTcp || isRadSec) && client instanceof org.aaa4j.radius.client.ConnectionManager) {
            return ((org.aaa4j.radius.client.ConnectionManager) client).disconnect();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public TransportConfig getConfig() {
        return config;
    }

    /**
     * Билдер для {@link SocketRadiusTransport}.
     */
    public static class Builder {
        private TransportConfig config;
        private byte[] secret;
        private boolean isTcp = true; // По умолчанию TCP
        private boolean isRadSec = false; // По умолчанию не RadSec

        public Builder config(TransportConfig config) {
            this.config = config;
            return this;
        }

        public Builder secret(byte[] secret) {
            this.secret = secret;
            return this;
        }

        public Builder tcp(boolean tcp) {
            this.isTcp = tcp;
            return this;
        }

        public Builder radSec(boolean radSec) {
            this.isRadSec = radSec;
            return this;
        }

        public SocketRadiusTransport build() {
            return new SocketRadiusTransport(this);
        }
    }
} 