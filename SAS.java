import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/*
 * Tifany Yung
 * tyung1@jhu.edu
 * Assignment 3
 * October 2, 2013
 * USAGE
 * To compile: javac SAS.java
 * To run: java SAS SAS.class < loop.s (or loop.z)
 */

/**
 * Class to generate a .scram file.
 *
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

    /**
    * Generates .scram file from loop.s or (.z) with opcodesToByte and operands.
    * @param args the arguments.
    * @throws IOException the exception.
    */
    public static void main(String[] args) throws IOException {

        // A FileOutputStream to write the loop.scram file.
        FileOutputStream outFile = new FileOutputStream("loop.scram");
        // A Scanner to read input from loop.s or loop.z.
        Scanner fileIn = new Scanner(System.in);

        String line = ""; // Current line being reading (trimmed).
        String holdLine = ""; // Current line being readm(uncut).
        String[] lineFrags;
        String[] storeLines = new String[MAX_OPERATIONS];
        int[] storeLineNums = new int[MAX_OPERATIONS];

        boolean isData = false;
        int lineNum = 1; // Current number of the line being read.
        int numOperations = 0;
        int holdAddress = 0;
        int holdOpcodeByte = 0;
        byte opByte = 0;

        // A HashMap mapping opcodesToByte to their binary representation.
        HashMap<String, Integer> opcodesToByte = new HashMap<String, Integer>();

        opcodesToByte.put("DAT", 0);
        opcodesToByte.put("LDA", 1);
        opcodesToByte.put("LDI", 2);
        opcodesToByte.put("STA", THREE);
        opcodesToByte.put("STI", FOUR);
        opcodesToByte.put("ADD", FIVE);
        opcodesToByte.put("SUB", SIX);
        opcodesToByte.put("JMP", SEVEN);
        opcodesToByte.put("JMZ", EIGHT);

        HashMap<String, Integer> labelToOpNum = new HashMap<String, Integer>();

        //First pass.
        while (fileIn.hasNext()) {

            lineNum++;
            holdLine = fileIn.nextLine();
            if (numOperations > MAX_OPERATIONS) {

                System.out.print("Maximum number of operations exceeds memor");
                System.out.println("y of 16 at line " + lineNum);
                System.exit(0);
            }

            // Split current line into label/instruction/operand and comments.
            lineFrags = holdLine.split("#", 2);

            if (!lineFrags[0].isEmpty()) {

                line = lineFrags[0].trim(); // Save label/instruction/operand.
                // Attempt to separate label and instruction/operand.
                lineFrags = line.split(":", 2);

                if (lineFrags.length > 1) { // If there's a label.

                    // Check if the label was defined twice.
                    if (labelToOpNum.containsKey(lineFrags[0])) {

                        System.out.print("Label was repeated at line ");
                        System.out.println(lineNum);
                        System.exit(0);
                    } else {

                        labelToOpNum.put(lineFrags[0], numOperations);
                    }

                    if (!lineFrags[1].isEmpty()) { // If more than a label.

                        storeLines[numOperations] = lineFrags[1].trim();
                        storeLineNums[numOperations] = lineNum;
                        numOperations++;
                    }
                } else {

                    storeLines[numOperations] = lineFrags[0].trim();
                    storeLineNums[numOperations] = lineNum;
                    numOperations++;
                }
            }
        }

        // Labels and comments have now been removed.
        // Pass two.
        for (int i = 0; i < MAX_OPERATIONS; i++) {

            line = storeLines[i];
            if (line == null) {
                break;
            }
            lineFrags = line.split("\t", 2);
            System.out.println(lineFrags[0]);
            //System.out.println(lineFrags[1]);

            for (int j = 0; j < lineFrags.length; j++) {

                if (!lineFrags[j].trim().isEmpty()) {

                    if (opcodesToByte.containsKey(lineFrags[j])) { // Is opcode.

                        holdOpcodeByte = opcodesToByte.get(lineFrags[j]);
                        if (lineFrags[j].equals("DAT")) {

                            isData = true;
                        }

                        opByte = (byte) (holdOpcodeByte << FOUR);
                    } else { // Probably an address, either numeric or a label.

                        if (isNumeric(lineFrags[j])) { // If it's a number.

                            holdAddress = Integer.parseInt(lineFrags[j]);

                            if (holdAddress > MAX_OPERATIONS && !isData) {

                                System.out.print("Address cannot exceed 15 ");
                                System.out.print("at line ");
                                System.out.println(storeLineNums[i]);
                                System.exit(0);
                            } else if (holdAddress > MAX_DATA_WIDTH
                                && isData) {

                                System.out.print("Data cannot exceed ");
                                System.out.print("255 at line ");
                                System.out.println(storeLineNums[i]);
                                System.exit(0);
                            }
                        } else if (labelToOpNum.containsKey(lineFrags[j])) {
                            // If it's a label.
                            holdAddress = labelToOpNum.get(lineFrags[j]);
                        } else {

                            System.out.print("Invalid address at line ");
                            System.out.println(labelToOpNum.get(lineFrags[j]));
                            System.exit(0);
                        }

                        opByte = (byte) (opByte | (byte) holdAddress);
                    }
                }
            }
            outFile.write(opByte);
        }
        outFile.close();
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
