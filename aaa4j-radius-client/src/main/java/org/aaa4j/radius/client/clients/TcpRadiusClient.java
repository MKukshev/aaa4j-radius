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

import org.aaa4j.radius.core.dictionary.Dictionary;
import org.aaa4j.radius.core.packet.PacketIdGenerator;
import org.aaa4j.radius.core.util.RandomProvider;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * RADIUS-клиент, использующий TCP как транспортный уровень. Создавайте экземпляры через {@link Builder}.
 *
 * <p>Этот клиент поддерживает синхронные и асинхронные операции, управление соединением,
 * keep-alive, автоматическое переподключение и настраиваемые стратегии повторных попыток для обработки
 * сетевых сбоев.</p>
 */
public class TcpRadiusClient extends BaseTcpRadiusClient {

    private TcpRadiusClient(Builder builder) {
        super(builder);
    }

    /**
     * Создает новый объект билдера.
     *
     * @return новый объект билдера
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Билдер для {@link TcpRadiusClient}.
     */
    public final static class Builder extends BaseTcpRadiusClient.Builder<Builder> {

        /**
         * Устанавливает адрес сервера. Обязательный параметр.
         *
         * @param address адрес сервера
         * @return этот билдер
         */
        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Устанавливает общий секрет RADIUS. Обязательный параметр.
         *
         * @param secret общий секрет
         * @return этот билдер
         */
        public Builder secret(byte[] secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Устанавливает {@link Dictionary} для использования. Необязательный параметр. Если не задан, используется стандартный словарь.
         *
         * @param dictionary словарь
         * @return этот билдер
         */
        public Builder dictionary(Dictionary dictionary) {
            this.dictionary = dictionary;
            return this;
        }

        /**
         * Устанавливает {@link PacketIdGenerator} для генерации идентификаторов пакетов. Необязательный параметр. Если не задан, используется генератор по умолчанию.
         *
         * @param packetIdGenerator генератор идентификаторов
         * @return этот билдер
         */
        public Builder packetIdGenerator(PacketIdGenerator packetIdGenerator) {
            this.packetIdGenerator = packetIdGenerator;
            return this;
        }

        /**
         * Устанавливает {@link RandomProvider} для генерации случайных данных. Необязательный параметр. Если не задан, используется безопасный источник.
         *
         * @param randomProvider генератор случайных данных
         * @return этот билдер
         */
        public Builder randomProvider(RandomProvider randomProvider) {
            this.randomProvider = randomProvider;
            return this;
        }

        /**
         * Устанавливает конфигурацию соединения. Необязательный параметр. Если не задана, используются настройки по умолчанию.
         *
         * @param connectionConfig конфигурация соединения
         * @return этот билдер
         */
        public Builder connectionConfig(ConnectionConfig connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }

        /**
         * Устанавливает executor для асинхронных операций. Необязательный параметр. Если не задан, используется пул по умолчанию.
         *
         * @param executorService executor
         * @return этот билдер
         */
        public Builder executorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Возвращает новый {@link TcpRadiusClient} с заданными параметрами билдера.
         *
         * @return новый TcpRadiusClient
         */
        public TcpRadiusClient build() {
            return new TcpRadiusClient(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
} 