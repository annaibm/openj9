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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Utility to find JFR recording files in Liberty server directory.
 * Searches for .jfr files and writes the path to a marker file.
 * Usage: java FindJfrFile <server-directory>
 */
public class FindJfrFile {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: FindJfrFile <server-directory>");
            System.exit(1);
        }

        File serverDir = new File(args[0]);
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            System.err.println("Server directory does not exist: " + serverDir);
            System.exit(1);
        }

        File jfrFile = findJfrFile(serverDir);
        if (jfrFile != null) {
            System.out.println("JFR file found: " + jfrFile.getAbsolutePath());

            // Write the path to a marker file for subsequent tests
            try {
                File markerFile = new File(serverDir, ".jfr_file_path");
                try (PrintWriter writer = new PrintWriter(new FileWriter(markerFile))) {
                    writer.println(jfrFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not write marker file: " + e.getMessage());
            }
        } else {
            System.err.println("No JFR file found in: " + serverDir);
            System.exit(1);
        }
    }

    private static File findJfrFile(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        // First check current directory for .jfr files
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".jfr")) {
                return file;
            }
        }

        // Recursively search subdirectories
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findJfrFile(file);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}

// Made with Bob
