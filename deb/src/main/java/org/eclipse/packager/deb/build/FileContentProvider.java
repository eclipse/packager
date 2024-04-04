/*
 * Copyright (c) 2014, 2016 Contributors to the Eclipse Foundation
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
package org.eclipse.packager.deb.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileContentProvider implements ContentProvider {
    private final Path file;

    public FileContentProvider(final Path file) {
        this.file = file;
    }

    @Override
    public long getSize() {
        try {
            return Files.size(this.file);
        } catch (final IOException e) {
            return 0L;
        }
    }

    @Override
    public InputStream createInputStream() throws IOException {
        if (this.file == null) {
            return null;
        }

        return Files.newInputStream(this.file);
    }

    @Override
    public boolean hasContent() {
        return this.file != null;
    }

}
