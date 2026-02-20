/*
 * Copyright IBM Corp. and others 2025
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 */
package org.openj9.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to verify JFR recording files by calling the jfr command.
 * Reads the JFR file path from the marker file created by FindJfrFile.
 * Usage: java VerifyJfrRecording <server-directory> <command> [event-type]
 * Commands: summary, print
 */
public class VerifyJfrRecording {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: VerifyJfrRecording <server-directory> <command> [event-type]");
            System.exit(1);
        }

        String serverDir = args[0];
        String command = args[1];
        String eventType = args.length > 2 ? args[2] : null;

        // Read JFR file path from marker file
        File markerFile = new File(serverDir, ".jfr_file_path");
        String jfrFilePath = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(markerFile))) {
            jfrFilePath = reader.readLine();
        } catch (Exception e) {
            System.err.println("Could not read JFR file path from marker: " + e.getMessage());
            System.exit(1);
        }

        if (jfrFilePath == null || jfrFilePath.trim().isEmpty()) {
            System.err.println("JFR file path is empty");
            System.exit(1);
        }

        File jfrFile = new File(jfrFilePath);
        if (!jfrFile.exists()) {
            System.err.println("JFR file does not exist: " + jfrFilePath);
            System.exit(1);
        }

        // Build jfr command
        String javaHome = System.getProperty("java.home");
        String jfrExe = javaHome + File.separator + "bin" + File.separator + "jfr";

        List<String> cmdList = new ArrayList<>();
        cmdList.add(jfrExe);

        if ("summary".equals(command)) {
            cmdList.add("summary");
            cmdList.add(jfrFilePath);
        } else if ("print".equals(command)) {
            cmdList.add("print");
            cmdList.add("--xml");
            if (eventType != null) {
                cmdList.add("--events");
                cmdList.add(eventType);
            }
            cmdList.add("--stack-depth");
            cmdList.add("1");
            cmdList.add(jfrFilePath);
        } else {
            System.err.println("Unknown command: " + command);
            System.exit(1);
        }

        // Execute jfr command
        try {
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read and print output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("jfr command failed with exit code: " + exitCode);
                System.exit(exitCode);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute jfr command: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// Made with Bob
