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

package org.eclipse.packager.rpm.coding;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.packager.rpm.deps.Dependency;

public class NullPayloadCoding implements PayloadCodingProvider {
    protected NullPayloadCoding() {
    }

    @Override
    public String getCoding() {
        return null;
    }

    @Override
    public void fillRequirements(final Consumer<Dependency> requirementsConsumer) {

    }

    @Override
    public InputStream createInputStream(final InputStream in) {
        return in;
    }

    @Override
    public OutputStream createOutputStream(final OutputStream out, final Optional<PayloadFlags> optionalPayloadFlags) {
        return out;
    }
}
