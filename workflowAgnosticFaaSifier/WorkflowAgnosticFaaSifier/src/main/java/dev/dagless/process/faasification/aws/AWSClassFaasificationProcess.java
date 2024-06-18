package dev.dagless.process.faasification.aws;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashMap;
import java.util.logging.Logger;

import static dev.dagless.process.ProcessUtils.isMain;

public class AWSClassFaasificationProcess extends AbstractProcessor<CtClass<?>> {

    /**
     * This method is called for every class declaration in the program.
     * It adds the RequestHandler interface to the class containing the main method.
     * The input is defined as HashMap<String, String> and the output as HashMap<String, Object>.
     *
     * @param ctClass
     */
    @Override
    public void process(CtClass<?> ctClass) {
        for (CtMethod<?> ctMethod : ctClass.getMethods()) {
            if (isMain(ctMethod)) {
                CtTypeReference<?> superInterface = getFactory().Type().createReference("com.amazonaws.services.lambda.runtime.RequestHandler");

                CtTypeReference<?> lambdaInputType = getFactory().Type().createReference(HashMap.class);
                lambdaInputType.addActualTypeArgument(getFactory().Type().createReference(String.class));
                lambdaInputType.addActualTypeArgument(getFactory().Type().createReference(String.class));

                CtTypeReference<?> lambdaReturnType = getFactory().Type().createReference(HashMap.class);
                lambdaReturnType.addActualTypeArgument(getFactory().Type().createReference(String.class));
                lambdaReturnType.addActualTypeArgument(getFactory().Type().createReference(Object.class));

                superInterface.addActualTypeArgument(lambdaInputType);
                superInterface.addActualTypeArgument(lambdaReturnType);

                ctClass.setSuperInterfaces(java.util.Collections.singleton(superInterface));
            }
        }
    }

}
