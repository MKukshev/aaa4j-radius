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
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.aaa4j.radius.client.IntervalRetransmissionStrategy;
import org.aaa4j.radius.client.clients.RadSecRadiusClient;
import org.aaa4j.radius.client.clients.TcpRadiusClient;
import org.aaa4j.radius.client.clients.UdpRadiusClient;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccessRequest;

/**
 * Examples demonstrating how to use the different RADIUS client implementations.
 */
public class ClientExamples {

    public static void main(String[] args) {
        // UDP Client Example
        udpClientExample();
        
        // TCP Client Example
        tcpClientExample();
        
        // RadSec Client Example
        radSecClientExample();
        
        // Async Client Example
        asyncClientExample();
    }

    /**
     * Example of using the UDP RADIUS client.
     */
    public static void udpClientExample() {
        System.out.println("=== UDP Client Example ===");
        
        try {
            UdpRadiusClient client = UdpRadiusClient.newBuilder()
                    .address(new InetSocketAddress("radius.example.com", 1812))
                    .secret("shared-secret".getBytes())
                    .build();

            // Create an Access-Request packet
            Packet request = new AccessRequest();

            // Send the request synchronously
            Packet response = client.send(request);
            System.out.println("UDP Response: " + response.getCode());
            
        } catch (Exception e) {
            System.err.println("UDP Client Error: " + e.getMessage());
        }
    }

    /**
     * Example of using the TCP RADIUS client with retransmission strategy.
     * This demonstrates how to configure retransmission behavior for handling network failures.
     */
    public static void tcpClientExample() {
        System.out.println("\n=== TCP Client Example ===");
        
        try {
            TcpRadiusClient client = TcpRadiusClient.newBuilder()
                    .address(new InetSocketAddress("radius.example.com", 2083))
                    .secret("shared-secret".getBytes())
                    // Configure retransmission strategy: 3 attempts with 5-second intervals
                    .retransmissionStrategy(new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5)))
                    .connectionConfig(
                            new TcpRadiusClient.ConnectionConfig.Builder()
                                    .keepAliveInterval(Duration.ofMinutes(5))
                                    .connectionTimeout(Duration.ofSeconds(30))
                                    .autoReconnectEnabled(true)
                                    .maxReconnectAttempts(3)
                                    .reconnectDelay(Duration.ofSeconds(5))
                                    .build()
                    )
                    .build();

            // Connect to the server
            client.connect().get();

            // Create an Access-Request packet
            Packet request = new AccessRequest();

            // Send the request synchronously
            Packet response = client.send(request);
            System.out.println("TCP Response: " + response.getCode());

            // Close the connection
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing TCP client: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("TCP Client Error: " + e.getMessage());
        }
    }

    /**
     * Example of using the RadSec RADIUS client with retransmission strategy.
     * This demonstrates how to configure retransmission behavior for secure TLS connections.
     */
    public static void radSecClientExample() {
        System.out.println("\n=== RadSec Client Example ===");
        
        try {
            RadSecRadiusClient client = RadSecRadiusClient.newBuilder()
                    .address(new InetSocketAddress("radius.example.com", 2083))
                    .secret("shared-secret".getBytes())
                    // Configure retransmission strategy: 3 attempts with 5-second intervals
                    .retransmissionStrategy(new IntervalRetransmissionStrategy(3, Duration.ofSeconds(5)))
                    .enabledProtocols("TLSv1.2", "TLSv1.3")
                    .connectionConfig(
                            new RadSecRadiusClient.ConnectionConfig.Builder()
                                    .keepAliveInterval(Duration.ofMinutes(5))
                                    .connectionTimeout(Duration.ofSeconds(30))
                                    .autoReconnectEnabled(true)
                                    .maxReconnectAttempts(3)
                                    .reconnectDelay(Duration.ofSeconds(5))
                                    .build()
                    )
                    .build();

            // Connect to the server
            client.connect().get();

            // Create an Access-Request packet
            Packet request = new AccessRequest();

            // Send the request synchronously
            Packet response = client.send(request);
            System.out.println("RadSec Response: " + response.getCode());

            // Close the connection
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing RadSec client: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("RadSec Client Error: " + e.getMessage());
        }
    }

    /**
     * Example of using clients asynchronously.
     */
    public static void asyncClientExample() {
        System.out.println("\n=== Async Client Example ===");
        
        try {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
            
            TcpRadiusClient client = TcpRadiusClient.newBuilder()
                    .address(new InetSocketAddress("radius.example.com", 2083))
                    .secret("shared-secret".getBytes())
                    .executorService(executor)
                    .build();

            // Connect to the server
            client.connect().get();

            // Create multiple requests
            for (int i = 0; i < 5; i++) {
                final int requestId = i;
                Packet request = new AccessRequest();

                // Send requests asynchronously
                CompletableFuture<Packet> future = client.sendAsync(request);
                
                future.thenAccept(response -> {
                    System.out.println("Async Response " + requestId + ": " + response.getCode());
                }).exceptionally(throwable -> {
                    System.err.println("Async Error " + requestId + ": " + throwable.getMessage());
                    return null;
                });
            }

            // Wait a bit for all requests to complete
            Thread.sleep(2000);

            // Close the connection
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing async client: " + e.getMessage());
            }
            executor.shutdown();
            
        } catch (Exception e) {
            System.err.println("Async Client Error: " + e.getMessage());
        }
    }
} 