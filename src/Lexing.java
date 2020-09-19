import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexing {
    public Lexing(File input) throws FileNotFoundException {
        Scanner scanner = new Scanner(input);
//        ArrayList<String> rows = new ArrayList<>();
        ArrayList<TokenList<String>> tokens = new ArrayList<>();
        int counter = 0;

        String currentRow;
        String currentWord;
//        ArrayList<Matcher> match = new ArrayList<>();

        //ending of rows (with ";" or without it) and comments
        scanner.useDelimiter(";\\s+|/{2,}([\\s\\S]+?)\\n+\\s*");
        Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)|\\S");
        Matcher matcher;

        //splitting text by rows removing comments and ;
        while (scanner.hasNext()) {
            currentRow = scanner.next();
//            rows.add(currentRow);
            tokens.add(new TokenList<>());
            System.out.println(currentRow);

            matcher = pattern.matcher(currentRow);
            while (matcher.find()) {
                currentWord = currentRow.substring(matcher.start(), matcher.end());
                tokens.get(counter).add(currentWord);
            }
            counter ++;
        }
    }

//    private static Boolean isKeyword(String buff){
//        String[] KEYWORDS = {"auto","break","case","char","const","continue","default","do","double","else",
//                "enum","extern","float","for","goto","if","int","long","register","return","short","signed",
//                "sizeof","static","struct","switch","typedef","union","unsigned","void","volatile","while"};
//        for (String word : KEYWORDS) {
//            if(buff == word) return true;
//        }
//        return false;
//    }
}
