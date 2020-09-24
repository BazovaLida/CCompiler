import java.io.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        var file = new File("src/c_program.c");
        Compiler compiler = new Compiler(file);

        boolean error = compiler.startLexing();
        if(error) return;

        boolean notError = compiler.startParsing();
        if(!notError) return;
        System.out.println("Success!");
    }

}
