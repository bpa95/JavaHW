package ru.ifmo.ctddev.berdnikov.walk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class RecursiveWalk {
    private final static Charset charsetUTF8 = Charset.forName("UTF-8");
    private static Walker walker;

    static class Walker extends SimpleFileVisitor<Path> {
        private final Writer writer;

        Walker(Writer writer) {
            this.writer = writer;
        }

        private static final int X0 = 0x811c9dc5;
        private static final int FNV_32_PRIME = 0x01000193;

        private int countHash(Path file) {
            try (BufferedInputStream fileReader = new BufferedInputStream(new FileInputStream(file.toString()))) {
                int h = X0;
                int ch;
                while ((ch = fileReader.read()) != -1) {
                    h = (h * FNV_32_PRIME) ^ (ch & 0xff);
                }
                return h;
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            writer.write(String.format("%08x %s%n", countHash(file), file.toString()));
            return CONTINUE;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java RecursiveWalk <input file> <output file>");
    }

    private static void walk(Path path, Writer writer) throws IOException {
        try {
            Files.walkFileTree(path, walker);
        } catch (IOException e) {
            writer.write(String.format("%08x %s%n", 0, path.toString()));
        }
    }

    private static boolean checkArgs(String[] args) {
        if (args == null) {
            System.err.format("%s%n", "Error: No args");
            printUsage();
            return false;
        }

        if (args.length != 2) {
            printUsage();
            return false;
        }

        if (args[0] == null || args[1] == null) {
            System.err.format("%s%n", "Error: null in args");
            printUsage();
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        if (!checkArgs(args)) {
            return;
        }

        Path inputPath = Paths.get(args[0]);
            Path outputPath = Paths.get(args[1]);
            try (BufferedReader reader = Files.newBufferedReader(inputPath, charsetUTF8);
                 BufferedWriter writer = Files.newBufferedWriter(outputPath, charsetUTF8)) {
                String line;
                walker = new Walker(writer);
                while ((line = reader.readLine()) != null) {
                    Path path = Paths.get(line);
                    walk(path, writer);
                }
            } catch (NoSuchFileException e) {
                System.err.format("Error, no such file: %s%n", e.getFile());
                printUsage();
            } catch (IOException e) {
                System.err.format("IOException: %s%n", e);
                printUsage();
            }
    }
}
