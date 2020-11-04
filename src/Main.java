import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        var file = new File("4-1-java-IV-81-Bazova.txt");

        Compiler compiler = new Compiler(file);

        boolean error = compiler.startLexing();
        if(error) return;

        Node node = compiler.startParsing();
        if(Objects.isNull(node)) return;
        System.out.println("Successful parsing!");

        String codeASM = compiler.generator(node);
        try {
            FileWriter myWriter = new FileWriter("4-1-java-IV-81-Bazova.asm");
            myWriter.write(codeASM);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing output.");
            e.printStackTrace();
        }
    }
}
