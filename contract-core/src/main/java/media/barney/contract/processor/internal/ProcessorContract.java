package media.barney.contract.processor.internal;

import java.util.Map;
import media.barney.contract.runtime.ContractRuntime;
import media.barney.contract.runtime.RuntimeContract;

record ProcessorContract(
        RuntimeContract kind,
        String description,
        boolean customDescription,
        String maskRendererType,
        long min,
        long max,
        boolean minInclusive,
        boolean maxInclusive,
        int sizeMin,
        int sizeMax,
        String regexp) {

    private static final Map<Character, String> ESCAPES = Map.of(
            '\\', "\\\\",
            '"', "\\\"",
            '\n', "\\n",
            '\r', "\\r",
            '\t', "\\t");

    String parameterStatement(String valueExpression, String methodName, String parameterName) {
        return ContractRuntime.class.getCanonicalName()
                + ".requireParameterValue("
                + valueExpression
                + ", "
                + quote(methodName)
                + ", "
                + quote(parameterName)
                + ", "
                + kindExpression()
                + ", "
                + quote(description)
                + ", "
                + customDescription
                + ", "
                + maskRendererExpression()
                + ", "
                + min
                + "L, "
                + max
                + "L, "
                + minInclusive
                + ", "
                + maxInclusive
                + ", "
                + sizeMin
                + ", "
                + sizeMax
                + ", "
                + nullableString(regexp)
                + ");";
    }

    String returnExpression(String valueExpression, String methodName) {
        return ContractRuntime.class.getCanonicalName()
                + ".requireReturnValue("
                + valueExpression
                + ", "
                + quote(methodName)
                + ", "
                + kindExpression()
                + ", "
                + quote(description)
                + ", "
                + customDescription
                + ", "
                + maskRendererExpression()
                + ", "
                + min
                + "L, "
                + max
                + "L, "
                + minInclusive
                + ", "
                + maxInclusive
                + ", "
                + sizeMin
                + ", "
                + sizeMax
                + ", "
                + nullableString(regexp)
                + ")";
    }

    private String kindExpression() {
        return RuntimeContract.class.getCanonicalName() + "." + kind.name();
    }

    private String maskRendererExpression() {
        return maskRendererType == null ? "null" : maskRendererType + ".class";
    }

    private static String nullableString(String value) {
        return value == null ? "null" : quote(value);
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('"');
        for (int index = 0; index < value.length(); index++) {
            escaped.append(escape(value.charAt(index)));
        }
        escaped.append('"');
        return escaped.toString();
    }

    private static String escape(char character) {
        String escaped = ESCAPES.get(character);
        return escaped == null ? Character.toString(character) : escaped;
    }
}
