package dev.dagless.process.faasification.gcp;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.io.IOException;

import static dev.dagless.process.ProcessUtils.isMain;

public class GCPMethodFaasificationProcess extends AbstractProcessor<CtMethod<?>> {

    public GCPMethodFaasificationProcess() {
    }

    @Override
    public void process(CtMethod<?> ctMethod) {
        if (isMain(ctMethod)) {
            // NAME
            ctMethod.setSimpleName("service");
            // MODIFIERS
            ctMethod.setModifiers(java.util.Collections.singleton(ModifierKind.PUBLIC));
            // RETURN TYPE
            ctMethod.setType(getFactory().Type().VOID_PRIMITIVE);
            // ANNOTATION
            ctMethod.getAnnotations().clear();
            ctMethod.addAnnotation(getFactory().Code().createAnnotation(getFactory().Type().createReference(Override.class)));
            // EXCEPTIONS
            CtTypeReference<IOException> ioException = getFactory().Type().createReference(IOException.class);
            ctMethod.addThrownType(ioException);
            // PARAMETERS
            ctMethod.getParameters().clear();
            // adding the context parameter
            // since I do not want to add the dependency of the lambda runtime, I will just add the parameter like this
            getFactory()
                    .createParameter(
                            ctMethod.getReference().getExecutableDeclaration(),
                            getFactory().Type().createReference(
                                    "com.google.cloud.functions.HttpRequest"), "request").addModifier(ModifierKind.FINAL);
            getFactory()
                    .createParameter(
                            ctMethod.getReference().getExecutableDeclaration(),
                            getFactory().Type().createReference(
                                    "com.google.cloud.functions.HttpResponse"), "response").addModifier(ModifierKind.FINAL);
        }
    }
}
