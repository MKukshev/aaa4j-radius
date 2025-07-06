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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Manages connection lifecycle for RADIUS clients that maintain persistent connections.
 */
public interface ConnectionManager {

    /**
     * Establishes a connection to the RADIUS server.
     * 
     * @return a CompletableFuture that completes when the connection is established
     */
    CompletableFuture<Void> connect();

    /**
     * Closes the connection to the RADIUS server.
     * 
     * @return a CompletableFuture that completes when the connection is closed
     */
    CompletableFuture<Void> disconnect();

    /**
     * Checks if the connection is currently established.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Attempts to reconnect if the connection is lost.
     * 
     * @return a CompletableFuture that completes when reconnection is successful
     */
    CompletableFuture<Void> reconnect();

    /**
     * Configuration for connection management.
     */
    interface Config {
        /**
         * Gets the keep-alive interval.
         * 
         * @return the keep-alive interval, or null if keep-alive is disabled
         */
        Duration getKeepAliveInterval();

        /**
         * Gets the connection timeout.
         * 
         * @return the connection timeout
         */
        Duration getConnectionTimeout();

        /**
         * Gets the maximum number of reconnection attempts.
         * 
         * @return the maximum number of reconnection attempts
         */
        int getMaxReconnectAttempts();

        /**
         * Gets the delay between reconnection attempts.
         * 
         * @return the delay between reconnection attempts
         */
        Duration getReconnectDelay();

        /**
         * Checks if automatic reconnection is enabled.
         * 
         * @return true if automatic reconnection is enabled
         */
        boolean isAutoReconnectEnabled();
    }
} 