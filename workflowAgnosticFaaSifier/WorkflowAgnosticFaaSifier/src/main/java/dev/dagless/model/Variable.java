package dev.dagless.model;

public class Variable implements Comparable<Variable> {

    private String identifier;
    private String type;
    private boolean parallel = false;

    // Used by Jackson
    public Variable() {
    }

    public Variable(String identifier, String type) {
        this.identifier = identifier;
        this.type = type;
    }

    public Variable(String identifier, String type, boolean parallel) {
        this.identifier = identifier;
        this.type = type;
        this.parallel = parallel;
    }

    // Copy constructor
    public Variable(Variable variable) {
        this.identifier = variable.identifier;
        this.type = variable.type;
        this.parallel = variable.parallel;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getClassType() {
        switch (type) {
            case "int" -> {
                return "Integer";
            }
            case "double" -> {
                return "Double";
            }
            case "float" -> {
                return "Float";
            }
            case "long" -> {
                return "Long";
            }
            case "boolean" -> {
                return "Boolean";
            }
            case "char" -> {
                return "Character";
            }
            case "byte" -> {
                return "Byte";
            }
            case "short" -> {
                return "Short";
            }
            default -> {
                return type;
            }
        }
    }

    public String getNonParameterizedType() {
        return getClassType().replaceAll("<.*>", "");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVariableAsJson() {
        return "{" +
                "\"identifier\":\"" + this.identifier + "\"," +
                "\"type\":\"" + this.type + "\"," +
                "\"parallel\":\"" + this.parallel +
                "\"}";
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public String toString() {
        return "Variable{" +
                "identifier='" + identifier + '\'' +
                ", type='" + type + '\'' +
                ", isParallel=" + parallel +
                '}';
    }

    @Override
    public int compareTo(Variable o) {
        return this.identifier.compareTo(o.identifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable variable)) return false;
        return identifier.equals(variable.identifier);
    }
}
