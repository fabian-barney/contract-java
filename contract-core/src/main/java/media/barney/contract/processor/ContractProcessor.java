package media.barney.contract.processor;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("*")
@SupportedOptions(ContractProcessor.ENABLED_OPTION)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class ContractProcessor extends AbstractProcessor {

    public static final String ENABLED_OPTION = "contracts.enabled";

    private JavacTrees trees;
    private ParserFactory parserFactory;
    private ContractResolver resolver;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        trees = JavacTrees.instance(processingEnvironment);
        Context context = ((JavacProcessingEnvironment) processingEnvironment).getContext();
        parserFactory = ParserFactory.instance(context);
        resolver = new ContractResolver(processingEnvironment.getElementUtils(), processingEnvironment.getTypeUtils());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver() || !enabled()) {
            return false;
        }

        boolean hasErrors = validate(roundEnvironment.getRootElements());
        if (!hasErrors) {
            transform(roundEnvironment.getRootElements());
        }

        return false;
    }

    private boolean enabled() {
        String option = processingEnv.getOptions().getOrDefault(ENABLED_OPTION, "true");
        return Boolean.parseBoolean(option);
    }

    private boolean validate(Set<? extends Element> rootElements) {
        boolean hasErrors = false;
        for (Element rootElement : rootElements) {
            hasErrors |= validateElement(rootElement);
        }

        return hasErrors;
    }

    private boolean validateElement(Element element) {
        boolean hasErrors = validateOwnContracts(element);
        for (Element enclosed : element.getEnclosedElements()) {
            hasErrors |= validateElement(enclosed);
        }

        return hasErrors;
    }

    private boolean validateOwnContracts(Element element) {
        if (element instanceof VariableElement variable) {
            return validateContracts(variable, variable.asType());
        }
        if (element instanceof ExecutableElement executable) {
            return validateExecutable(executable);
        }

        return false;
    }

    private boolean validateExecutable(ExecutableElement executable) {
        boolean hasErrors = false;
        for (VariableElement parameter : executable.getParameters()) {
            hasErrors |= validateContracts(parameter, parameter.asType());
        }

        List<ProcessorContract> returnContracts = resolver.semanticContracts(executable);
        if (!returnContracts.isEmpty() && executable.getReturnType().getKind() == TypeKind.VOID) {
            error(executable, "method contracts are not supported on void methods");
            return true;
        }

        return hasErrors | validateContracts(executable, executable.getReturnType());
    }

    private boolean validateContracts(Element element, TypeMirror valueType) {
        boolean hasErrors = false;
        for (ProcessorContract contract : resolver.semanticContracts(element)) {
            if (!resolver.supports(contract, valueType)) {
                error(element, "contract " + contract.kind() + " does not support type " + valueType);
                hasErrors = true;
            }
        }

        return hasErrors;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void transform(Set<? extends Element> rootElements) {
        for (Element rootElement : rootElements) {
            JCTree tree = trees.getTree(rootElement);
            if (tree != null) {
                tree.accept(new ContractTranslator());
            }
        }
    }

    private JCStatement statement(String source) {
        JavacParser parser = parserFactory.newParser(source, false, false, false);
        return parser.parseStatement();
    }

    private final class ContractTranslator extends TreeTranslator {

        @Override
        public void visitMethodDef(JCMethodDecl method) {
            if (method.body == null || method.sym == null) {
                result = method;
                return;
            }

            String methodName = methodName(method);
            List<ProcessorContract> returnContracts = resolver.semanticContracts(method.sym);
            method.body = rewriteReturns(method.body, methodName, returnContracts);
            method.body.stats = prepend(parameterChecks(method, methodName), method.body.stats);
            result = method;
        }

        private JCBlock rewriteReturns(JCBlock body, String methodName, List<ProcessorContract> returnContracts) {
            if (returnContracts.isEmpty()) {
                return body;
            }

            return (JCBlock) new ReturnTranslator(methodName, returnContracts).translate(body);
        }

        private List<JCStatement> parameterChecks(JCMethodDecl method, String methodName) {
            List<JCStatement> statements = new ArrayList<>();
            int parameterIndex = 0;

            for (JCVariableDecl parameter : method.params) {
                String parameterName = parameterName(parameter, parameterIndex);
                for (ProcessorContract contract : resolver.semanticContracts(parameter.sym)) {
                    statements.add(statement(
                            contract.parameterStatement(parameter.getName().toString(), methodName, parameterName)));
                }
                parameterIndex++;
            }

            return statements;
        }

        private com.sun.tools.javac.util.List<JCStatement> prepend(
                List<JCStatement> prefix, com.sun.tools.javac.util.List<JCStatement> original) {
            com.sun.tools.javac.util.List<JCStatement> updated = original;
            for (int index = prefix.size() - 1; index >= 0; index--) {
                updated = updated.prepend(prefix.get(index));
            }

            return updated;
        }

        private String methodName(JCMethodDecl method) {
            Element owner = method.sym.getEnclosingElement();
            String typeName = owner instanceof TypeElement typeElement
                    ? typeElement.getQualifiedName().toString()
                    : owner.toString();
            String simpleName = method.sym.getKind() == ElementKind.CONSTRUCTOR
                    ? "<init>"
                    : method.getName().toString();
            return typeName + "." + simpleName;
        }

        private String parameterName(JCVariableDecl parameter, int index) {
            String name = parameter.getName().toString();
            if (name == null || name.isBlank()) {
                return "arg" + index;
            }

            return name;
        }
    }

    private final class ReturnTranslator extends TreeTranslator {

        private final String methodName;
        private final List<ProcessorContract> contracts;

        ReturnTranslator(String methodName, List<ProcessorContract> contracts) {
            this.methodName = methodName;
            this.contracts = contracts;
        }

        @Override
        public void visitReturn(JCReturn tree) {
            if (tree.expr == null) {
                result = tree;
                return;
            }

            result = statement("return " + returnExpression(tree.expr) + ";");
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            result = tree;
        }

        @Override
        public void visitLambda(JCLambda tree) {
            result = tree;
        }

        private String returnExpression(JCExpression expression) {
            String source = expression.toString();
            for (ProcessorContract contract : contracts) {
                source = contract.returnExpression(source, methodName);
            }

            return source;
        }
    }
}
