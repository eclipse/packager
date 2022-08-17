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

package org.eclipse.packager.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A spool out target based on the local file system
 */
public class FileSystemSpoolOutTarget implements SpoolOutTarget {
    private final Path basePath;

    public FileSystemSpoolOutTarget(final Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public void spoolOut(final String fileName, final String mimeType, final IOConsumer<OutputStream> streamConsumer) throws IOException {
        final Path path = this.basePath.resolve(fileName);
        Files.createDirectories(path.getParent());
        try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(path))) {
            streamConsumer.accept(stream);
        }
    }

}
