package me.f1nal.trinity.logging;

public class Logging {
    private static String format(String format, Object... args) {
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < format.length()) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex]);
                    argIndex++;
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    public static void log(String color, Object format, Object... args) {
        final String formatted = format(String.valueOf(format), args);
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final String callerClass;
        if (stackTrace.length > 2) {
            final String className = stackTrace[2].getClassName();
            callerClass = className.substring(className.lastIndexOf('.') + 1) + "#" + stackTrace[2].getMethodName();
        } else {
            callerClass = "<unknown>";
        }
        System.out.println(callerClass + " " + color + " " + formatted);
    }

    public static void info(Object format, Object... args) {
        log("\033[36mINFO\033[0m", format, args);
    }

    public static void error(Object format, Object... args) {
        log("\033[31mERROR\033[0m", format, args);
    }

    public static void warn(Object format, Object... args) {
        log("\033[33mWARN\033[0m", format, args);
    }

    public static void debug(Object format, Object... args) {
        log("\033[35mDEBUG\033[0m", format, args);
    }
}
