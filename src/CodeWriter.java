import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

/*
 * one data type: one 16-bit signed int
 * ram: 32k 16bit words:
 *      0-15: virtual registers
 *      16-255: static variables
 *      256-2047: stack
 *
 * virtual registers:
 *      SP ram[0] - holds address following address that holds top most stack value
 *      LCL ram[1] - base address of local
 *      ARG ram[2] - base address of argument segment
 *      THIS ram[3] - base address of this segment; pointer 0
 *      THAT ram[4] - base address of that segment; pointer 1
 *      TEMP ram[5-12] - holds temp segment
 *
 *      R13
 *      R14 RAM[13-15] registers for VM translator
 *      R15
 */

 /* Pushing into the stack: push constant i
 * RAM[SP] = i
 * SP++
 *      @_i_
 *      D=A
 * // RAM[SP] = D
 *      @SP
 *      A=M
 *      M=D
 * // SP++
 *      @SP /
 *      M=M+1
 */

 /* pop segment i
 * addr <- SEG + I
 * SP--
 * RAM[addr] <- RAM[SP]
 *
 * @SEG
 * D=M
 * @I
 * A=D+A
 * D=A // D = addr
 *
 * @r13
 * M=D // r13 = addr
 *
 * // SP--
 * @SP
 * M=M-1
 *
 * A=M
 * D=M // D=RAM[SP]
 *
 * @r13
 * A=M
 * M=D // RAM[addr] = D
 *
  */

/* push segment i
 addr <- LCL + i
 RAM[SP] <- RAM[addr]
 SP++

 // addr <- LCL + i
 @SEG
 D=M
 @i
 A=D+A
 D=M // D=RAM[LCL + I]

 @SP
 A=M
 M=D // RAM[SP] = D

// SP++
 @SP
 M=M+1
 */

/* push static i
 @Foo.i
 D=M // D=Foo.i

 @SP
 A=M
 M=D // RAM[SP] = Foo.i

 @SP
 M=M+1 // SP++
*/

/* pop static i
  @SP
  M=M-1 // SP--
  A=M
  D=M // D=RAM[SP]

  @Foo.i
  M=D // Foo.i = RAM[SP]
 */
/* operation neg
 * @SP
 * M=M-1
 * A=M
 * M=!M
 *
 * @SP
 * M=M+1
 */
/* operation add sub etc arg1 op arg2
 @SP
 M=M-1 // SP--
 A=M
 D=M // D=RAM[SP]=arg2

 @R13
 M=D // R13 = arg2

 @SP
 M=M-1 // SP--
 A=M
 D=M // D=RAM[SP]=arg1

 @r13
 D=DopM // D=D op M

 @SP
 M=M+1 // SP++
 A=M
 M=D // RAM[SP] = D
 */
public class CodeWriter {

//    private final Map<String, Vector<Short>> segmentMap;

//    private final Stack<Short> mainStack;
//    private final short stackBase = 256;
    private final PrintWriter writer;
    private final String filename;
    private int labelNumber;
    private void writeIncSP() {
        writer.write("@SP\n");
        writer.write("M=M+1\n");
    }

    private void writeDecSP() {
        writer.write("@SP\n");
        writer.write("M=M-1\n");
    }

    private void writeBinaryOp(String op) {
        writeDecSP();
        writer.write("A=M\n");
        writer.write("D=M\n");
        writer.write("@SP\n");
        writer.write("A=M-1\n");
        writer.write(op+"\n");
    }

    private void writeLogicalOp(String jmpSym) {
        final String jumpLabel = String.format("LABEL_%s_%d", jmpSym, labelNumber);

        writeDecSP();
        writer.write("A=M\n");
        writer.write("D=M\n");

        writer.write("@SP\n");
        writer.write("A=M-1\n");

        writer.write("D=M-D\n"); // D = arg1 - arg2
        writer.write("M=-1\n");
        writer.write(String.format("@%s\n", jumpLabel));
        writer.write(String.format("D;%s\n", jmpSym));

        writer.write("@SP\n");
        writer.write("A=M-1\n");
        writer.write("M=0\n");
        writer.write(String.format("(%s)\n", jumpLabel));

        ++labelNumber;
    }

    private void writeUnaryOp(String line) {
        writer.write("@SP\n");
        writer.write("A=M-1\n");
        writer.write(line+"\n");
    }

    private void writeOffsetSegmentValue(String segment, String symbol, int offset) {
        writer.write(String.format("%s\n", symbol));
        writer.write("D=A\n");

        if (!segment.equals("pointer")) {
            writer.write(String.format("@%d\n", offset));
            writer.write("A=D+A\n");
            writer.write("D=A\n");
        }
    }
    private void writeOffsetSegmentMemory(String segment, String symbol, int offset) {
        writer.write(String.format("%s\n", symbol));
        writer.write("D=M\n");
        writer.write(String.format("@%d\n", offset));
        writer.write("A=D+A\n");
        writer.write("D=A\n");
    }

    private void writeOffsetSegment(String segment, String symbol, int offset) {
        switch (segment) {
            case "temp", "pointer" -> {
                writeOffsetSegmentValue(segment, symbol, offset);
            }
            case "local", "argument", "this", "that" -> {
                writeOffsetSegmentMemory(segment, symbol, offset);
            }
            default -> throw new RuntimeException("writeOffsetSegment: " + segment);
        }
    }

    private void writeRamSP() {
        writer.write("@SP\n");
        writer.write("A=M\n");
    }

    private void writePushSegment(String segment, int index) {
        switch (segment) {
            case "constant" -> {
                writer.write(String.format("@%d\n", index));
                writer.write("D=A\n");

                writer.write("@SP\n");
                writer.write("AM=M+1\n");
                writer.write("A=A-1\n");
                writer.write("M=D\n");
            }
            case "static" -> {
                final String sym = String.format("@%s.%d\n", filename.split(".+?/(?=[^/]+$)")[1], index);
                writer.write(sym);
                writer.write("D=M\n");

                writeRamSP();
                writer.write("M=D\n");

                writeIncSP();
            }
            case "local", "argument", "this", "that", "pointer", "temp" -> {
                final String sym = switch (segment) {
                    case "local" -> "@LCL";
                    case "argument" -> "@ARG";
                    case "this" -> "@THIS";
                    case "that" -> "@THAT";
                    case "pointer" -> index == 0 ? "@THIS" : "@THAT";
                    case "temp" -> "@5";
                    default -> throw new RuntimeException("writePushSegment: " + segment);
                };

                writeOffsetSegment(segment, sym, index);
                writer.write("D=M\n");

                writeRamSP();
                writer.write("M=D\n");

                writeIncSP();
            }
        }

    }

    private void writePopSegment(String segment, int index) {
        switch (segment) {
            case "static" -> {
                final String sym = String.format("@%s.%d\n", filename.split(".+?/(?=[^/]+$)")[1], index);

                writeDecSP();
                writer.write("A=M\n");
                writer.write("D=M\n");

                writer.write(sym);
                writer.write("M=D\n");
            }

            case "local", "argument", "this", "that", "temp", "pointer" -> {
                final String sym = switch (segment) {
                    case "local" -> "@LCL";
                    case "argument" -> "@ARG";
                    case "this" -> "@THIS";
                    case "that" -> "@THAT";
                    case "temp" -> "@5";
                    case "pointer" -> index == 0 ? "@THIS" : "@THAT";
                    default -> throw new RuntimeException("writePopSegment " + segment);
                };

                writeOffsetSegment(segment, sym, index);

                writer.write("@R13\n");
                writer.write("M=D\n");

                writeDecSP();

                writer.write("A=M\n");
                writer.write("D=M\n");

                writer.write("@R13\n");
                writer.write("A=M\n");
                writer.write("M=D\n");
            }
        }
    }

    public CodeWriter(String filename) {
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.filename = filename;
        this.labelNumber = 0;

    }

    public void writeArithmetic(String command) {
//        final short first = mainStack.pop();
//        short res = 0;
        switch (command) {
            case "add" -> {
//                res = (short) (mainStack.pop() + first);
                writeBinaryOp("M=D+M");
            }
            case "sub" -> {
//                res = (short) (mainStack.pop() - first);
                writeBinaryOp("M=M-D");
            }

            case "neg" -> {
//                res = (short) -first;
                writeUnaryOp("M=-M");
            }

            case "and" -> {
                writeBinaryOp("M=D&M");
            }

            case "or" -> {
                writeBinaryOp("M=D|M");
            }

            case "eq" -> {
                writeLogicalOp("JEQ");
            }

            case "gt" -> {
                writeLogicalOp("JGT");
            }

            case "lt" -> {
                writeLogicalOp("JLT");
            }

            case "not" -> {
                writeUnaryOp("M=!M");
            }
        };

//        mainStack.push(res);
    }

    public void writePushPop(CommandType cmd, String segment, int index) {
        if (cmd == CommandType.C_PUSH) {
            writePushSegment(segment, index);
        } else if (cmd == CommandType.C_POP) {
            writePopSegment(segment, index);
        }
    }

    public void close() {
        writer.close();
    }
}
