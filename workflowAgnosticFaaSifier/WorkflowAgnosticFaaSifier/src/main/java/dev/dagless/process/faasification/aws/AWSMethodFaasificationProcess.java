package dev.dagless.process.faasification.aws;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashMap;

import static dev.dagless.process.ProcessUtils.isMain;

public class AWSMethodFaasificationProcess extends AbstractProcessor<CtMethod<?>> {

    public AWSMethodFaasificationProcess() {
    }

    /**
     * This method is called for every method declaration in the program. It filters for the main method and then
     * transforms it to a lambda function. It adds the necessary parameters, return type, and code to the method.
     * It also implements the parameter HashMap<String, String> input and the return HashMap<String, Object> output.
     * The input and output are defined in the Config class.
     *
     * @param ctMethod
     */
    @Override
    public void process(CtMethod<?> ctMethod) {
        if (isMain(ctMethod)) {
            // NAME
            ctMethod.setSimpleName("handleRequest");
            // MODIFIERS
            ctMethod.setModifiers(java.util.Collections.singleton(ModifierKind.PUBLIC));
            // RETURN TYPE
            ctMethod.setType(createReturnTypeReference());
            // ANNOTATION
            ctMethod.getAnnotations().clear();
            ctMethod.addAnnotation(getFactory().Code().createAnnotation(getFactory().Type().createReference(Override.class)));
            // PARAMETERS
            ctMethod.getParameters().clear();
            CtParameter<HashMap<String, String>> inputParameter = createInputParameter();
            ctMethod.addParameter(inputParameter);
            // adding the context parameter
            // since I do not want to add the dependency of the lambda runtime, I will just add the parameter like this
            getFactory()
                    .createParameter(
                            ctMethod.getReference().getExecutableDeclaration(),
                            getFactory().Type().createReference(
                                    "com.amazonaws.services.lambda.runtime.Context"), "context");
        }
    }

    /**
     * Creates the return type HashMap<String, Object>
     *
     * @return
     */
    private CtTypeReference<?> createReturnTypeReference() {
        CtTypeReference<?> mapTypeRef = getFactory().Type().createReference(HashMap.class);
        mapTypeRef.addActualTypeArgument(getFactory().Type().createReference(String.class));
        mapTypeRef.addActualTypeArgument(getFactory().Type().createReference(Object.class));
        return mapTypeRef;
    }

    /**
     * Creates the input parameter HashMap<String, String>
     *
     * @return
     */
    private CtParameter<HashMap<String, String>> createInputParameter() {
        CtTypeReference<?> mapTypeRef = getFactory().Type().createReference(HashMap.class);
        mapTypeRef.addActualTypeArgument(getFactory().Type().createReference(String.class));
        mapTypeRef.addActualTypeArgument(getFactory().Type().createReference(String.class));

        CtParameter<HashMap<String, String>> inputParameter = getFactory().createParameter();
        inputParameter.setSimpleName("input");
        inputParameter.setType(mapTypeRef);
        return inputParameter;
    }
}
