package dev.dagless.process;

import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;

public class ProcessUtils {

    /**
     * Checks if the method is the main method in the program
     *
     * @param method
     * @return
     */
    public static boolean isMain(CtMethod<?> method) {
        return method.getModifiers().contains(ModifierKind.PUBLIC)
                && method.getModifiers().contains(ModifierKind.STATIC)
                && method.getType().getSimpleName().equals("void")
                && method.getSimpleName().equals("main");
    }


    public static void clearMethod(CtMethod<?> ctMethod) {
        CtBlock<?> mainBody = ctMethod.getBody();
        mainBody.getStatements().clear();
    }
}
