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

package org.aaa4j.radius.client;

import org.aaa4j.radius.core.packet.Packet;

import java.util.concurrent.CompletableFuture;

/**
 * An asynchronous RADIUS client that sends RADIUS request packets and receives RADIUS response packets
 * asynchronously using CompletableFuture.
 */
public interface AsyncRadiusClient {

    /**
     * Sends a RADIUS request packet asynchronously.
     *
     * @param requestPacket the request packet to send
     * 
     * @return a CompletableFuture that will complete with the RADIUS response packet
     */
    CompletableFuture<Packet> sendAsync(Packet requestPacket);

    /**
     * Closes the client and releases any resources.
     * 
     * @return a CompletableFuture that completes when the client is closed
     */
    CompletableFuture<Void> close();

    /**
     * Checks if the client is connected.
     * 
     * @return true if the client is connected, false otherwise
     */
    boolean isConnected();
} 