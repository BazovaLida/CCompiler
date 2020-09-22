import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Identity{INT_KEYWORD, IDENTIFIER, OPEN_PARENTHESES, CLOSE_PARENTHESES, OPEN_BRACE, RETURN_KEYWORD,
    INT_CONSTANT, CLOSE_BRACE, UNKNOWN}

public class Lexing {
    public Lexing(File input) throws FileNotFoundException {
        Scanner scanner = new Scanner(input);
        LinkedHashMap<String, Identity> tokens = new LinkedHashMap<>();
        String currentToken;

        //word or any other symbol
        Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)|\\S|\\s");

        //splitting text by rows removing comments and ;
        while (scanner.hasNext(pattern)) {
//            currentToken = scanner.next(pattern);
            currentToken = 
            tokens.put(currentToken, setTokenIdentity(currentToken));

            System.out.println(currentToken);
        }
    }

    private Identity setTokenIdentity(String next) {
        Matcher matcher;
        if(Pattern.matches("int", (CharSequence) next)){
            return Identity.INT_KEYWORD;
        } else if (Pattern.matches("[a-zA-Z_][a-zA-Z_0-9]*", (CharSequence) next)){
            return Identity.IDENTIFIER;
        } else if (Pattern.matches("\\(", (CharSequence) next)){
            return Identity.OPEN_PARENTHESES;
        } else if (Pattern.matches("\\)", (CharSequence) next)){
            return Identity.CLOSE_PARENTHESES;
        } else if (Pattern.matches("\\{", (CharSequence) next)){
            return Identity.OPEN_BRACE;
        } else if (Pattern.matches("return", (CharSequence) next)){
            return Identity.RETURN_KEYWORD;
        } else if (Pattern.matches("[0-9]+", (CharSequence) next)){
            return Identity.INT_CONSTANT;
        } else if (Pattern.matches("}", (CharSequence) next)){
            return Identity.CLOSE_BRACE;
        } else {
            System.out.println("Current version of this compiler doesn't support token " + next);
            return Identity.UNKNOWN;
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
