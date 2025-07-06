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
import org.aaa4j.radius.core.packet.packets.AccessAccept;
import org.aaa4j.radius.core.packet.packets.AccessReject;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import org.aaa4j.radius.core.packet.PacketCodec;
import org.aaa4j.radius.core.dictionary.dictionaries.StandardDictionary;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Тестовый RADIUS сервер для тестирования транспортов.
 * Поддерживает UDP, TCP и RadSec соединения.
 */
public class TestRadiusServer {

    private final String secret;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicInteger acceptCount = new AtomicInteger(0);
    private final AtomicInteger rejectCount = new AtomicInteger(0);
    
    private TestUdpServer udpServer;
    private TestTcpServer tcpServer;
    private TestRadSecServer radSecServer;
    
    private boolean shouldAccept = true;
    private long responseDelay = 0;
    private boolean simulateError = false;

    public TestRadiusServer(String secret) {
        this.secret = secret;
    }

    /**
     * Запускает UDP сервер на указанном порту.
     */
    public void startUdpServer(int port) throws Exception {
        udpServer = new TestUdpServer(port, secret, this::handleRequest);
        udpServer.start();
    }

    /**
     * Запускает TCP сервер на указанном порту.
     */
    public void startTcpServer(int port) throws Exception {
        tcpServer = new TestTcpServer(port, secret, this::handleRequest);
        tcpServer.start();
    }

    /**
     * Запускает RadSec сервер на указанном порту.
     */
    public void startRadSecServer(int port) throws Exception {
        radSecServer = new TestRadSecServer(port, secret, this::handleRequest);
        radSecServer.start();
    }

    /**
     * Останавливает все серверы.
     */
    public void stop() {
        if (udpServer != null) {
            udpServer.stop();
        }
        if (tcpServer != null) {
            tcpServer.stop();
        }
        if (radSecServer != null) {
            radSecServer.stop();
        }
    }

    /**
     * Обработчик RADIUS запросов.
     */
    private Packet handleRequest(Packet request) {
        requestCount.incrementAndGet();
        
        // Симулируем задержку ответа
        if (responseDelay > 0) {
            try {
                Thread.sleep(responseDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Симулируем ошибку
        if (simulateError) {
            throw new RuntimeException("Simulated server error");
        }
        
        // Отвечаем в зависимости от настроек
        if (shouldAccept) {
            acceptCount.incrementAndGet();
            return new AccessAccept();
        } else {
            rejectCount.incrementAndGet();
            return new AccessReject();
        }
    }

    // Геттеры для статистики
    public int getRequestCount() { return requestCount.get(); }
    public int getAcceptCount() { return acceptCount.get(); }
    public int getRejectCount() { return rejectCount.get(); }

    // Сеттеры для настройки поведения
    public void setShouldAccept(boolean shouldAccept) { this.shouldAccept = shouldAccept; }
    public void setResponseDelay(long responseDelay) { this.responseDelay = responseDelay; }
    public void setSimulateError(boolean simulateError) { this.simulateError = simulateError; }

    /**
     * Сбрасывает счетчики.
     */
    public void resetCounters() {
        requestCount.set(0);
        acceptCount.set(0);
        rejectCount.set(0);
    }

    /**
     * Простой UDP сервер для тестирования.
     */
    private static class TestUdpServer {
        private final int port;
        private final String secret;
        private final Function<Packet, Packet> handler;
        private java.net.DatagramSocket serverSocket;
        private volatile boolean running = false;
        private final PacketCodec packetCodec;

        public TestUdpServer(int port, String secret, Function<Packet, Packet> handler) {
            this.port = port;
            this.secret = secret;
            this.handler = handler;
            this.packetCodec = new PacketCodec(new StandardDictionary());
        }

        public void start() throws Exception {
            serverSocket = new java.net.DatagramSocket(port);
            running = true;
            
            Thread serverThread = new Thread(() -> {
                while (running) {
                    try {
                        byte[] buffer = new byte[4096];
                        java.net.DatagramPacket packet = new java.net.DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);
                        
                        // Парсим RADIUS пакет
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                        
                        Packet request = packetCodec.decodeRequest(data, secret.getBytes());
                        Packet response = handler.apply(request);
                        
                        // Кодируем ответ
                        byte[] requestAuthenticator = request.getReceivedFields().getAuthenticator();
                        byte[] responseData = packetCodec.encodeResponse(response, secret.getBytes(), 
                            request.getReceivedFields().getIdentifier(), requestAuthenticator);
                        
                        java.net.DatagramPacket responsePacket = new java.net.DatagramPacket(
                            responseData, responseData.length, packet.getAddress(), packet.getPort()
                        );
                        serverSocket.send(responsePacket);
                    } catch (Exception e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        }

        public void stop() {
            running = false;
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    /**
     * Простой TCP сервер для тестирования.
     */
    private static class TestTcpServer {
        private final int port;
        private final String secret;
        private final Function<Packet, Packet> handler;
        private java.net.ServerSocket serverSocket;
        private volatile boolean running = false;
        private final PacketCodec packetCodec;

        public TestTcpServer(int port, String secret, Function<Packet, Packet> handler) {
            this.port = port;
            this.secret = secret;
            this.handler = handler;
            this.packetCodec = new PacketCodec(new StandardDictionary());
        }

        public void start() throws Exception {
            serverSocket = new java.net.ServerSocket(port);
            running = true;
            
            Thread serverThread = new Thread(() -> {
                while (running) {
                    try {
                        java.net.Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (Exception e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        }

        private void handleClient(java.net.Socket clientSocket) {
            Thread clientThread = new Thread(() -> {
                try {
                    java.io.InputStream input = clientSocket.getInputStream();
                    java.io.OutputStream output = clientSocket.getOutputStream();

                    // Читаем длину пакета (4 байта, big-endian)
                    int packetLength = 0;
                    for (int i = 0; i < 4; i++) {
                        int b = input.read();
                        if (b == -1) return;
                        packetLength = (packetLength << 8) | b;
                    }
                    
                    // Проверяем корректность длины пакета
                    if (packetLength < 20 || packetLength > 4096) return;
                    
                    // Читаем пакет
                    byte[] packetData = new byte[packetLength];
                    int bytesRead = 0;
                    while (bytesRead < packetLength) {
                        int count = input.read(packetData, bytesRead, packetLength - bytesRead);
                        if (count == -1) return;
                        bytesRead += count;
                    }

                    // Парсим RADIUS пакет
                    Packet request = packetCodec.decodeRequest(packetData, secret.getBytes());
                    Packet response = handler.apply(request);

                    // Кодируем ответ
                    byte[] requestAuthenticator = request.getReceivedFields().getAuthenticator();
                    byte[] responseData = packetCodec.encodeResponse(response, secret.getBytes(),
                        request.getReceivedFields().getIdentifier(), requestAuthenticator);

                    // TCP: отправляем length prefix (4 байта длины, big-endian) + пакет
                    output.write((responseData.length >> 24) & 0xFF);
                    output.write((responseData.length >> 16) & 0xFF);
                    output.write((responseData.length >> 8) & 0xFF);
                    output.write(responseData.length & 0xFF);
                    output.write(responseData);
                    output.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }

        public void stop() {
            running = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Простой RadSec сервер для тестирования.
     */
    private static class TestRadSecServer {
        private final int port;
        private final String secret;
        private final Function<Packet, Packet> handler;
        private javax.net.ssl.SSLServerSocket serverSocket;
        private volatile boolean running = false;
        private final PacketCodec packetCodec;

        public TestRadSecServer(int port, String secret, Function<Packet, Packet> handler) {
            this.port = port;
            this.secret = secret;
            this.handler = handler;
            this.packetCodec = new PacketCodec(new StandardDictionary());
        }

        public void start() throws Exception {
            // Создаем простой SSL контекст для тестирования
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[]{new TrustAllTrustManager()}, null);
            
            javax.net.ssl.SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            serverSocket = (javax.net.ssl.SSLServerSocket) factory.createServerSocket(port);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            
            running = true;
            
            Thread serverThread = new Thread(() -> {
                while (running) {
                    try {
                        javax.net.ssl.SSLSocket clientSocket = (javax.net.ssl.SSLSocket) serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (Exception e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
        }
        
        private void handleClient(javax.net.ssl.SSLSocket clientSocket) {
            Thread clientThread = new Thread(() -> {
                try {
                    java.io.InputStream input = clientSocket.getInputStream();
                    java.io.OutputStream output = clientSocket.getOutputStream();
                    
                    // Читаем длину пакета (4 байта, big-endian)
                    int packetLength = 0;
                    for (int i = 0; i < 4; i++) {
                        int b = input.read();
                        if (b == -1) return;
                        packetLength = (packetLength << 8) | b;
                    }
                    
                    // Проверяем корректность длины пакета
                    if (packetLength < 20 || packetLength > 4096) return;
                    
                    // Читаем пакет
                    byte[] packetData = new byte[packetLength];
                    int bytesRead = 0;
                    while (bytesRead < packetLength) {
                        int count = input.read(packetData, bytesRead, packetLength - bytesRead);
                        if (count == -1) return;
                        bytesRead += count;
                    }
                    
                    // Парсим RADIUS пакет
                    Packet request = packetCodec.decodeRequest(packetData, secret.getBytes());
                    Packet response = handler.apply(request);
                    
                    // Кодируем ответ
                    byte[] requestAuthenticator = request.getReceivedFields().getAuthenticator();
                    byte[] responseData = packetCodec.encodeResponse(response, secret.getBytes(), 
                        request.getReceivedFields().getIdentifier(), requestAuthenticator);
                    
                    // RadSec: отправляем length prefix (4 байта длины, big-endian) + пакет
                    output.write((responseData.length >> 24) & 0xFF);
                    output.write((responseData.length >> 16) & 0xFF);
                    output.write((responseData.length >> 8) & 0xFF);
                    output.write(responseData.length & 0xFF);
                    output.write(responseData);
                    output.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }

        public void stop() {
            running = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * TrustManager, который доверяет всем сертификатам (только для тестирования).
     */
    private static class TrustAllTrustManager implements javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    }
} 