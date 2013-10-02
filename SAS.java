/*
 * Tifany Yung
 * tyung1@jhu.edu
 * Assignment 3
 * October 2, 2013
 *
 * USAGE: Please place SAS.java and input file into the same directory first.
 * To compile: javac SAS.java
 * To run: java SAS SAS.class < loop.s (or loop.z)
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Class to generate a .scram file.
 */
public final class SAS {

    static final int THREE = 3;
    static final int FOUR = 4;
    static final int FIVE = 5;
    static final int SIX = 6;
    static final int SEVEN = 7;
    static final int EIGHT = 8;
    static final int MAX_OPERATIONS = 16;
    static final int MAX_DATA_WIDTH = 256;
    // A HashMap mapping OPCODES to their binary representation.
    static final HashMap<String, Integer> OPCODES =
            new HashMap<String, Integer>();

    HashMap<String, Integer> labelToOpNum;
    FileOutputStream outFile;
    Scanner fileIn;
    String line; // Current line being reading (trimmed).
    String holdLine; // Current line being readm(uncut).
    String[] lineFrags;
    String[] storeLines;
    int[] storeLineNums;

    /**
     * Default constructor.
     */
    private SAS(FileOutputStream stream, Scanner scanner) throws IOException {

        this.line = "";
        this.holdLine = "";
        this.storeLines = new String[MAX_OPERATIONS];
        this.storeLineNums = new int[MAX_OPERATIONS];
        this.labelToOpNum = new HashMap<String, Integer>();
        this.outFile = new FileOutputStream("loop.scram");
        this.fileIn = new Scanner(System.in);

        OPCODES.put("DAT", 0);
        OPCODES.put("LDA", 1);
        OPCODES.put("LDI", 2);
        OPCODES.put("STA", THREE);
        OPCODES.put("STI", FOUR);
        OPCODES.put("ADD", FIVE);
        OPCODES.put("SUB", SIX);
        OPCODES.put("JMP", SEVEN);
        OPCODES.put("JMZ", EIGHT);
    }

    /**
    * Generates .scram file from loop.s or (.z) with OPCODES and operands.
    * @param args the arguments.
    * @throws IOException the exception.
    */
    public static void main(String[] args) throws IOException {

        SAS sas = new SAS(new FileOutputStream("loop.scram"),
            new Scanner(System.in));

        int lineNum = 1; // Current number of the line being read.
        int numOperations = 0;

        //First pass.
        while (sas.fileIn.hasNext()) {

            lineNum++;
            sas.holdLine = sas.fileIn.nextLine();
            if (numOperations > MAX_OPERATIONS) {

                System.out.print("Maximum number of operations exceeds memor");
                System.out.println("y of 16 at line " + lineNum);
                System.exit(0);
            }

            // Split current line into label/instruction/operand and comments.
            sas.lineFrags = sas.holdLine.split("#", 2);

            if (!sas.lineFrags[0].isEmpty()) {

                // Save label/instruction/operand.
                sas.line = sas.lineFrags[0].trim();
                // Attempt to separate label and instruction/operand.
                sas.lineFrags = sas.line.split(":", 2);

                if (sas.lineFrags.length > 1) { // If there's a label.

                    // Check if the label was defined twice.
                    if (sas.labelToOpNum.containsKey(sas.lineFrags[0])) {

                        System.out.print("Label was repeated at line ");
                        System.out.println(lineNum);
                        System.exit(0);
                    } else {

                        sas.labelToOpNum.put(sas.lineFrags[0], numOperations);
                    }

                    if (!sas.lineFrags[1].isEmpty()) { // If more than a label.

                        sas.storeLines[numOperations] = sas.lineFrags[1].trim();
                        sas.storeLineNums[numOperations] = lineNum;
                        numOperations++;
                    }
                } else {

                    sas.storeLines[numOperations] = sas.lineFrags[0].trim();
                    sas.storeLineNums[numOperations] = lineNum;
                    numOperations++;
                }
            }
        }

        // Labels and comments have now been removed.
        // Pass two.
        for (int i = 0; i < MAX_OPERATIONS; i++) {

            sas.line = sas.storeLines[i];
            if (sas.line == null) {
                break;
            }
            sas.lineFrags = sas.line.split("\\s", 2);
            sas.outFile.write(calculateByte(sas, i));
        }
        sas.outFile.close();
    }

    /**
     * Calculates the byte to write to the .scram file.
     * @param sas the SAS instance.
     * @param ln the current line number.
     * @return the byte to write.
     */
    static byte calculateByte(SAS sas, int ln) {

        byte retByte = 0;
        boolean isData = false;
        int holdAddress = 0;
        int holdOpcodeByte = 0;

        for (int j = 0; j < sas.lineFrags.length; j++) {

            if (!sas.lineFrags[j].trim().isEmpty()) {

                if (OPCODES.containsKey(sas.lineFrags[j])) { // Is opcode.

                    holdOpcodeByte = OPCODES.get(sas.lineFrags[j]);
                    if (sas.lineFrags[j].equals("DAT")) {

                        isData = true;
                    }

                    retByte = (byte) (holdOpcodeByte << FOUR);
                } else { // Probably an address, either numeric or a label.

                    if (isNumeric(sas.lineFrags[j])) { // If it's a number.

                        holdAddress = Integer.parseInt(sas.lineFrags[j]);
                        checkAddressSize(sas, holdAddress, ln, isData);

                    } else if (sas.labelToOpNum.containsKey(sas.lineFrags[j])) {
                        // If it's a label.
                        holdAddress = sas.labelToOpNum.get(sas.lineFrags[j]);
                    } else {

                        System.out.print("Invalid address at line ");
                        System.out.println(
                            sas.labelToOpNum.get(sas.lineFrags[j]));
                        System.exit(0);
                    }

                    System.out.println(retByte);
                    System.out.println((byte) holdAddress);
                    retByte = (byte) (retByte | (byte) holdAddress);
                    System.out.println(retByte);
                }
            }
        }
        return retByte;
    }

    /**
     * Checks whether the address size is small enough; closes program if not.
     * If the address is data, the address may take a value up to 255.
     * If the address is not data, the address may take a value up to 15.
     * @param sas the SAS instance.
     * @param addr the address to check.
     * @param ln the current line number.
     * @param boolData true if the address is actually data, false otherwise.
     */
    static void checkAddressSize(SAS sas, int addr, int ln, boolean boolData) {

        if (addr > MAX_OPERATIONS && !boolData) {

            System.out.print("Address cannot exceed 15 ");
            System.out.print("at line ");
            System.out.println(sas.storeLineNums[ln]);
            System.exit(0);
        } else if (addr > MAX_DATA_WIDTH && boolData) {

            System.out.print("Data cannot exceed ");
            System.out.print("255 at line ");
            System.out.println(sas.storeLineNums[ln]);
            System.exit(0);
        }
    }

    /**
     * Checks if the String is an integer number.
     * @param s the String to be checked.
     * @return true if s is numeric, false otherwise.
     */
    static boolean isNumeric(String s) {

        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
