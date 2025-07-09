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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.aaa4j.radius.client.AsyncRadiusClient;
import org.aaa4j.radius.client.RadiusClient;
import org.aaa4j.radius.client.RadiusClientException;
import org.aaa4j.radius.client.transport.RadiusTransport;
import org.aaa4j.radius.client.transport.RadiusTransportFactory;
import org.aaa4j.radius.client.transport.TransportConfig;
import org.aaa4j.radius.client.transport.TransportType;
import org.aaa4j.radius.core.packet.Packet;

/**
 * Универсальный RADIUS клиент, поддерживающий различные типы транспорта.
 * Позволяет выбирать между Socket и Netty транспортами через builder pattern.
 * 
 * <p>Этот клиент предоставляет единый API для всех типов транспорта,
 * что упрощает переключение между ними в зависимости от требований к производительности.</p>
 */
public class UniversalRadiusClient implements RadiusClient, AsyncRadiusClient {

    private final RadiusTransport transport;
    private final byte[] secret;

    private UniversalRadiusClient(Builder builder) {
        this.secret = builder.secret;
        
        // Создаем транспорт через фабрику
        this.transport = RadiusTransportFactory.createTransport(
            builder.transportType,
            builder.transportConfig,
            builder.secret
        );
    }

    @Override
    public Packet send(Packet requestPacket) throws RadiusClientException {
        try {
            return transport.send(requestPacket).get();
        } catch (Exception e) {
            if (e instanceof RadiusClientException) {
                throw (RadiusClientException) e;
            }
            throw new RadiusClientException(e);
        }
    }

    @Override
    public CompletableFuture<Packet> sendAsync(Packet requestPacket) {
        return transport.send(requestPacket);
    }

    /**
     * Проверяет, подключен ли клиент к серверу.
     *
     * @return true если подключен, false в противном случае
     */
    public boolean isConnected() {
        return transport.isConnected();
    }

    /**
     * Устанавливает соединение с сервером.
     *
     * @return CompletableFuture, который завершается при успешном подключении
     */
    public CompletableFuture<Void> connect() {
        return transport.connect();
    }

    /**
     * Закрывает соединение и освобождает ресурсы асинхронно.
     *
     * @return CompletableFuture, который завершается при закрытии
     */
    public CompletableFuture<Void> closeAsync() {
        return transport.close();
    }

    /**
     * Закрывает соединение и освобождает ресурсы.
     *
     * @throws IOException если произошла ошибка при закрытии
     */
    @Override
    public void close() throws IOException {
        try {
            closeAsync().get();
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to close universal client", e);
        }
    }

    /**
     * Получает используемый транспорт.
     *
     * @return транспорт
     */
    public RadiusTransport getTransport() {
        return transport;
    }

    /**
     * Получает конфигурацию транспорта.
     *
     * @return конфигурация транспорта
     */
    public TransportConfig getTransportConfig() {
        return transport.getConfig();
    }

    /**
     * Создает новый билдер для {@link UniversalRadiusClient}.
     *
     * @return новый билдер
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Билдер для {@link UniversalRadiusClient}.
     */
    public static class Builder {
        private TransportType transportType = TransportType.SOCKET; // По умолчанию Socket
        private TransportConfig transportConfig;
        private byte[] secret;

        /**
         * Устанавливает тип транспорта.
         *
         * @param transportType тип транспорта
         * @return этот билдер
         */
        public Builder transportType(TransportType transportType) {
            this.transportType = transportType;
            return this;
        }

        /**
         * Устанавливает конфигурацию транспорта.
         *
         * @param transportConfig конфигурация транспорта
         * @return этот билдер
         */
        public Builder transportConfig(TransportConfig transportConfig) {
            this.transportConfig = transportConfig;
            return this;
        }

        /**
         * Устанавливает RADIUS секрет.
         *
         * @param secret RADIUS секрет
         * @return этот билдер
         */
        public Builder secret(byte[] secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Создает новый экземпляр {@link UniversalRadiusClient}.
         *
         * @return новый клиент
         * @throws IllegalStateException если не указаны обязательные параметры
         */
        public UniversalRadiusClient build() {
            if (transportConfig == null) {
                throw new IllegalStateException("Transport config is required");
            }
            if (secret == null) {
                throw new IllegalStateException("Secret is required");
            }
            return new UniversalRadiusClient(this);
        }
    }
} 