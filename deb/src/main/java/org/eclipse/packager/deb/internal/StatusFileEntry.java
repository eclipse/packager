/*
 * Copyright (c) 2015, 2016 Contributors to the Eclipse Foundation
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
package org.eclipse.packager.deb.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.packager.deb.FieldFormatter;

public final class StatusFileEntry {
    public static final Map<String, FieldFormatter> FORMATTERS;

    static {
        final Map<String, FieldFormatter> formatters = new HashMap<>();
        formatters.put("Description", FieldFormatter.MULTI);

        FORMATTERS = Collections.unmodifiableMap(formatters);
    }

    private StatusFileEntry() {
    }
}
