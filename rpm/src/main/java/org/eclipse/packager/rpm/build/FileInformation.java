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

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.packager.rpm.FileFlags;
import org.eclipse.packager.rpm.VerifyFlags;

public class FileInformation {
    private Instant timestamp = Instant.now();

    private String user = BuilderContext.DEFAULT_USER;

    private String group = BuilderContext.DEFAULT_GROUP;

    private Set<FileFlags> fileFlags = EnumSet.noneOf(FileFlags.class);

    // https://github.com/ctron/rpm-builder/issues/41
    /**
     * If null (default), this will result in a bitmask with value -1.
     */
    private Set<VerifyFlags> verifyFlags;

    private short mode = 0644;

    public void setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    @Deprecated
    public void setConfiguration(final boolean configuration) {
        if (configuration == true) {
            this.fileFlags.add(FileFlags.CONFIGURATION);
        } else {
            this.fileFlags.remove(FileFlags.CONFIGURATION);
        }
    }

    @Deprecated
    public boolean isConfiguration() {
        return this.fileFlags.contains(FileFlags.CONFIGURATION);
    }

    public void setFileFlags(final Set<FileFlags> fileFlags) {
        if (fileFlags.isEmpty()) {
            this.fileFlags = EnumSet.noneOf(FileFlags.class);
        } else {
            this.fileFlags = EnumSet.copyOf(fileFlags);
        }
    }

    public Set<FileFlags> getFileFlags() {
        return this.fileFlags;
    }

    /**
     * verify flags control the verify behaviour, i.e. <code>rpm -V ...</code>.
     * See https://github.com/ctron/rpm-builder/issues/41
     * <br>
     * The value null restores the default.
     * This will result in a bitmask with value -1, so the RPM tool will then verify everything it knows of,
     * not only what this library knows of.
     * Thus there is a subtle difference between passing <code>null</code> and passing
     * <code>EnumSet.allOf(VerifyFlags.class)</code>
     *
     * @param verifyFlags set of VerifyFlags, maybe null.
     * @since 0.15.2
     */
    public void setVerifyFlags(final Set<VerifyFlags> verifyFlags) {
        if (verifyFlags == null) {
            this.verifyFlags = null;
        } else if (verifyFlags.isEmpty()) {
            this.verifyFlags = EnumSet.noneOf(VerifyFlags.class);
        } else {
            this.verifyFlags = EnumSet.copyOf(verifyFlags);
        }
    }

    /**
     * If non-null, specifies the verification flags.
     * Null (default) means: verify everything (old behaviour).
     *
     * @return maybe null
     */
    public Set<VerifyFlags> getVerifyFlags() {
        return this.verifyFlags;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getUser() {
        return this.user;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getGroup() {
        return this.group;
    }

    public void setMode(final short mode) {
        this.mode = mode;
    }

    public short getMode() {
        return this.mode;
    }

    @Override
    public String toString() {
        return String.format("[FileInformation - user: %s, group: %s, mode: 0%04o, flags: %s]", this.user, this.group, this.mode, this.fileFlags);
    }
}
