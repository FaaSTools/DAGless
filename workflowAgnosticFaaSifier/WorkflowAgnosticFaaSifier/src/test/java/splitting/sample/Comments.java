package splitting.sample;

public class Comments {
    public static void main(String[] args) {
        int a = 1;
        // Just a comment
        int b = 2;
        /*
        Another comment
        */
        print("Hello World");
        /* A final comment */
        print(String.valueOf(a + b));
    }

    private static void print(String message) {
        System.out.println(message);
    }
}
