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
 * Интерфейс конфигурации транспорта RADIUS клиентов.
 * Определяет общие параметры для всех типов транспорта.
 */
public interface TransportConfig {

    /**
     * Получает адрес сервера.
     *
     * @return адрес сервера
     */
    String getServerAddress();

    /**
     * Получает порт сервера.
     *
     * @return порт сервера
     */
    int getServerPort();

    /**
     * Получает таймаут соединения.
     *
     * @return таймаут соединения
     */
    Duration getConnectionTimeout();

    /**
     * Получает таймаут чтения.
     *
     * @return таймаут чтения
     */
    Duration getReadTimeout();

    /**
     * Получает таймаут записи.
     *
     * @return таймаут записи
     */
    Duration getWriteTimeout();

    /**
     * Получает максимальное количество попыток переподключения.
     *
     * @return максимальное количество попыток
     */
    int getMaxReconnectAttempts();

    /**
     * Получает задержку между попытками переподключения.
     *
     * @return задержка переподключения
     */
    Duration getReconnectDelay();

    /**
     * Проверяет, включено ли автоматическое переподключение.
     *
     * @return true если включено, false в противном случае
     */
    boolean isAutoReconnectEnabled();
} 