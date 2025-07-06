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
 * Интерфейс для абстракции транспорта RADIUS клиентов.
 * Позволяет использовать различные транспортные реализации (Socket, Netty, etc.)
 * с единым API для отправки RADIUS пакетов.
 */
public interface RadiusTransport {

    /**
     * Отправляет RADIUS пакет через транспорт и возвращает CompletableFuture с ответом.
     *
     * @param requestPacket пакет для отправки
     * @return CompletableFuture с ответным пакетом
     */
    CompletableFuture<Packet> send(Packet requestPacket);

    /**
     * Проверяет, подключен ли транспорт к серверу.
     *
     * @return true если подключен, false в противном случае
     */
    boolean isConnected();

    /**
     * Устанавливает соединение с сервером.
     *
     * @return CompletableFuture, который завершается при успешном подключении
     */
    CompletableFuture<Void> connect();

    /**
     * Закрывает соединение и освобождает ресурсы.
     *
     * @return CompletableFuture, который завершается при закрытии
     */
    CompletableFuture<Void> close();

    /**
     * Получает конфигурацию транспорта.
     *
     * @return конфигурация транспорта
     */
    TransportConfig getConfig();
} 