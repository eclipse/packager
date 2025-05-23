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

package org.eclipse.packager.rpm.app;

import static org.eclipse.packager.rpm.Rpms.IMMUTABLE_TAG_HEADER;
import static org.eclipse.packager.rpm.Rpms.IMMUTABLE_TAG_SIGNATURE;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.eclipse.packager.rpm.Architecture;
import org.eclipse.packager.rpm.OperatingSystem;
import org.eclipse.packager.rpm.RpmBaseTag;
import org.eclipse.packager.rpm.RpmLead;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.Type;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.eclipse.packager.rpm.parse.HeaderValue;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;

public class Dumper {
    private static final boolean SKIP_META = Boolean.getBoolean("skipMeta");

    private static final boolean SKIP_SIGNATURES = Boolean.getBoolean("skipSignatures");

    private static final boolean SKIP_HEADERS = Boolean.getBoolean("skipHeaders");

    private static final boolean SKIP_PAYLOAD = Boolean.getBoolean("skipPayload");

    private static final boolean SORTED = Boolean.getBoolean("sorted");

    public static String dumpFlag(final int value, final IntFunction<Optional<?>> func) {
        final Optional<?> flag = func.apply(value);
        return flag.map(o -> String.format("%s (%s)", o, value)).orElseGet(() -> String.format("%s", value));
    }

    public static void dumpAll(final RpmInputStream in) throws IOException {
        final RpmLead lead = in.getLead();

        if (!SKIP_META) {
            System.out.format("Version: %s.%s%n", lead.getMajor(), lead.getMinor());
            System.out.format("Name: %s%n", lead.getName());
            System.out.format("Signature Version: %s%n", lead.getSignatureVersion());
            System.out.format("Type: %s, Arch: %s, OS: %s%n", dumpFlag(lead.getType(), Type::fromValue), dumpFlag(lead.getArchitecture(), Architecture::fromValue), dumpFlag(lead.getOperatingSystem(), OperatingSystem::fromValue));
        }

        if (!SKIP_SIGNATURES) {
            dumpHeader("Signature", in.getSignatureHeader(), RpmSignatureTag::find);
        }
        if (!SKIP_HEADERS) {
            dumpHeader("Payload", in.getPayloadHeader(), RpmTag::find);
        }

        if (!SKIP_PAYLOAD) {
            final CpioArchiveInputStream cpio = in.getCpioStream();
            CpioArchiveEntry entry;
            while ((entry = cpio.getNextEntry()) != null) {
                dumpEntry(entry);
            }
        }

        if (!SKIP_META) {
            dumpGroup(in, "Require", RpmTag.REQUIRE_NAME, RpmTag.REQUIRE_VERSION, RpmTag.REQUIRE_FLAGS);
            dumpGroup(in, "Provide", RpmTag.PROVIDE_NAME, RpmTag.PROVIDE_VERSION, RpmTag.PROVIDE_FLAGS);
            dumpGroup(in, "Conflict", RpmTag.CONFLICT_NAME, RpmTag.CONFLICT_VERSION, RpmTag.CONFLICT_FLAGS);
            dumpGroup(in, "Obsolete", RpmTag.OBSOLETE_NAME, RpmTag.OBSOLETE_VERSION, RpmTag.OBSOLETE_FLAGS);
            dumpGroup(in, "Suggest", RpmTag.SUGGEST_NAME, RpmTag.SUGGEST_VERSION, RpmTag.SUGGEST_FLAGS);
            dumpGroup(in, "Recommend", RpmTag.RECOMMEND_NAME, RpmTag.RECOMMEND_VERSION, RpmTag.RECOMMEND_FLAGS);
            dumpGroup(in, "Supplement", RpmTag.SUPPLEMENT_NAME, RpmTag.SUPPLEMENT_VERSION, RpmTag.SUPPLEMENT_FLAGS);
            dumpGroup(in, "Enhance", RpmTag.ENHANCE_NAME, RpmTag.ENHANCE_VERSION, RpmTag.ENHANCE_FLAGS);
        }

    }

    private static void dumpGroup(final RpmInputStream in, final String name, final RpmTag nameTag, final RpmTag versionTag, final RpmTag flagTag) throws IOException {
        final InputHeader<RpmTag> payloadHeader = in.getPayloadHeader();
        final List<String> names = payloadHeader.getStringList(nameTag);
        final List<String> versions = payloadHeader.getStringList(versionTag);
        final List<Integer> flags = payloadHeader.getIntegerList(flagTag);
        dumpDeps(name, names, versions, flags);
    }

    private static void dumpDeps(final String string, final List<String> names, final List<String> versions, final List<Integer> flags) {
        if (names == null) {
            return;
        }

        IntStream.range(0, names.size()).forEach(i -> System.out.format("%s: %s - %s - %s %s%n", string, names.get(i), versions.get(i), flags.get(i), RpmDependencyFlags.parse(flags.get(i))));
    }

    private static void dumpHeader(final String string, final InputHeader<? extends RpmBaseTag> header, final IntFunction<RpmBaseTag> func) {
        System.out.println(string);
        System.out.println("=================================");

        Set<Entry<Integer, HeaderValue<?>>> data;
        if (SORTED) {
            data = new TreeMap<>(header.getRawTags()).entrySet();
        } else {
            data = header.getRawTags().entrySet();
        }

        for (final Map.Entry<Integer, HeaderValue<?>> entry : data) {
            final RpmBaseTag tag = func.apply(entry.getKey());
            final HeaderValue<?> value = entry.getValue();
            System.out.format("%20s - %s%n", tag != null ? tag : entry.getKey(), Rpms.dumpValue(value));

            if (entry.getKey() == IMMUTABLE_TAG_SIGNATURE || entry.getKey() == IMMUTABLE_TAG_HEADER) {
                final ByteBuffer buf = ByteBuffer.wrap(entry.getValue().getValue().asByteArray().orElseThrow());
                System.out.format("Immutable - tag: %s, type: %s, position: %s, count: %s%n", buf.getInt(), buf.getInt(), buf.getInt(), buf.getInt());
            }
        }
    }

    private static void dumpEntry(final CpioArchiveEntry entry) {
        System.out.format("-----------------------------------%n");
        System.out.format(" %s%n", entry.getName());
        System.out.format(" Size: %s, Chksum: %016x, Align: %s, Inode: %016x, Mode: %08o, NoL: %s, Device: %s.%s%n", entry.getSize(), entry.getChksum(), entry.getAlignmentBoundary(), entry.getInode(), entry.getMode(), entry.getNumberOfLinks(), entry.getDeviceMaj(), entry.getDeviceMin());
    }

    public static void main(final String[] args) {
        for (final String file : args) {
            dump(Path.of(file));
        }
    }

    private static void dump(final Path path) {
        if (!Files.exists(path)) {
            System.err.format("%s: does not exist%n", path);
            return;
        }

        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            Dumper.dumpAll(in);
        } catch (final Exception e) {
            System.err.format("%s: failed to read file%n", path);
            e.printStackTrace(System.err);
        }
    }
}
