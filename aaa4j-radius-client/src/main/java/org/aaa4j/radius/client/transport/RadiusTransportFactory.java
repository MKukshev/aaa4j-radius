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
 * Фабрика для создания RADIUS транспортов различных типов.
 */
public class RadiusTransportFactory {

    /**
     * Создает транспорт указанного типа.
     *
     * @param type тип транспорта
     * @param config конфигурация транспорта
     * @param secret RADIUS секрет
     * @return созданный транспорт
     * @throws IllegalArgumentException если тип транспорта не поддерживается
     */
    public static RadiusTransport createTransport(TransportType type, TransportConfig config, byte[] secret) {
        switch (type) {
            case SOCKET:
                return new SocketRadiusTransport.Builder()
                        .config(config)
                        .secret(secret)
                        .tcp(false) // UDP для обычного Socket транспорта
                        .build();
            case SOCKET_TCP:
                return new SocketRadiusTransport.Builder()
                        .config(config)
                        .secret(secret)
                        .tcp(true)
                        .build();
            case SOCKET_RADSEC:
                return new SocketRadiusTransport.Builder()
                        .config(config)
                        .secret(secret)
                        .tcp(true)
                        .radSec(true)
                        .build();
            case NETTY:
                // Для NETTY по умолчанию используем UDP
                return new NettyRadiusTransport.Builder()
                        .config(config)
                        .secret(secret)
                        .tcp(false) // UDP по умолчанию
                        .build();
            case NETTY_TCP:
                return new NettyRadiusTransport.Builder()
                        .config(config)
                        .secret(secret)
                        .tcp(true)
                        .build();
            case NETTY_RADSEC:
                return new NettyRadiusTransport.Builder()
                        .config(config)
                        .secret(secret)
                        .tcp(true)
                        .radSec(true)
                        .build();
            default:
                throw new IllegalArgumentException("Unsupported transport type: " + type);
        }
    }

    /**
     * Создает UDP Socket транспорт.
     *
     * @param config конфигурация транспорта
     * @param secret RADIUS секрет
     * @return созданный UDP транспорт
     */
    public static RadiusTransport createUdpTransport(TransportConfig config, byte[] secret) {
        return new SocketRadiusTransport.Builder()
                .config(config)
                .secret(secret)
                .tcp(false)
                .build();
    }

    /**
     * Создает TCP Socket транспорт.
     *
     * @param config конфигурация транспорта
     * @param secret RADIUS секрет
     * @return созданный TCP транспорт
     */
    public static RadiusTransport createTcpTransport(TransportConfig config, byte[] secret) {
        return new SocketRadiusTransport.Builder()
                .config(config)
                .secret(secret)
                .tcp(true)
                .build();
    }

    /**
     * Создает RadSec Socket транспорт.
     *
     * @param config конфигурация транспорта
     * @param secret RADIUS секрет
     * @return созданный RadSec транспорт
     */
    public static RadiusTransport createRadSecTransport(TransportConfig config, byte[] secret) {
        return new SocketRadiusTransport.Builder()
                .config(config)
                .secret(secret)
                .tcp(true)
                .radSec(true)
                .build();
    }
} 