package com.majortom.algorithms.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypedAlgorithmInvokerTest {

    @Test
    void rejectsNullAndWrongInputBeforeAlgorithmExecution() {
        AlgorithmInvoker invoker = new EchoProvider().invoker();

        AlgorithmInvocationException nullFailure = assertThrows(
                AlgorithmInvocationException.class,
                () -> invoker.prepare(null));
        assertEquals("algorithm.input.null", nullFailure.code());

        AlgorithmInvocationException typeFailure = assertThrows(
                AlgorithmInvocationException.class,
                () -> invoker.prepare(new OtherInput()));
        assertEquals("algorithm.input.type-mismatch", typeFailure.code());
    }

    @Test
    void rejectsNullOutput() {
        AlgorithmInvoker invoker = new NullOutputProvider().invoker();
        AlgorithmContext context = new AlgorithmContext() {
            @Override
            public String runId() {
                return "run";
            }

            @Override
            public String algorithmId() {
                return "null-output";
            }

            @Override
            public void emit(AlgorithmEvent event) {
            }

            @Override
            public void checkpoint() {
            }
        };

        AlgorithmInvocationException failure = assertThrows(
                AlgorithmInvocationException.class,
                () -> invoker.invoke(new EchoInput("value"), context));
        assertEquals("algorithm.output.null", failure.code());
    }

    private record EchoInput(String value) implements AlgorithmInput {
    }

    private record OtherInput() implements AlgorithmInput {
    }

    private record EchoOutput(String value) implements AlgorithmOutput {
    }

    private static class EchoProvider implements AlgorithmProvider<EchoInput, EchoOutput> {

        @Override
        public AlgorithmMetadata metadata() {
            return new AlgorithmMetadata("echo", "test", "1");
        }

        @Override
        public Class<EchoInput> inputType() {
            return EchoInput.class;
        }

        @Override
        public Class<EchoOutput> outputType() {
            return EchoOutput.class;
        }

        @Override
        public Algorithm<EchoInput, EchoOutput> createAlgorithm() {
            return (input, context) -> new EchoOutput(input.value());
        }
    }

    private static final class NullOutputProvider extends EchoProvider {

        @Override
        public AlgorithmMetadata metadata() {
            return new AlgorithmMetadata("null-output", "test", "1");
        }

        @Override
        public Algorithm<EchoInput, EchoOutput> createAlgorithm() {
            return (input, context) -> null;
        }
    }
}
