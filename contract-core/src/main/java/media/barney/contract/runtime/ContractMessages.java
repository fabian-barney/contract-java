package media.barney.contract.runtime;

import media.barney.contract.MaskRenderer;

final class ContractMessages {

    private ContractMessages() {}

    static IllegalArgumentException preconditionViolation(
            String methodName,
            String parameterName,
            ContractRule rule,
            Object value,
            Class<? extends MaskRenderer> maskRenderer) {
        return new IllegalArgumentException(parameterMessage(methodName, parameterName, rule, value, maskRenderer));
    }

    static IllegalStateException postconditionViolation(
            String methodName, ContractRule rule, Object value, Class<? extends MaskRenderer> maskRenderer) {
        return new IllegalStateException(returnMessage(methodName, rule, value, maskRenderer));
    }

    private static String parameterMessage(
            String methodName,
            String parameterName,
            ContractRule rule,
            Object value,
            Class<? extends MaskRenderer> maskRenderer) {
        String separator = rule.customDescription() ? ": " : " ";
        return "Parameter '"
                + parameterName
                + "' of method '"
                + methodName
                + "'"
                + separator
                + rule.description()
                + ", but was: "
                + ValueRenderer.render(value, maskRenderer);
    }

    private static String returnMessage(
            String methodName, ContractRule rule, Object value, Class<? extends MaskRenderer> maskRenderer) {
        return "Postcondition of method '"
                + methodName
                + "' violated: "
                + returnDescription(rule)
                + ", but was: "
                + ValueRenderer.render(value, maskRenderer);
    }

    private static String returnDescription(ContractRule rule) {
        if (rule.customDescription()) {
            return rule.description();
        }

        return "return value " + rule.description();
    }
}
