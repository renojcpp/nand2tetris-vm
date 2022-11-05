public class VMTranslator {
    public static void translate(String[] args) {
        for (String arg : args) {
            translate(arg);
        }
    }

    private static void translate(String arg) {
        final int extIndex = arg.lastIndexOf(".");
        final String outputName = arg.substring(0, extIndex) + ".asm";
        CodeWriter writer = new CodeWriter(outputName);
        Parser parser = new Parser(arg);

        while (parser.hasMoreLines()) {
            String arg1 = "";
            String arg2 = "";
            CommandType cmdType = parser.commandType();
            arg1 = cmdType != CommandType.C_RETURN ? parser.arg1() : "";
            if (cmdType == CommandType.C_PUSH || cmdType == CommandType.C_POP ||
                    cmdType == CommandType.C_FUNCTION || cmdType == CommandType.C_CALL) {
                arg2 = parser.arg2();
            }

            switch (cmdType) {
                case C_ARITHMETIC -> {
                    writer.writeArithmetic(arg1);
                }
                case C_POP, C_PUSH -> {
                    writer.writePushPop(cmdType, arg1, Integer.parseInt(arg2));
                }
            }
            parser.advance();
        }

        writer.close();
    }
}
