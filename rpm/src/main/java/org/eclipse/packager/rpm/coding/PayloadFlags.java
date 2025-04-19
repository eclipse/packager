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

package org.eclipse.packager.rpm.coding;

import java.util.Locale;
import java.util.Objects;
import java.util.zip.Deflater;

public class PayloadFlags {
    private String coding;

    private Integer level;

    private String threads;

    private Integer strategy;

    private String windowLog;

    private Boolean smallMode;

    public PayloadFlags() {

    }

    public PayloadFlags(final PayloadCoding payloadCoding) {
        this.coding = payloadCoding.name();
    }

    public PayloadFlags(final PayloadCoding payloadCoding, final Integer level) {
        this.coding = payloadCoding.name();
        this.level = level;
    }

    public PayloadFlags(final PayloadCoding payloadCoding, final Integer level, final Integer strategy) {
        this.coding = payloadCoding.name();
        this.level = level;
        this.strategy = strategy;
    }

    public PayloadFlags(final PayloadCoding payloadCoding, final Integer level, final Boolean smallMode) {
        this.coding = payloadCoding.name();
        this.level = level;
        this.smallMode = smallMode;
    }

    public PayloadFlags(final PayloadCoding payloadCoding, final Integer level, final String threads) {
        this.coding = payloadCoding.name();
        this.level = level;
        this.threads = threads;
    }

    public PayloadFlags(final PayloadCoding payloadCoding, final Integer level, final String threads, final String windowLog) {
        this.coding = payloadCoding.name();
        this.level = level;
        this.threads = threads;
        this.windowLog = windowLog;
    }

    public PayloadFlags(final PayloadCoding payloadCoding, final String flags) {
        this.coding = payloadCoding.name();

        if (flags == null || flags.isEmpty()) {
            return;
        }

        switch (payloadCoding) {
            case gzip:
                parseFlagsGzip(flags);
                break;
            case bzip2:
                parseFlagsBZip2(flags);
                break;
            case lzma:
            case xz:
                parseFlagsLZMA(flags);
                break;
            case zstd:
                parseFlagsZstd(flags);
                break;
            case none:
                break;
        }
    }

    public PayloadFlags(final String coding, final String flags) {
        this(PayloadCoding.valueOf(coding.toLowerCase(Locale.ROOT)), flags);
    }

    public String getCoding() {
        return this.coding;
    }

    public void setCoding(final String coding) {
        this.coding = coding != null ? coding.toLowerCase(Locale.ROOT) : null;
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public String getThreads() {
        return this.threads;
    }

    public void setThreads(final String threads) {
        this.threads = threads;
    }

    public Integer getStrategy() {
        return this.strategy;
    }

    public void setStrategy(final Integer strategy) {
        this.strategy = strategy;
    }

    public String getWindowLog() {
        return this.windowLog;
    }

    public void setWindowLog(final String windowLog) {
        this.windowLog = windowLog;
    }

    public Boolean getSmallMode() {
        return this.smallMode;
    }

    public void setSmallMode(final Boolean smallMode) {
        this.smallMode = smallMode;
    }

    private void parseFlagsGzip(final String flags) {
        int i = 0;

        while (i < flags.length()) {
            final char c = flags.charAt(i);

            if (Character.isDigit(c)) {
                this.level = Integer.parseInt(String.valueOf(c));
            } else if (c == 'f') {
                this.strategy = Deflater.FILTERED;
            } else if (c == 'h') {
                this.strategy = Deflater.HUFFMAN_ONLY;
            }

            i++;
        }
    }

    private void parseFlagsBZip2(final String flags) {
        int i = 0;

        while (i < flags.length()) {
            final char c = flags.charAt(i);

            if (Character.isDigit(c)) {
                level = Integer.parseInt(String.valueOf(c));
            } else if (c == 's') {
                smallMode = Boolean.TRUE;
            }

            i++;
        }
    }

    private void parseFlagsLZMA(final String flags) {
        int i = 0;

        while (i < flags.length()) {
            final char c = flags.charAt(i);

            if (Character.isDigit(c)) {
                level = Integer.parseInt(String.valueOf(c));
                i++;
            } else if (c == 'T') {
                final int start = ++i;

                while (i < flags.length() && Character.isDigit(flags.charAt(i))) {
                    i++;
                }

                threads = flags.substring(start, i);
            }
        }
    }

    private void parseFlagsZstd(final String flags) {
        int i = 0;

        while (i < flags.length()) {
            final char c = flags.charAt(i);

            if (Character.isDigit(c)) {
                final int start = i;

                while (i < flags.length() && Character.isDigit(flags.charAt(i))) {
                    i++;
                }

                level = Integer.parseInt(flags.substring(start, i));
            } else if (c == 'L') {
                final int start = ++i;

                while (i < flags.length() && Character.isDigit(flags.charAt(i))) {
                    i++;
                }

                windowLog = flags.substring(start, i);
            } else if (c == 'T') {
                final int start = ++i;

                while (i < flags.length() && Character.isDigit(flags.charAt(i))) {
                    i++;
                }

                threads = flags.substring(start, i);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PayloadFlags that = (PayloadFlags) o;
        return Objects.equals(this.coding, that.coding) && Objects.equals(this.level, that.level) && Objects.equals(this.threads, that.threads) && Objects.equals(this.strategy, that.strategy) && Objects.equals(this.windowLog, that.windowLog) && Objects.equals(this.smallMode, that.smallMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.coding, level, this.threads, this.strategy, this.windowLog, this.smallMode);
    }

    @Override
    public String toString() {
        if (this.coding == null) {
            return "";
        }

        final PayloadCoding payloadCoding = PayloadCoding.valueOf(this.coding);

        if (payloadCoding == PayloadCoding.none) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();

        if (this.level != null) {
            sb.append(this.level);
        }

        if (this.strategy != null && payloadCoding == PayloadCoding.bzip2) {
            if (this.strategy == Deflater.FILTERED) {
                sb.append('f');
            } else if (this.strategy == Deflater.HUFFMAN_ONLY) {
                sb.append('h');
            }
        }

        if (this.smallMode != null && payloadCoding == PayloadCoding.bzip2) {
            sb.append('s');
        }

        if (this.threads != null && payloadCoding != PayloadCoding.gzip && payloadCoding != PayloadCoding.bzip2) {
            sb.append('T').append(this.threads);
        }

        if (this.windowLog != null && payloadCoding == PayloadCoding.zstd) {
            sb.append('L').append(windowLog);
        }

        return sb.toString();
    }
}
