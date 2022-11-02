import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

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
 *
 * Pushing into the stack: push constant i
 *      @i
 *      D=A
 * // RAM[SP] = D
 *      @SP
 *      A=M
 *      M=D
 * // SP++
 *      @SP /
 *      M=M+1
 *
 *
 */
public class CodeWriter {
    private short[] tempSegment; // starts at 5
    private short[] staticSegment; // starts at 16
    private short[] pointerSegment; // 3 and 4
    private short[] localSegment; // todo: find out

    private Map<String, short[]> segmentMap;

    private Stack<Short> mainStack;
    private final short stackBase = 256;
    private PrintWriter writer;


    public CodeWriter(String filename) {
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tempSegment = new short[8];

    }

    void writeArithmetic(String command) {
        final short first = mainStack.pop();

        final int res = switch (command) {
            case "add" -> mainStack.pop() + first;
            case "sub" -> mainStack.pop() - first;
            case "neg" -> -first;
        };

        mainStack.push((short) res);
    }

    void writePushPop(CommandType cmd, String segment, int index) {
        if (cmd == CommandType.C_PUSH) {

        }
    }

    public void close() {

    }
}
