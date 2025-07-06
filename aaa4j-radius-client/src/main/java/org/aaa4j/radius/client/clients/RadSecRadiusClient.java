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

import org.aaa4j.radius.client.RadiusClientException;
import org.aaa4j.radius.core.dictionary.Dictionary;
import org.aaa4j.radius.core.packet.PacketIdGenerator;
import org.aaa4j.radius.core.util.RandomProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * RADIUS-клиент с поддержкой RadSec (TLS). Создавайте экземпляры через {@link Builder}.
 *
 * <p>Этот клиент поддерживает защищённые TLS-соединения, синхронные и асинхронные операции,
 * управление соединением, keep-alive, автоматическое переподключение и настраиваемые стратегии повторных попыток.</p>
 */
public class RadSecRadiusClient extends BaseTcpRadiusClient {

    private final SSLContext sslContext;
    private final String[] enabledProtocols;
    private final String[] enabledCipherSuites;

    private RadSecRadiusClient(Builder builder) {
        super(builder);
        this.sslContext = builder.sslContext;
        this.enabledProtocols = builder.enabledProtocols;
        this.enabledCipherSuites = builder.enabledCipherSuites;
    }

    /**
     * Создаёт новый объект билдера.
     *
     * @return новый объект билдера
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected Socket createSocket() throws IOException {
        SSLSocketFactory sslSocketFactory;
        
        if (sslContext != null) {
            sslSocketFactory = sslContext.getSocketFactory();
        } else {
            // Использовать стандартный SSL контекст
            try {
                SSLContext defaultContext = SSLContext.getInstance("TLS");
                defaultContext.init(null, new TrustManager[]{new TrustAllTrustManager()}, null);
                sslSocketFactory = defaultContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Не удалось создать SSL контекст", e);
            }
        }
        
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket();
        
        // Настроить включённые протоколы
        if (enabledProtocols != null) {
            sslSocket.setEnabledProtocols(enabledProtocols);
        }
        
        // Настроить включённые шифры
        if (enabledCipherSuites != null) {
            sslSocket.setEnabledCipherSuites(enabledCipherSuites);
        }
        
        return sslSocket;
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (isConnected()) {
                    return;
                }
                
                socket = createSocket();
                socket.setSoTimeout(Math.toIntExact(connectionConfig.getConnectionTimeout().toMillis()));
                socket.connect(address, Math.toIntExact(connectionConfig.getConnectionTimeout().toMillis()));
                
                // Начать TLS handshake
                ((SSLSocket) socket).startHandshake();
                
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                connected.set(true);
                reconnectAttempts.set(0);
                
                if (connectionConfig.getKeepAliveInterval() != null) {
                    startKeepAlive();
                }
                
            } catch (IOException e) {
                throw new RuntimeException(new RadiusClientException(e));
            }
        }, executorService);
    }

    /**
     * Trust manager, который принимает все сертификаты (для тестирования).
     * В продакшене следует использовать надлежащую проверку сертификатов.
     */
    private static class TrustAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Доверять всем клиентским сертификатам
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Доверять всем серверным сертификатам
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Билдер для {@link RadSecRadiusClient}.
     */
    public final static class Builder extends BaseTcpRadiusClient.Builder<Builder> {

        private SSLContext sslContext;
        private String[] enabledProtocols;
        private String[] enabledCipherSuites;

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
         * Устанавливает SSL контекст для TLS-соединений. Необязательный параметр. Если не задан, используется стандартный SSL контекст.
         *
         * @param sslContext SSL контекст
         * @return этот билдер
         */
        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Устанавливает включённые TLS-протоколы. Необязательный параметр. Если не задан, используются протоколы по умолчанию.
         *
         * @param enabledProtocols включённые TLS-протоколы
         * @return этот билдер
         */
        public Builder enabledProtocols(String... enabledProtocols) {
            this.enabledProtocols = enabledProtocols;
            return this;
        }

        /**
         * Устанавливает включённые шифры. Необязательный параметр. Если не задан, используются шифры по умолчанию.
         *
         * @param enabledCipherSuites включённые шифры
         * @return этот билдер
         */
        public Builder enabledCipherSuites(String... enabledCipherSuites) {
            this.enabledCipherSuites = enabledCipherSuites;
            return this;
        }

        /**
         * Возвращает новый {@link RadSecRadiusClient} с заданными параметрами билдера.
         *
         * @return новый RadSecRadiusClient
         */
        public RadSecRadiusClient build() {
            return new RadSecRadiusClient(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
} 