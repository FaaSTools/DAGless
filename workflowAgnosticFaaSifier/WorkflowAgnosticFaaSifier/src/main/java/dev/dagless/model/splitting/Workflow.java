package dev.dagless.model.splitting;

import dev.dagless.model.Variable;
import java.util.List;
import java.util.Set;

public class Workflow extends AbstractWorkflow{

        public Workflow(Set<Variable> inputVariables, Set<Variable> outputVariables, Set<SplitFunction> functions) {
            super(inputVariables, outputVariables, functions);
        }
}
