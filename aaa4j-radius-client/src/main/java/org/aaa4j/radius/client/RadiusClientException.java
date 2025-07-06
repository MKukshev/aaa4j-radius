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

/**
 * Exception indicating a problem when sending a RADIUS packet (or receiving a response) using the RADIUS client.
 */
public class RadiusClientException extends Exception {

    public RadiusClientException(String message) {
        super(message);
    }

    public RadiusClientException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new RadiusClientException with the specified message and cause.
     * This constructor is used for wrapping other exceptions with additional context.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public RadiusClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
