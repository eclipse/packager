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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.packager.rpm.deps.Dependency;

public interface PayloadCodingProvider {
    String getCoding();

    void fillRequirements(final Consumer<Dependency> requirementsConsumer);

    default List<Dependency> getRequirements() {
        final List<Dependency> result = new LinkedList<>();
        fillRequirements(result::add);
        return result;
    }

    InputStream createInputStream(final InputStream in) throws IOException;

    OutputStream createOutputStream(final OutputStream out, final Optional<PayloadFlags> optionalPayloadFlags) throws IOException;

}
