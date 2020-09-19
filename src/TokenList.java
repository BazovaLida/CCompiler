import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Identity{INT_KEYWORD, IDENTIFIER, OPEN_PARENTHESES, CLOSE_PARENTHESES, OPEN_BRACE, RETURN_KEYWORD,
INT_CONSTANT, CLOSE_BRACE, UNKNOWN}

public class TokenList<String> {
    private LinkedHashMap<String, Identity> tokens = new LinkedHashMap<>();

    public void add(String next) {

        tokens.put(next, tokenIdentity(next));
        System.out.println(tokenIdentity(next));
    }

    private Identity tokenIdentity(String next) {
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
}
