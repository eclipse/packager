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

package org.eclipse.packager.rpm;

import java.util.Objects;
import java.util.Optional;

import static org.eclipse.packager.rpm.RpmVersionValidator.validateEVR;
import static org.eclipse.packager.rpm.RpmVersionValidator.validateEpoch;
import static org.eclipse.packager.rpm.RpmVersionValidator.validateVersion;

public class RpmVersion implements Comparable<RpmVersion> {
    private final Optional<Integer> epoch;

    private final String version;

    private final Optional<String> release;

    public RpmVersion(final String version) {
        this(version, null);
    }

    public RpmVersion(final String version, final String release) {
        this(null, version, release);
    }

    public RpmVersion(final Integer epoch, final String version, final String release) {
        this(Optional.ofNullable(epoch), version, Optional.ofNullable(release));
    }

    public RpmVersion(final Optional<Integer> epoch, final String version, final Optional<String> release) {
        this.epoch = Objects.requireNonNull(epoch);
        this.version = Objects.requireNonNull(version);
        validateVersion(this.version);
        this.release = Objects.requireNonNull(release);
        this.release.ifPresent(RpmVersionValidator::validateRelease);
    }

    public Optional<Integer> getEpoch() {
        return this.epoch;
    }

    public String getVersion() {
        return this.version;
    }

    public Optional<String> getRelease() {
        return this.release;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        this.epoch.ifPresent(v -> sb.append(v).append(':'));

        sb.append(this.version);

        if (this.release.isPresent() && !this.release.get().isEmpty()) {
            sb.append('-').append(this.release.get());
        }

        return sb.toString();
    }

    public static RpmVersion valueOf(final String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }

        validateEVR(version);

        final String[] toks1 = version.split(":", 2);

        final String n;
        Integer epoch = null;
        if (toks1.length > 1) {
            final String epochStr = toks1[0];
            validateEpoch(epochStr);
            epoch = Integer.parseInt(epochStr);
            n = toks1[1];
        } else {
            n = toks1[0];
        }

        final String[] toks2 = n.split("-", 2);

        final String ver = toks2[0];
        final String rel = toks2.length > 1 ? toks2[1] : null;

        return new RpmVersion(epoch, ver, rel);
    }

    public static int compare(final String a, final String b) {
        if (a.equals(b)) {
            return 0;
        }

        final RpmVersionScanner scanner1 = new RpmVersionScanner(a);
        final RpmVersionScanner scanner2 = new RpmVersionScanner(b);

        while (scanner1.hasNext() || scanner2.hasNext()) {
            if (scanner1.hasNextTilde() || scanner2.hasNextTilde()) {
                if (!scanner1.hasNextTilde()) {
                    return 1;
                }

                if (!scanner2.hasNextTilde()) {
                    return -1;
                }

                scanner1.next();
                scanner2.next();
                continue;
            }

            if (scanner1.hasNextCarat() || scanner2.hasNextCarat()) {
                if (!scanner1.hasNext()) {
                    return -1;
                }

                if (!scanner2.hasNext()) {
                    return 1;
                }

                if (!scanner1.hasNextCarat()) {
                    return 1;
                }

                if (!scanner2.hasNextCarat()) {
                    return -1;
                }

                scanner1.next();
                scanner2.next();
                continue;
            }

            if (scanner1.hasNextAlpha() && scanner2.hasNextAlpha()) {
                final CharSequence one = scanner1.next();
                final CharSequence two = scanner2.next();
                final int i = CharSequence.compare(one, two);

                if (i != 0) {
                    return i < 0 ? -1 : 1;
                }
            } else {
                final boolean digit1 = scanner1.hasNextDigit();
                final boolean digit2 = scanner2.hasNextDigit();

                if (digit1 && digit2) {
                    final CharSequence one = scanner1.next();
                    final CharSequence two = scanner2.next();
                    final int oneLength = one.length();
                    final int twoLength = two.length();

                    if (oneLength > twoLength) {
                        return 1;
                    }

                    if (twoLength > oneLength) {
                        return -1;
                    }

                    final int i = CharSequence.compare(one, two);

                    if (i != 0) {
                        return i < 0 ? -1 : 1;
                    }
                } else if (digit1) {
                    return 1;
                } else if (digit2) {
                    return -1;
                } else if (scanner1.hasNext()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }

        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RpmVersion that = (RpmVersion) o;
        return Objects.equals(this.epoch, that.epoch) && Objects.equals(this.version, that.version) && Objects.equals(this.release, that.release);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.epoch, this.version, this.release);
    }

    @Override
    public int compareTo(final RpmVersion that) {
        // RPM currently treats no epoch as 0
        final int i = Integer.compare(epoch.orElse(0), that.epoch.orElse(0));

        if (i != 0) {
            return i;
        }

        final int j =  compare(this.version, that.version);

        if (j != 0) {
            return j;
        }

        if (this.release.isPresent() || that.release.isPresent()) {
            return this.release.map(rel -> that.release.map(otherRel -> compare(rel, otherRel)).orElse(1)).orElse(-1);
        }

        return 0;
    }
}
