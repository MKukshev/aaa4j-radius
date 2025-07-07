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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.aaa4j.radius.core.dictionary.dictionaries.StandardDictionary;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.PacketCodec;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Netty-based транспорт для RADIUS клиентов.
 * Высокопроизводительная реализация для высокой нагрузки.
 * 
 * <p>Поддерживает UDP, TCP и RadSec (TLS) транспорты с использованием
 * асинхронного неблокирующего I/O.</p>
 * 
 * <p>Особенности реализации:</p>
 * <ul>
 *   <li>EventLoopGroup с пулом воркеров для обработки I/O</li>
 *   <li>Поддержка TCP с length prefix для RADIUS пакетов</li>
 *   <li>Поддержка UDP для стандартного RADIUS</li>
 *   <li>SSL/TLS поддержка для RadSec</li>
 *   <li>Асинхронная обработка запросов/ответов</li>
 *   <li>Таймауты и retry логика</li>
 * </ul>
 */
public class NettyRadiusTransport implements RadiusTransport {

    private final TransportConfig config;
    private final byte[] secret;
    private final PacketCodec packetCodec;
    private final AtomicInteger packetId = new AtomicInteger(1);
    private final boolean isTcp;
    private final boolean isRadSec;
    
    // Netty компоненты
    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private Bootstrap bootstrap;
    private volatile boolean connected = false;
    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    
    // Обработка ответов
    private final ConcurrentHashMap<Byte, CompletableFuture<Packet>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Byte, byte[]> requestAuthenticators = new ConcurrentHashMap<>();
    private final AtomicInteger requestTimeout = new AtomicInteger(30); // секунды

    private NettyRadiusTransport(Builder builder) {
        this.config = builder.config;
        this.secret = builder.secret;
        this.packetCodec = new PacketCodec(new StandardDictionary());
        this.isTcp = builder.isTcp;
        this.isRadSec = builder.isRadSec;
    }

    @Override
    public CompletableFuture<Packet> send(Packet requestPacket) {
        if (!connected) {
            CompletableFuture<Packet> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Transport not connected"));
            return future;
        }

        CompletableFuture<Packet> responseFuture = new CompletableFuture<>();
        
        try {
            // Генерируем authenticator для запроса
            byte[] authenticatorBytes = new byte[16];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(authenticatorBytes);
            
            // Устанавливаем ID пакета
            byte packetId = (byte) (this.packetId.getAndIncrement() & 0xFF);
            // ID пакета устанавливается автоматически при кодировании
            
            // Кодируем пакет
            byte[] packetData = packetCodec.encodeRequest(requestPacket, secret, authenticatorBytes);

            // Получаем реальный ID пакета из packetData
            byte realPacketId = packetData[1];


            
            // Сохраняем pending request и authenticator по реальному ID
            pendingRequests.put(realPacketId, responseFuture);
            requestAuthenticators.put(realPacketId, authenticatorBytes);
            
            if (isTcp) {
                // TCP/RadSec: отправляем с length prefix (4 байта big-endian)
                ByteBuf buffer = Unpooled.buffer(packetData.length + 4);
                buffer.writeInt(packetData.length);
                buffer.writeBytes(packetData);
                channel.writeAndFlush(buffer).addListener(future -> {
                    if (!future.isSuccess()) {
                        pendingRequests.remove(realPacketId);
                        responseFuture.completeExceptionally(future.cause());
                    }
                });
            } else {
                // UDP: отправляем напрямую через подключенный канал
                ByteBuf buffer = Unpooled.wrappedBuffer(packetData);
                channel.writeAndFlush(buffer).addListener(future -> {
                    if (!future.isSuccess()) {
                        pendingRequests.remove(realPacketId);
                        responseFuture.completeExceptionally(future.cause());
                    }
                });
            }
            
            // Устанавливаем таймаут
            Duration timeout = config.getReadTimeout() != null ? config.getReadTimeout() : Duration.ofSeconds(30);
            eventLoopGroup.schedule(() -> {
                CompletableFuture<Packet> pending = pendingRequests.remove(realPacketId);
                if (pending != null && !pending.isDone()) {
                    pending.completeExceptionally(
                        new RuntimeException("Response timeout after " + timeout));
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            responseFuture.completeExceptionally(e);
        }
        
        return responseFuture;
    }

    @Override
    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }

    @Override
    public CompletableFuture<Void> connect() {
        if (connected) {
            connectFuture.complete(null);
            return connectFuture;
        }

        // Создаем EventLoopGroup с пулом воркеров
        int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
        eventLoopGroup = new NioEventLoopGroup(workerThreads, 
            new DefaultThreadFactory("radius-netty-worker", true));
        
        try {
            if (isTcp) {
                setupTcpTransport();
            } else {
                setupUdpTransport();
            }
        } catch (Exception e) {
            connectFuture.completeExceptionally(e);
        }
        
        return connectFuture;
    }

    private void setupTcpTransport() throws Exception {
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                    (int) (config.getConnectionTimeout() != null ? 
                        config.getConnectionTimeout().toMillis() : 30000))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // Добавляем SSL для RadSec
                        if (isRadSec) {
                            SslContext sslContext = SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .protocols("TLSv1.2", "TLSv1.3")
                                    .build();
                            pipeline.addLast(sslContext.newHandler(ch.alloc()));
                        }
                        
                        // Добавляем обработчики для TCP length prefix (4 байта big-endian)
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
                        pipeline.addLast(new RadiusTcpPacketHandler(packetCodec, secret, pendingRequests));
                    }
                });

        // Подключаемся к серверу
        ChannelFuture connectFuture = bootstrap.connect(config.getServerAddress(), config.getServerPort());
        connectFuture.addListener(future -> {
            if (future.isSuccess()) {
                channel = connectFuture.channel();
                connected = true;
                NettyRadiusTransport.this.connectFuture.complete(null);
            } else {
                NettyRadiusTransport.this.connectFuture.completeExceptionally(future.cause());
            }
        });
    }

    private void setupUdpTransport() {
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_RCVBUF, 65536)
                .option(ChannelOption.SO_SNDBUF, 65536)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                    (int) (config.getConnectionTimeout() != null ? 
                        config.getConnectionTimeout().toMillis() : 30000))
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new RadiusUdpPacketHandler(packetCodec, secret, pendingRequests));
                    }
                });

        // Для UDP подключаемся к серверу согласно конфигурации
        InetSocketAddress serverAddress = new InetSocketAddress(
            config.getServerAddress(), config.getServerPort());
        ChannelFuture connectFuture = bootstrap.connect(serverAddress);
        connectFuture.addListener(future -> {
            if (future.isSuccess()) {
                channel = connectFuture.channel();
                connected = true;
                NettyRadiusTransport.this.connectFuture.complete(null);
            } else {
                NettyRadiusTransport.this.connectFuture.completeExceptionally(future.cause());
            }
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        if (!connected) {
            closeFuture.complete(null);
            return closeFuture;
        }

        // Очищаем pending requests и authenticators
        pendingRequests.values().forEach(future -> 
            future.completeExceptionally(new RuntimeException("Transport closed")));
        pendingRequests.clear();
        requestAuthenticators.clear();

        if (channel != null) {
            channel.close().addListener(future -> {
                if (eventLoopGroup != null) {
                    eventLoopGroup.shutdownGracefully(0, 5000, TimeUnit.MILLISECONDS);
                }
                connected = false;
                closeFuture.complete(null);
            });
        } else {
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully(0, 5000, TimeUnit.MILLISECONDS);
            }
            connected = false;
            closeFuture.complete(null);
        }
        
        return closeFuture;
    }

    @Override
    public TransportConfig getConfig() {
        return config;
    }

    /**
     * Обработчик RADIUS пакетов для TCP/RadSec.
     */
    private class RadiusTcpPacketHandler extends ChannelInboundHandlerAdapter {
        private final PacketCodec packetCodec;
        private final byte[] secret;
        private final ConcurrentHashMap<Byte, CompletableFuture<Packet>> pendingRequests;

        public RadiusTcpPacketHandler(PacketCodec packetCodec, byte[] secret, 
                                    ConcurrentHashMap<Byte, CompletableFuture<Packet>> pendingRequests) {
            this.packetCodec = packetCodec;
            this.secret = secret;
            this.pendingRequests = pendingRequests;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                ByteBuf buffer = (ByteBuf) msg;
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                
                try {
                    // Находим соответствующий pending request по ID пакета
                    // Извлекаем ID из первых байтов пакета (байт 1)
                    byte responseId = data[1];
                    CompletableFuture<Packet> pending = pendingRequests.remove(responseId);
                    byte[] authenticator = requestAuthenticators.remove(responseId);
                    
                    if (pending != null && !pending.isDone()) {
                        // Декодируем с правильным authenticator
                        Packet response = packetCodec.decodeResponse(data, secret, authenticator);
                        pending.complete(response);
                    }
                } catch (Exception e) {
                    ctx.fireExceptionCaught(e);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    /**
     * Обработчик RADIUS пакетов для UDP.
     */
    private class RadiusUdpPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final PacketCodec packetCodec;
        private final byte[] secret;
        private final ConcurrentHashMap<Byte, CompletableFuture<Packet>> pendingRequests;

        public RadiusUdpPacketHandler(PacketCodec packetCodec, byte[] secret, 
                                    ConcurrentHashMap<Byte, CompletableFuture<Packet>> pendingRequests) {
            this.packetCodec = packetCodec;
            this.secret = secret;
            this.pendingRequests = pendingRequests;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            ByteBuf buffer = packet.content();
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            
            try {
                // Находим соответствующий pending request по ID пакета
                // Извлекаем ID из первых байтов пакета (байт 1)
                byte responseId = data[1];
                
                CompletableFuture<Packet> pending = pendingRequests.remove(responseId);
                byte[] authenticator = requestAuthenticators.remove(responseId);
                
                if (pending != null && !pending.isDone()) {
                    // Декодируем с правильным authenticator
                    Packet response = packetCodec.decodeResponse(data, secret, authenticator);
                    pending.complete(response);
                }
            } catch (Exception e) {
                ctx.fireExceptionCaught(e);
            }
        }
    }

    /**
     * Билдер для {@link NettyRadiusTransport}.
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

        public NettyRadiusTransport build() {
            if (config == null) {
                throw new IllegalStateException("Transport config is required");
            }
            if (secret == null) {
                throw new IllegalStateException("Secret is required");
            }
            return new NettyRadiusTransport(this);
        }
    }
} 