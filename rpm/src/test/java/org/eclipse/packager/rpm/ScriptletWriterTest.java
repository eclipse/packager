/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.deps.Dependency;
import org.eclipse.packager.rpm.deps.RpmDependencyFlags;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ScriptletWriterTest {

    @TempDir
    private Path outBase;

    @DisplayName("Test single scriptlet setter with different interpreters")
    @ParameterizedTest(name = "[{index}] {1} with \"{3}\" interpreter")
    @MethodSource({"scriptSource"})
    void singleScriptTest(ScriptletConsumer scriptConsumer, RpmTag scriptTag, RpmTag interpreterTag, String interpreter, String script, List<Dependency> expectedRequiredDependencies) throws IOException {
        final Path outFile;

        try (final RpmBuilder builder = new RpmBuilder("singleScriptTest", "1.0.0", "1", "noarch", outBase)) {
            outFile = builder.getTargetFile();
            scriptConsumer.accept(builder, interpreter, script);
            builder.build();
        }

        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
            List<Dependency> requiredDependencies = readRequiredDependencies(in);
            String resultScript = readTag(in, scriptTag);
            String resultInterpreter = readTag(in, interpreterTag);

            assertThat(requiredDependencies).containsExactlyElementsOf(expectedRequiredDependencies);
            assertThat(resultScript).isEqualTo(script);
            assertThat(resultInterpreter).isEqualTo(interpreter);
        }
    }

    @DisplayName("Test multiple scriptlet setter at once with different interpreters")
    @Test
    void combinedScriptTest() throws IOException {
        final String luaInterpreter = "<lua>";
        final String customInterpreter = "/bin/custom/interpreter";
        final String shellInterpreter = "/bin/sh";
        Path outFile;

        List<Dependency> expectedRequiredDependencies = List.of(
            new Dependency("rpmlib(BuiltinLuaScripts)", "4.2.2-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB),
            new Dependency(customInterpreter, "", RpmDependencyFlags.INTERPRETER, RpmDependencyFlags.SCRIPT_POST),
            new Dependency(shellInterpreter, "", RpmDependencyFlags.INTERPRETER, RpmDependencyFlags.SCRIPT_POSTUN),
            new Dependency(shellInterpreter, "", RpmDependencyFlags.INTERPRETER, RpmDependencyFlags.POSTTRANS),
            new Dependency("rpmlib(CompressedFileNames)", "3.0.4-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB),
            new Dependency("rpmlib(PayloadFilesHavePrefix)", "4.0-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));

        try (final RpmBuilder builder = new RpmBuilder("combinedScriptTest", "1.0.0", "1", "noarch", outBase)) {
            outFile = builder.getTargetFile();
            builder.setPreTransactionScript(luaInterpreter, "my pretransaction script");
            builder.setPreInstallationScript(luaInterpreter, "my preinstallation script");
            builder.setPostInstallationScript(customInterpreter, "my postinstallation script");
            builder.setPostRemoveScript("my postremove script");
            builder.setPostTransactionScript("my posttransaction script");
            builder.build();
        }

        try (final RpmInputStream in = new RpmInputStream(new BufferedInputStream(Files.newInputStream(outFile)))) {
            List<Dependency> requiredDependencies = readRequiredDependencies(in);
            assertThat(requiredDependencies).containsExactlyInAnyOrderElementsOf(expectedRequiredDependencies);

            assertThat(readTag(in, RpmTag.PRETRANSACTION_SCRIPT)).isEqualTo("my pretransaction script");
            assertThat(readTag(in, RpmTag.PRETRANSACTION_SCRIPT_PROG)).isEqualTo(luaInterpreter);

            assertThat(readTag(in, RpmTag.PREINSTALL_SCRIPT)).isEqualTo("my preinstallation script");
            assertThat(readTag(in, RpmTag.PREINSTALL_SCRIPT_PROG)).isEqualTo(luaInterpreter);

            assertThat(readTag(in, RpmTag.POSTINSTALL_SCRIPT)).isEqualTo("my postinstallation script");
            assertThat(readTag(in, RpmTag.POSTINSTALL_SCRIPT_PROG)).isEqualTo(customInterpreter);

            assertThat(readTag(in, RpmTag.POSTREMOVE_SCRIPT)).isEqualTo("my postremove script");
            assertThat(readTag(in, RpmTag.POSTREMOVE_SCRIPT_PROG)).isEqualTo(shellInterpreter);

            assertThat(readTag(in, RpmTag.POSTTRANSACTION_SCRIPT)).isEqualTo("my posttransaction script");
            assertThat(readTag(in, RpmTag.POSTTRANSACTION_SCRIPT_PROG)).isEqualTo(shellInterpreter);
        }
    }

    public static Stream<Arguments> scriptSource() {
        final String lua = "<lua>";
        final String shell = "/bin/sh";
        return Stream.of(
            // lua interpreter
            Arguments.of((ScriptletConsumer) RpmBuilder::setPreInstallationScript, RpmTag.PREINSTALL_SCRIPT, RpmTag.PREINSTALL_SCRIPT_PROG, lua, "my preinstall script", simpleLuaDependency()),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPostInstallationScript, RpmTag.POSTINSTALL_SCRIPT, RpmTag.POSTINSTALL_SCRIPT_PROG, lua, "my postinstall script", simpleLuaDependency()),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPreRemoveScript, RpmTag.PREREMOVE_SCRIPT, RpmTag.PREREMOVE_SCRIPT_PROG, lua, "my preremove script", simpleLuaDependency()),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPostRemoveScript, RpmTag.POSTREMOVE_SCRIPT, RpmTag.POSTREMOVE_SCRIPT_PROG, lua, "my postremove script", simpleLuaDependency()),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPreTransactionScript, RpmTag.PRETRANSACTION_SCRIPT, RpmTag.PRETRANSACTION_SCRIPT_PROG, lua, "my pretransaction script", simpleLuaDependency()),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPostTransactionScript, RpmTag.POSTTRANSACTION_SCRIPT, RpmTag.POSTTRANSACTION_SCRIPT_PROG, lua, "my posttransaction script", simpleLuaDependency()),
            Arguments.of((ScriptletConsumer) RpmBuilder::setVerifyScript, RpmTag.VERIFY_SCRIPT, RpmTag.VERIFY_SCRIPT_PROG, lua, "my verify script", simpleLuaDependency()),
            // shell interpreter
            Arguments.of((ScriptletConsumer) RpmBuilder::setPreInstallationScript, RpmTag.PREINSTALL_SCRIPT, RpmTag.PREINSTALL_SCRIPT_PROG, shell, "my preinstall script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.SCRIPT_PRE)),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPostInstallationScript, RpmTag.POSTINSTALL_SCRIPT, RpmTag.POSTINSTALL_SCRIPT_PROG, shell, "my postinstall script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.SCRIPT_POST)),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPreRemoveScript, RpmTag.PREREMOVE_SCRIPT, RpmTag.PREREMOVE_SCRIPT_PROG, shell, "my preremove script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.SCRIPT_PREUN)),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPostRemoveScript, RpmTag.POSTREMOVE_SCRIPT, RpmTag.POSTREMOVE_SCRIPT_PROG, shell, "my postremove script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.SCRIPT_POSTUN)),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPreTransactionScript, RpmTag.PRETRANSACTION_SCRIPT, RpmTag.PRETRANSACTION_SCRIPT_PROG, shell, "my pretransaction script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.PRETRANS)),
            Arguments.of((ScriptletConsumer) RpmBuilder::setPostTransactionScript, RpmTag.POSTTRANSACTION_SCRIPT, RpmTag.POSTTRANSACTION_SCRIPT_PROG, shell, "my posttransaction script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.POSTTRANS)),
            Arguments.of((ScriptletConsumer) RpmBuilder::setVerifyScript, RpmTag.VERIFY_SCRIPT, RpmTag.VERIFY_SCRIPT_PROG, shell, "my verify script", simpleInterpreterDependencyFor(shell, RpmDependencyFlags.SCRIPT_VERIFY))
        );
    }

    public static List<Dependency> simpleLuaDependency() {
        return List.of(
            new Dependency("rpmlib(BuiltinLuaScripts)", "4.2.2-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB),
            new Dependency("rpmlib(CompressedFileNames)", "3.0.4-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB),
            new Dependency("rpmlib(PayloadFilesHavePrefix)", "4.0-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
    }

    public static List<Dependency> simpleInterpreterDependencyFor(String interpreter, RpmDependencyFlags scriptletPhase) {
        return List.of(
            new Dependency(interpreter, "", RpmDependencyFlags.INTERPRETER, scriptletPhase),
            new Dependency("rpmlib(CompressedFileNames)", "3.0.4-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB),
            new Dependency("rpmlib(PayloadFilesHavePrefix)", "4.0-1", RpmDependencyFlags.LESS, RpmDependencyFlags.EQUAL, RpmDependencyFlags.RPMLIB));
    }


    @FunctionalInterface
    interface ScriptletConsumer {
        void accept(RpmBuilder rpmBuilder, String interpreter, String script);
    }

    private static List<Dependency> readRequiredDependencies(final RpmInputStream in) throws IOException {
        return readGroup(in, RpmTag.REQUIRE_NAME, RpmTag.REQUIRE_VERSION, RpmTag.REQUIRE_FLAGS);
    }

    private static String readTag(final RpmInputStream in, final RpmTag tag) throws IOException {
        return in.getPayloadHeader().getString(tag);
    }

    private static List<Dependency> readGroup(final RpmInputStream in, final RpmTag nameTag, final RpmTag versionTag, final RpmTag flagTag) throws IOException {
        final List<String> names = in.getPayloadHeader().getStringList(nameTag);
        final List<String> versions = in.getPayloadHeader().getStringList(versionTag);
        final List<Integer> flags = in.getPayloadHeader().getIntegerList(flagTag);

        List<Dependency> dependencies = new ArrayList<>(names.size());
        if (names.isEmpty()) {
            return dependencies;
        }
        for (int i = 0; i < names.size(); i++) {
            dependencies.add(new Dependency(names.get(i), versions.get(i), RpmDependencyFlags.parse(flags.get(i))));
        }
        return dependencies;
    }

}
