package dev.dagless.process.faasification.gcp;

import dev.dagless.process.ProcessUtils;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;

public class GCPClassFaasificationProcess extends AbstractProcessor<CtClass<?>> {

    @Override
    public void process(CtClass<?> ctClass) {
        for (CtMethod<?> ctMethod : ctClass.getMethods()) {
            if (ProcessUtils.isMain(ctMethod)) {
                CtTypeReference<?> superInterface = getFactory().Type().createReference("com.google.cloud.functions.HttpFunction");
                ctClass.setSuperInterfaces(java.util.Collections.singleton(superInterface));
            }
        }
    }

}
