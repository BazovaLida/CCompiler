import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        var file = new File("5-1-java-IV-81-Bazova.c");

        Compiler compiler = new Compiler(file);
        System.out.println("Program started.");
        boolean error = compiler.startLexing();
        if(!error) {
            System.out.println("Successful lexing!");
            Node node = compiler.startParsing();
            if (!Objects.isNull(node)) {
                System.out.println("Successful parsing!");

                String codeASM = compiler.generator(node);
                if (codeASM != null) {
                    System.out.println("Successful generation!");
                    try {
                        FileWriter myWriter = new FileWriter("5-1-java-IV-81-Bazova.asm");
                        myWriter.write(codeASM);
                        myWriter.close();
                        System.out.println("Successfully wrote to the file.");
                    } catch (IOException e) {
                        System.out.println("An error occurred while writing output in file");
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("Program finished.");
    }
}
