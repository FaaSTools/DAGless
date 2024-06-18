package splitting.sample;

public class CustomMethodInvocations {

    public static void main(String[] args) {
        int a = 1;
        int c = 3;
        int workflowOutput;
        print(String.valueOf(a));
        int d = 4;
        int b = 2;
        print(String.valueOf(b) + String.valueOf(c));
        String both = String.valueOf(a) + String.valueOf(b) + String.valueOf(d);
        print(both);
    }

    public static void print(String a) {
        System.out.println(a);
    }
}
