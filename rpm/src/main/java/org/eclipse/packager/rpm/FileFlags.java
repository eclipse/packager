/*
 * Copyright (c) 2015, 2022 Contributors to the Eclipse Foundation
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

import java.util.EnumSet;

public enum FileFlags {
    CONFIGURATION(1 << 0), /* !< from %%config */
    DOC(1 << 1), /* !< from %%doc */
    ICON(1 << 2), /* !< from %%donotuse. */
    MISSINGOK(1 << 3), /* !< from %%config(missingok) */
    NOREPLACE(1 << 4), /* !< from %%config(noreplace) */
    GHOST(1 << 6), /* !< from %%ghost */
    LICENSE(1 << 7), /* !< from %%license */
    README(1 << 8), /* !< from %%readme */
    /* bits 9-10 unused */
    PUBKEY(1 << 11), /* !< from %%pubkey */
    ARTIFACT(1 << 12); /* !< from %%artifact */

    private final int value;

    FileFlags(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static EnumSet<FileFlags> decode(final int flagValue) {
        final EnumSet<FileFlags> fileFlags = EnumSet.noneOf(FileFlags.class);
        if (flagValue != 0) {
            for (final FileFlags fileFlag : FileFlags.values()) {
                if ((fileFlag.getValue() & flagValue) == fileFlag.getValue()) {
                    fileFlags.add(fileFlag);
                }
            }
        }
        return fileFlags;
    }

}
