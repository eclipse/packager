/*
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.packager.rpm;

import org.bouncycastle.bcpg.HashAlgorithmTags;

public enum HashAlgorithm {
    SHA1(HashAlgorithmTags.SHA1, "sha"),
    SHA256(HashAlgorithmTags.SHA256, "sha256"),
    SHA512(HashAlgorithmTags.SHA512, "sha512");

    private final int value;

    private final String id;

    HashAlgorithm(final int value, final String id) {
        this.value = value;
        this.id = id;
    }

    public int getValue() {
        return this.value;
    }

    public String getId() {
        return this.id;
    }

    /**
     * Get a hash algorithm from a string
     * <p>
     * This method will return the hash algorithm as specified by the
     * parameter "name". If this parameter is {@code null} or an empty
     * string, then the default algorithm {@link #SHA256} will be returned. If
     * algorithm is an invalid name, then an exception is thrown.
     * </p>
     *
     * @param name the name of hash algorithm, or {@code null}
     * @return a hash algorithm
     * @throws IllegalArgumentException if the name was provided, but is invalid
     */
    public static HashAlgorithm from(final String name) {
        if (name == null || name.isEmpty()) {
            return SHA256;
        }

        return HashAlgorithm.valueOf(name);
    }
}
