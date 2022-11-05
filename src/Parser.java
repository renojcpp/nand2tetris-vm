import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class Parser {
    private String[] commands;
    private String filename;
    private int index;

    public Parser(String filename) {
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader reader = new BufferedReader(fr);
            commands = reader.lines().map(String::trim).map(s -> s.replaceAll("\\s+", " "))
                    .filter(s -> !isComment(s) && !isEmptyLine(s))
                    .toArray(String[]::new);

            this.filename = filename;
        } catch (FileNotFoundException e) {
            System.err.println("No such file " + filename);
        }

        index = 0;
    }

    private String currentCommand() {
        return commands[index];
    }

    private boolean isComment(String src) {
        return src.startsWith("//");
    }

    private boolean isEmptyLine(String src) {
        return src.isBlank() || src.isEmpty();
    }

    public boolean hasMoreLines() {
        return index < commands.length;
    }

    public void advance() {
        index++;
    }
    public CommandType commandType() {
        final String instruction = currentCommand();
        int wpIndex = instruction.indexOf(' ');
        if (wpIndex == -1) {
            wpIndex = instruction.length();
        }

        final String first = (instruction.substring(0, wpIndex)).toLowerCase();

        return switch (first) {
            case "add", "sub", "neg",
                    "eq", "gt", "lt",
                    "and", "or", "not" -> CommandType.C_ARITHMETIC;
            case "push" -> CommandType.C_PUSH;
            case "pop" -> CommandType.C_POP;
            default -> throw new RuntimeException("CommandType()" + first);
        };
    }

    public String arg1() {
        final CommandType commandType = commandType();
        final String command = currentCommand();

        if (commandType == CommandType.C_ARITHMETIC) {
            return command;
        }

        return command.split(" ")[1];
    }

    public String arg2() {
        final String command = currentCommand();

        return command.split(" ")[2];
    }
}
