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

package org.aaa4j.radius.client.examples;

import java.io.IOException;
import java.time.Duration;

import org.aaa4j.radius.client.clients.UniversalRadiusClient;
import org.aaa4j.radius.client.transport.BaseTransportConfig;
import org.aaa4j.radius.client.transport.TransportType;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccessRequest;

/**
 * Примеры использования универсального RADIUS клиента с различными транспортами.
 */
public class UniversalClientExamples {

    public static void main(String[] args) {
        // Пример 1: Socket TCP транспорт (по умолчанию)
        socketTcpExample();

        // Пример 2: Socket UDP транспорт
        socketUdpExample();

        // Пример 3: Netty транспорт (требует зависимости Netty)
        nettyTransportExample();

        // Пример 4: Настройка производительности
        performanceTunedExample();
    }

    /**
     * Пример использования Socket TCP транспорта.
     */
    public static void socketTcpExample() {
        System.out.println("=== Socket TCP Transport Example ===");

        // Создаем конфигурацию транспорта
        BaseTransportConfig transportConfig = new BaseTransportConfig.Builder()
                .serverAddress("radius.example.com")
                .serverPort(1812)
                .connectionTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .maxReconnectAttempts(3)
                .reconnectDelay(Duration.ofSeconds(5))
                .autoReconnectEnabled(true)
                .build();

        // Создаем универсальный клиент с Socket TCP транспортом
        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET) // По умолчанию TCP
                .transportConfig(transportConfig)
                .secret("secret".getBytes())
                .build();

        try {
            // Создаем Access-Request пакет
            Packet request = new AccessRequest();

            // Отправляем синхронно
            Packet response = client.send(request);
            System.out.println("Received response: " + response.getCode());

            // Отправляем асинхронно
            client.sendAsync(request)
                    .thenAccept(resp -> System.out.println("Async response: " + resp.getCode()))
                    .exceptionally(throwable -> {
                        System.err.println("Async error: " + throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }

    /**
     * Пример использования Socket UDP транспорта.
     */
    public static void socketUdpExample() {
        System.out.println("\n=== Socket UDP Transport Example ===");

        BaseTransportConfig transportConfig = new BaseTransportConfig.Builder()
                .serverAddress("radius.example.com")
                .serverPort(1812)
                .connectionTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .autoReconnectEnabled(false) // UDP не требует переподключения
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET)
                .transportConfig(transportConfig)
                .secret("secret".getBytes())
                .build();

        try {
            Packet request = new AccessRequest();

            Packet response = client.send(request);
            System.out.println("UDP response: " + response.getCode());

        } catch (Exception e) {
            System.err.println("UDP error: " + e.getMessage());
        }
    }

    /**
     * Пример использования Netty транспорта (требует зависимости Netty).
     */
    public static void nettyTransportExample() {
        System.out.println("\n=== Netty Transport Example ===");

        BaseTransportConfig transportConfig = new BaseTransportConfig.Builder()
                .serverAddress("radius.example.com")
                .serverPort(1812)
                .connectionTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(15))
                .writeTimeout(Duration.ofSeconds(15))
                .maxReconnectAttempts(5)
                .reconnectDelay(Duration.ofSeconds(2))
                .autoReconnectEnabled(true)
                .build();

        try {
            UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                    .transportType(TransportType.NETTY)
                    .transportConfig(transportConfig)
                    .secret("secret".getBytes())
                    .build();

            Packet request = new AccessRequest();

            Packet response = client.send(request);
            System.out.println("Netty response: " + response.getCode());

        } catch (UnsupportedOperationException e) {
            System.out.println("Netty transport not available: " + e.getMessage());
            System.out.println("Add netty-all dependency to use Netty transport");
        } catch (Exception e) {
            System.err.println("Netty error: " + e.getMessage());
        }
    }

    /**
     * Пример настройки для высокой производительности.
     */
    public static void performanceTunedExample() {
        System.out.println("\n=== Performance Tuned Example ===");

        // Конфигурация для высокой нагрузки
        BaseTransportConfig transportConfig = new BaseTransportConfig.Builder()
                .serverAddress("radius.example.com")
                .serverPort(1812)
                .connectionTimeout(Duration.ofSeconds(3)) // Быстрое подключение
                .readTimeout(Duration.ofSeconds(10)) // Короткий таймаут чтения
                .writeTimeout(Duration.ofSeconds(5)) // Короткий таймаут записи
                .maxReconnectAttempts(10) // Больше попыток переподключения
                .reconnectDelay(Duration.ofMillis(100)) // Быстрое переподключение
                .autoReconnectEnabled(true)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET) // Или NETTY для еще большей производительности
                .transportConfig(transportConfig)
                .secret("secret".getBytes())
                .build();

        try {
            // Массовая отправка запросов
            for (int i = 0; i < 100; i++) {
                final int requestId = i;
                Packet request = new AccessRequest();

                // Асинхронная отправка для максимальной производительности
                client.sendAsync(request)
                        .thenAccept(response -> System.out.println("Response " + requestId + ": " + response.getCode()))
                        .exceptionally(throwable -> {
                            System.err.println("Error " + requestId + ": " + throwable.getMessage());
                            return null;
                        });
            }

            // Ждем завершения всех запросов
            Thread.sleep(5000);

        } catch (Exception e) {
            System.err.println("Performance test error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }
} 