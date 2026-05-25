package media.barney.contract.spring;

import java.util.Arrays;
import java.util.Set;
import media.barney.contract.runtime.ContractRuntime;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

final class ContractViolationHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Set<String> CONTRACT_STACK_CLASS_NAMES =
            Set.of(ContractRuntime.class.getName(), "media.barney.contract.runtime.ContractMessages");

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
        if (!isGeneratedContractViolation(exception)) {
            return null;
        }

        response.setStatus(resolveStatus(exception).value());
        return new ModelAndView();
    }

    private static boolean isGeneratedContractViolation(Exception exception) {
        if (!(exception instanceof IllegalArgumentException || exception instanceof IllegalStateException)) {
            return false;
        }

        return Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(CONTRACT_STACK_CLASS_NAMES::contains);
    }

    private static HttpStatus resolveStatus(Exception exception) {
        return exception instanceof IllegalArgumentException
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
