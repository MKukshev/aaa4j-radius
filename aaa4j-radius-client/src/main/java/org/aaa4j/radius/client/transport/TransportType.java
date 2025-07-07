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

/**
 * Типы транспорта для RADIUS клиентов.
 */
public enum TransportType {

    /**
     * Socket-based UDP транспорт (стандартные Java Socket API).
     * Подходит для простых случаев и низкой нагрузки.
     */
    SOCKET,

    /**
     * Socket-based TCP транспорт.
     * Подходит для случаев, когда требуется надежная доставка.
     */
    SOCKET_TCP,

    /**
     * Socket-based RadSec транспорт (RADIUS over TLS).
     * Подходит для случаев, когда требуется шифрование.
     */
    SOCKET_RADSEC,

    /**
     * Netty-based UDP транспорт (высокопроизводительный).
     * Подходит для высокой нагрузки и множественных соединений.
     */
    NETTY,

    /**
     * Netty-based TCP транспорт (высокопроизводительный).
     * Подходит для высокой нагрузки с надежной доставкой.
     */
    NETTY_TCP,

    /**
     * Netty-based RadSec транспорт (высокопроизводительный).
     * Подходит для высокой нагрузки с шифрованием.
     */
    NETTY_RADSEC
} 