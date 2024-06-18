package dev.dagless.process.generation;

import dev.dagless.model.config.FunctionProvider;
import dev.dagless.model.splitting.SplitFunction;
import spoon.processing.AbstractProcessor;

public class CodeGeneratorFactory {
    public static AbstractProcessor<?> createCodeGenerationProcessor(SplitFunction splitFunction, FunctionProvider provider) {
        if (splitFunction.isForEachLoop()){
            return new ForEachCodeGenerationProcess(splitFunction, provider);
        } else {
            return new SequentialCodeGenerationProcess(splitFunction, provider);
        }
    }
}
