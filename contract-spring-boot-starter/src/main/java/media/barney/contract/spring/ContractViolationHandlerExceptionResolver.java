package media.barney.contract.spring;

import java.util.Arrays;
import java.util.Set;
import media.barney.contract.runtime.ContractRuntime;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

final class ContractViolationHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Set<String> CONTRACT_RUNTIME_VIOLATION_METHODS =
            Set.of("requireParameter", "requireReturn", "requireParameterValue", "requireReturnValue");
    private static final Set<String> CONTRACT_MESSAGE_VIOLATION_METHODS =
            Set.of("preconditionViolation", "postconditionViolation");
    private static final String CONTRACT_RUNTIME_PACKAGE_PREFIX = "media.barney.contract.runtime.";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Object handler,
            Exception exception) {
        if (response.isCommitted() || !isGeneratedContractViolation(exception)) {
            return null;
        }

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ModelAndView();
    }

    private static boolean isGeneratedContractViolation(Exception exception) {
        if (!(exception instanceof IllegalArgumentException || exception instanceof IllegalStateException)) {
            return false;
        }

        return Arrays.stream(exception.getStackTrace())
                .anyMatch(ContractViolationHandlerExceptionResolver::isViolationFactoryFrame);
    }

    private static boolean isViolationFactoryFrame(StackTraceElement frame) {
        String className = frame.getClassName();
        if (ContractRuntime.class.getName().equals(className)) {
            return CONTRACT_RUNTIME_VIOLATION_METHODS.contains(frame.getMethodName());
        }

        return className.startsWith(CONTRACT_RUNTIME_PACKAGE_PREFIX)
                && className.endsWith(".ContractMessages")
                && CONTRACT_MESSAGE_VIOLATION_METHODS.contains(frame.getMethodName());
    }
}
