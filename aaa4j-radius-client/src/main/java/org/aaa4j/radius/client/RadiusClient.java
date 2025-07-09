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

import java.io.Closeable;
import java.io.IOException;

import org.aaa4j.radius.core.packet.Packet;

/**
 * A RADIUS client sends RADIUS request packets and receives RADIUS response packets.
 * All RADIUS clients implement Closeable to ensure proper resource cleanup.
 */
public interface RadiusClient extends Closeable {

    /**
     * Sends a RADIUS request packet.
     *
     * @param requestPacket the request packet to send
     * 
     * @return a RADIUS response packet
     */
    Packet send(Packet requestPacket) throws RadiusClientException;

    /**
     * Closes the client and releases any resources.
     * This method should be called when the client is no longer needed.
     *
     * @throws IOException if an I/O error occurs during closing
     */
    @Override
    void close() throws IOException;
}
