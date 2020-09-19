import java.io.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        var file = new File("src/c_program.c");
        Lexing lex = new Lexing(file);

    }
}
