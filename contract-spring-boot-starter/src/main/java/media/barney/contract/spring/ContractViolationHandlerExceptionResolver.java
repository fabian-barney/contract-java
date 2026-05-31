package media.barney.contract.spring;

import java.util.Arrays;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

final class ContractViolationHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Set<String> CONTRACT_MESSAGE_VIOLATION_METHODS =
            Set.of("preconditionViolation", "postconditionViolation");
    private static final String CONTRACT_MESSAGES_CLASS_NAME = "media.barney.contract.runtime.ContractMessages";

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

        response.resetBuffer();
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
        return CONTRACT_MESSAGES_CLASS_NAME.equals(frame.getClassName())
                && CONTRACT_MESSAGE_VIOLATION_METHODS.contains(frame.getMethodName());
    }
}
