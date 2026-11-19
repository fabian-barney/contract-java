package media.barney.contract.runtime;

import media.barney.contract.MaskRenderer;

final class BuiltInContractTestSupport {

    private BuiltInContractTestSupport() {}

    static final class TextMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            return "[text]";
        }
    }

    static final class ThrowingMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new IllegalStateException("renderer failed");
        }
    }

    static final class LinkageErrorMaskRenderer implements MaskRenderer {

        @Override
        public String render(Object value) {
            throw new LinkageError("renderer linkage failed");
        }
    }
}
