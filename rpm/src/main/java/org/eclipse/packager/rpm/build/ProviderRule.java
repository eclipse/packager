/*
 * Copyright (c) 2015, 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.build;

import java.io.IOException;
import java.util.Objects;

public class ProviderRule<T> {
    private final Class<T> clazz;

    private final FileInformationProvider<T> provider;

    public ProviderRule(final Class<T> clazz, final FileInformationProvider<T> provider) {
        this.clazz = clazz;
        this.provider = provider;
    }

    public FileInformation run(final String targetName, final Object object, final PayloadEntryType type) throws IOException {
        Objects.requireNonNull(object);

        if (this.clazz.isAssignableFrom(object.getClass())) {
            return this.provider.provide(targetName, this.clazz.cast(object), type);
        }
        return null;
    }
}
