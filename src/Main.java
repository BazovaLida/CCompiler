import java.io.*;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        //for linux:
        var file = new File("1-1-java-IV-81-Bazova.txt");
        //for windows:
        //var file = new File("src/1-1-java-IV-81-Bazova.txt");

        Compiler compiler = new Compiler(file);

        boolean error = compiler.startLexing();
        if(error) return;

        boolean notError = compiler.startParsing();
        if(!notError) return;
        System.out.println("Successful parsing!");

        String codeASM = compiler.generator();
        try {
            FileWriter myWriter = new FileWriter("1-1-java-IV-81-Bazova.asm");
            myWriter.write(codeASM);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing output.");
            e.printStackTrace();
        }
    }

}
