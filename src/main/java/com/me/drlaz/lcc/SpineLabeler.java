/*
 * Copyright (c) 2025. Andrew J. Lazarus. All rights reserved.
 */

package com.me.drlaz.lcc;

import picocli.CommandLine;

import javax.swing.*;
import java.awt.FileDialog;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Main and only class of project */
@CommandLine.Command(mixinStandardHelpOptions = true,
        version = "1.0",
        sortSynopsis = false,
        sortOptions = false,
        name = "SpineLabeler")
public class SpineLabeler implements Runnable {

    /** Matches all LCC formats (including some with minor errors) as I found them from LibraryThing */
    private static final Pattern master =
            Pattern.compile("""
                            ^(?<cl>[A-Z]{1,3})\
                            (?<topic>[0-9.]*)[^.]?\
                            (?<ctr>(?:[ ]?[.]?[A-Z][0-9]+)*)\
                            (?:\\s+(?<yr>\\d{4}[a-z]?)\\s*(?<xtra>.*))?$""");

    /** Matches LCCs with two Cutter numbers, including examples like `QA241.9L32X99` where `X99` should have
     *  been the author: a space is missing before it*/
    private static final Pattern doubleCutter =
             Pattern.compile("^(?<dot>.)+(?<subtop>[A-Z]\\d+)\\s*(?<ctr>\\.?[A-Z]\\d+)$");

    @CommandLine.Option(names = "-d",
            description = "Draft mode? Output to console",
            defaultValue = "false",
            order = 2)
    boolean draftMode; ///< Draft mode: readable output goes to System.out console
    @CommandLine.Option(names = {"-a", "--author"},
            description = "Author column, 0-based, default ${DEFAULT-VALUE}",
            defaultValue = "3",
            order = 3)
    int authorColumn;
    @CommandLine.Option(names = {"-y", "--year"},
            description = "Year column, 0-based, default ${DEFAULT-VALUE}",
            order = 4,
            defaultValue = "8")
    int yearColumn;
    @CommandLine.Option(names = {"-c", "--lcc"},
            description = "LCC column, 0-based, default ${DEFAULT-VALUE}",
            order = 5,
            defaultValue = "32")
    int lccColumn;
    @CommandLine.Option(names = {"-f", "--input"},
            description = "Path to input tsv file, omit for File chooser",
            order = 10,
            paramLabel = "<inputFile>")
    String fileName;
    @CommandLine.Option(names = {"-o", "--output"},
            description = "Path to output tsv file, omit for File chooser",
            order = 20,
            paramLabel = "<outputFile>")
    String outputFileName;

    /** Using <A href="https://picocli.info/">picocli</A>, main is just a stub */
    public static void main(String[] args) {
        new CommandLine(new SpineLabeler()).execute(args);
    }

    /**
     * Main routine. Open files. Loop thru input file: parse LCC, output fields.
     */
    public void run() {
        if (fileName == null) fileName = obtainInputFile();
        if (outputFileName == null && !draftMode) outputFileName = obtainOutputFile();
        try (var bf = new BufferedInputStream(new FileInputStream(fileName));
             Scanner scanner = new Scanner(bf);
             PrintWriter outFileWriter = draftMode ? new PrintWriter(System.out) :
                                         new PrintWriter (outputFileName)) {
            scanner.nextLine(); // skip headers
            if (!draftMode) outFileWriter.println("CL\tTOP\tCTR\tYR\tXT\tTP8"); // make headers
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] split = line.split("\t");
                String author = split[authorColumn];
                String dbYear = split[yearColumn];
                String lcc = split[lccColumn];
                Matcher matcher = master.matcher(lcc);
                boolean success = matcher.matches();
                if (!success) {
                    System.err.printf("Failed: %s %s%n", split[1], lcc);
                    continue;
                }
                String lccYear = matcher.group("yr");
                dbYear = lccYear != null ? lccYear.trim() : dbYear;
                final String klass = matcher.group("cl");
                String topic = matcher.group("topic");
                String cutter = matcher.group("ctr");
                String xtra = Optional.ofNullable(matcher.group("xtra")).orElse("");
                String topic8 = ""; //< only for very long topics, condensed font in MS Word template

                Matcher matcher1 = doubleCutter.matcher(cutter);
                if (matcher1.matches()) {
                    topic += matcher1.group("dot") + matcher1.group("subtop");
                    cutter = matcher1.group("ctr");
                }
                cutter = cutter.isEmpty() ? (draftMode ? author : "") : cutter.replaceAll("[ .]", "");
                if (draftMode) {
                    outFileWriter.printf("%20s\t%5s\t%-10s\t%10s\t%6s\t%10s%n",
                            lcc, klass, topic, cutter, dbYear, xtra);
                } else {
                    // don't want a newline in the topic.subtopic.more_subtopic field
                    // topic8 maps to a condensed font in the MS Word template
                    if (topic.length() > 7) {
                        topic8 = topic;
                        topic = "";
                    }
                    outFileWriter.printf("%s\t%s\t%s\t%s\t%s\t%s%n", klass, topic, cutter, dbYear, xtra,
                            topic8);
                }
            }
            outFileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e); // will cause abort
        }
        System.exit(0);
    }

    /** \note We exit on Cancel in either file dialog */
    private static String getFileName(FileDialog fileDialog) {
        fileDialog.setVisible(true);
        fileDialog.dispose();
        if (fileDialog.getFile() == null) {
            // Alert and exit
            JOptionPane.showMessageDialog(null, "Run canceled", "Alert",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(1);
        }
        return fileDialog.getDirectory() + "/" + fileDialog.getFile();
    }

    private static String obtainInputFile() {
        FileDialog fileDialog = new FileDialog(new JFrame(), "Select Input File", FileDialog.LOAD);
        return getFileName(fileDialog);
    }

    private static String obtainOutputFile() {
        FileDialog fileDialog = new FileDialog(new JFrame(), "Select Output File", FileDialog.SAVE);
        return getFileName(fileDialog);
    }
}