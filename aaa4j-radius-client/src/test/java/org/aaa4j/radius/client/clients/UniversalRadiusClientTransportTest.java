package org.aaa4j.radius.client.clients;

import java.time.Duration;

import org.aaa4j.radius.client.transport.BaseTransportConfig;
import org.aaa4j.radius.client.transport.TestRadiusServer;
import org.aaa4j.radius.client.transport.TransportType;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccessAccept;
import org.aaa4j.radius.core.packet.packets.AccessReject;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Интеграционные тесты UniversalRadiusClient с мок-сервером для всех типов транспорта.
 */
public class UniversalRadiusClientTransportTest {

    private static final String SECRET = "test-secret";
    private static final int UDP_PORT = 18120;
    private static final int TCP_PORT = 18121;
    private static final int RADSEC_PORT = 18122;
    private static TestRadiusServer testServer;

    @BeforeAll
    public static void setup() throws Exception {
        testServer = new TestRadiusServer(SECRET);
        testServer.startUdpServer(UDP_PORT);
        testServer.startTcpServer(TCP_PORT);
        testServer.startRadSecServer(RADSEC_PORT);
        // Даем серверам время стартовать
        Thread.sleep(1000);
    }

    @AfterAll
    public static void teardown() {
        testServer.stop();
    }

    @Test
    public void testUdpTransportAccept() throws Exception {
        testServer.resetCounters();
        testServer.setShouldAccept(true);
        
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(UDP_PORT)
                .autoReconnectEnabled(false)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET)
                .transportConfig(config)
                .secret(SECRET.getBytes())
                .build();

        Packet response = client.send(new org.aaa4j.radius.core.packet.packets.AccessRequest());
        assertEquals(AccessAccept.CODE, response.getCode());
        assertEquals(1, testServer.getAcceptCount());
        assertEquals(0, testServer.getRejectCount());
    }

    @Test
    public void testUdpTransportReject() throws Exception {
        testServer.resetCounters();
        testServer.setShouldAccept(false);
        
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(UDP_PORT)
                .autoReconnectEnabled(false)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET)
                .transportConfig(config)
                .secret(SECRET.getBytes())
                .build();

        Packet response = client.send(new org.aaa4j.radius.core.packet.packets.AccessRequest());
        assertEquals(AccessReject.CODE, response.getCode());
        assertEquals(0, testServer.getAcceptCount());
        assertEquals(1, testServer.getRejectCount());
    }

    @Test
    public void testTcpTransportAccept() throws Exception {
        testServer.resetCounters();
        testServer.setShouldAccept(true);
        
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(TCP_PORT)
                .autoReconnectEnabled(true)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET_TCP)
                .transportConfig(config)
                .secret(SECRET.getBytes())
                .build();

        Packet response = client.send(new org.aaa4j.radius.core.packet.packets.AccessRequest());
        assertEquals(AccessAccept.CODE, response.getCode());
        assertEquals(1, testServer.getAcceptCount());
        assertEquals(0, testServer.getRejectCount());
    }

    @Test
    public void testTcpTransportReject() throws Exception {
        testServer.resetCounters();
        testServer.setShouldAccept(false);
        
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(TCP_PORT)
                .autoReconnectEnabled(true)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET_TCP)
                .transportConfig(config)
                .secret(SECRET.getBytes())
                .build();

        Packet response = client.send(new org.aaa4j.radius.core.packet.packets.AccessRequest());
        assertEquals(AccessReject.CODE, response.getCode());
        assertEquals(0, testServer.getAcceptCount());
        assertEquals(1, testServer.getRejectCount());
    }

    @Test
    public void testRadSecTransportAccept() throws Exception {
        testServer.resetCounters();
        testServer.setShouldAccept(true);
        
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(RADSEC_PORT)
                .autoReconnectEnabled(true)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET_RADSEC)
                .transportConfig(config)
                .secret(SECRET.getBytes())
                .build();

        Packet response = client.send(new org.aaa4j.radius.core.packet.packets.AccessRequest());
        assertEquals(AccessAccept.CODE, response.getCode());
        assertEquals(1, testServer.getAcceptCount());
        assertEquals(0, testServer.getRejectCount());
    }

    // Простые тесты для проверки создания клиентов
    @Test
    public void testCreateUdpClient() {
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(1812)
                .autoReconnectEnabled(false)
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET)
                .transportConfig(config)
                .secret("test-secret".getBytes())
                .build();

        assertNotNull(client);
        assertNotNull(client.getTransport());
    }

    @Test
    public void testCreateTcpClient() {
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(1812)
                .autoReconnectEnabled(true)
                .connectionTimeout(Duration.ofSeconds(5))
                .maxReconnectAttempts(3)
                .reconnectDelay(Duration.ofSeconds(1))
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET_TCP)
                .transportConfig(config)
                .secret("test-secret".getBytes())
                .build();

        assertNotNull(client);
        assertNotNull(client.getTransport());
    }

    @Test
    public void testCreateRadSecClient() {
        BaseTransportConfig config = new BaseTransportConfig.Builder()
                .serverAddress("localhost")
                .serverPort(2083)
                .autoReconnectEnabled(true)
                .connectionTimeout(Duration.ofSeconds(5))
                .maxReconnectAttempts(3)
                .reconnectDelay(Duration.ofSeconds(1))
                .build();

        UniversalRadiusClient client = UniversalRadiusClient.newBuilder()
                .transportType(TransportType.SOCKET_RADSEC)
                .transportConfig(config)
                .secret("test-secret".getBytes())
                .build();

        assertNotNull(client);
        assertNotNull(client.getTransport());
    }
} 