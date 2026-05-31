package media.barney.contract.spring;

import java.util.Arrays;
import media.barney.contract.runtime.ContractRuntime;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

final class ContractViolationHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final String CONTRACT_RUNTIME_PACKAGE = "media.barney.contract.runtime.";

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

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ModelAndView();
    }

    private static boolean isGeneratedContractViolation(Exception exception) {
        if (!(exception instanceof IllegalArgumentException || exception instanceof IllegalStateException)) {
            return false;
        }

        return Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(ContractViolationHandlerExceptionResolver::isContractRuntimeFrame);
    }

    private static boolean isContractRuntimeFrame(String className) {
        return ContractRuntime.class.getName().equals(className) || className.startsWith(CONTRACT_RUNTIME_PACKAGE);
    }
}
