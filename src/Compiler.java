import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compiler {
    private final Scanner scanner;
    private final Tokens tokens = new Tokens();

    public Compiler(File input) throws FileNotFoundException {
        scanner = new Scanner(input);
    }

    public boolean startLexing() {
        boolean error;
        boolean comments = false;
        String currNextLine;
        String currNext;

        //ending of rows (with ";" or without it) and comments
        Pattern pattern = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*|\\S");
        Matcher matcher;

        System.out.println("List of lexems");
        while (scanner.hasNextLine()) {
            currNextLine = scanner.nextLine();

            matcher = pattern.matcher(currNextLine);
            System.out.println("\n\tFor \"" + currNextLine + "\"");
            while (matcher.find()) {
                currNext = currNextLine.substring(matcher.start(), matcher.end());

                //comment lexing
                if (currNext.equals("/")) {
                    if (comments) {
                        comments = false;
                        break;
                    }
                    comments = true;
                    continue;
                }

                error = tokens.setPair(currNext);
                if (!error) {
                    System.out.println("Error while lexing the program in the row \"" + currNextLine + "\"");
                    return true; //stop program because of error in input file
                }
            }
//            tokens.setPair("\n");
        }
        return false; //Lexing of program is successful
    }

    EnumSet<TokenT> keywordsTypes = EnumSet.range(TokenT.KEYWORD_INT, TokenT.KEYWORD_VOID);
    EnumSet<TokenT> numericTypes = EnumSet.range(TokenT.KEYWORD_INT, TokenT.KEYWORD_DOUBLE);
    EnumSet<TokenT> identifierTypes = EnumSet.range(TokenT.IDENTIFIER_MAIN, TokenT.IDENTIFIER);
    private int currIndex = 0;

    public boolean startParsing() {
        TokenT currT = tokens.getTypes().get(currIndex);
        if (currT.equals(TokenT.HASH)) {
            currT = tokens.getTypes().get(++currIndex);
            if (currT.equals(TokenT.KEYWORD_INCLUDE)) {
                currT = tokens.getTypes().get(++currIndex);
                if (currT.equals(TokenT.OPEN_INCLUDE)) {
                    currT = tokens.getTypes().get(++currIndex);
                    if (currT.equals(TokenT.IDENTIFIER)) {
                        currT = tokens.getTypes().get(++currIndex);
                        if (currT.equals(TokenT.CLOSE_INCLUDE)) {
                            System.out.println("\nParsed included library " + tokens.getTokens().get(currIndex - 1));
                            currIndex++;
                            return startParsing();
                        }
                    }
                }
            }
            return parsingError();
        }
        return parseFunction();
    }

    private static boolean parsingError() {
        System.out.println("Error while parsing");
        return false;
    }

    private boolean parseFunction() {
        boolean ok = true;
        boolean main = false;
        TokenT currT = tokens.getTypes().get(currIndex);
        if (keywordsTypes.contains(currT)) {
            currT = tokens.getTypes().get(++currIndex);
            if (identifierTypes.contains(currT)) {
                if (currT.equals(TokenT.IDENTIFIER_MAIN)) main = true;
                currT = tokens.getTypes().get(++currIndex);
                if (currT.equals(TokenT.OPEN_PARENTHESES)) {
                    currIndex++;
                    if (!main) ok = parseVariable(true);
                    currT = tokens.getTypes().get(currIndex);
                    if(currT.equals(TokenT.CLOSE_PARENTHESES)){
                        currT = tokens.getTypes().get(++currIndex);
                        if (currT.equals(TokenT.OPEN_BRACE)) {
                            currIndex ++;
                            return ok && parseFunctionBody();

                        }
                    }
                }
            }
            return parsingError();
        } return parsingError();
    }

    private boolean parseFunctionBody(){
        return true;
    }

    //insideFunc == "supposed to has value" - flag if we can set value to variable
    private boolean parseVariable(boolean insideFunc) {
        TokenT currT = tokens.getTypes().get(currIndex);
        if (keywordsTypes.contains(currT)) {
            if (insideFunc) {
                currT = tokens.getTypes().get(++currIndex);
                if (currT.equals(TokenT.IDENTIFIER)) {
                    currT = tokens.getTypes().get(++currIndex);
                    if (currT.equals(TokenT.COMMA)) {
                        currIndex++;
                        return parseVariable(insideFunc);
                    } else if (currT.equals(TokenT.CLOSE_PARENTHESES)) return true;
                }
            }
            currIndex++;
            return parseValue(currIndex - 1, false, false);
        }
        return true;
    }

    private boolean parseValue(int varIndex, boolean lastEquals, boolean lastComma) {
        TokenT currT = tokens.getTypes().get(currIndex);
        if (currT.equals(TokenT.IDENTIFIER)) {
            currT = tokens.getTypes().get(++currIndex);
            if (!lastEquals && currT.equals(TokenT.SEMICOLONS)) {
                currIndex++;
                return true;
            }
            else if (!lastEquals && currT.equals(TokenT.COMMA)) {
                currIndex++;
                return parseValue(varIndex, false, true);
            }
            else if (!lastComma && currT.equals(TokenT.EQUALS)) {
                currIndex ++;
                return afterEquals(varIndex);

            }
        }
        return false;
    }
    private boolean afterEquals(int varIndex){
        TokenT varT = tokens.getTypes().get(varIndex);
        TokenT currT = tokens.getTypes().get(currIndex);
        if (currT.equals(TokenT.INT_CONSTANT)) {
            currT = tokens.getTypes().get(++currIndex);
            if (currT.equals(TokenT.SEMICOLONS) && varT.equals(TokenT.KEYWORD_INT)) {
                System.out.println("Parsed int value");
                currIndex++;
                return true;
            }
            else if (currT.equals(TokenT.COMMA) && varT.equals(TokenT.KEYWORD_INT)) {
                currIndex++;
                return parseValue(varIndex, true, false);
            }
            else if (currT.equals(TokenT.DOT)) {
                currT = tokens.getTypes().get(++currIndex);
                if (currT.equals(TokenT.INT_CONSTANT)) {
                    currT = tokens.getTypes().get(++currIndex);
                    if (currT.equals(TokenT.IDENTIFIER_D) && varT.equals(TokenT.KEYWORD_DOUBLE)) {
                        currT = tokens.getTypes().get(++currIndex);
                        if (currT.equals(TokenT.SEMICOLONS)) {
                            System.out.println("Parsed double value");
                            currIndex++;
                            return true;
                        }
                        else if (currT.equals(TokenT.COMMA)) {
                            currIndex++;
                            return parseValue(varIndex, true, false);
                        }
                    }
                    else if (currT.equals(TokenT.SEMICOLONS) && varT.equals(TokenT.KEYWORD_FLOAT)) {
                        System.out.println("Parsed float value");
                        currIndex++;
                        return true;
                    }
                    else if (currT.equals(TokenT.COMMA) && varT.equals(TokenT.KEYWORD_FLOAT)) {
                        currIndex++;
                        return parseValue(varIndex, true, false);
                    }
                }
            }
        } else if (numericTypes.contains(varT)){
            System.out.println("Sorry, but current version of the program can parse only integer, double and float");
            return false;
        } return false;
    }
}

enum TokenT {
    KEYWORD_INCLUDE("include"),
    KEYWORD_INT("int"),
    KEYWORD_FLOAT("float"),
    KEYWORD_DOUBLE("double"),
    KEYWORD_CHAR("char"),
    KEYWORD_VOID("void"),
    KEYWORD_RETURN("return"),
    IDENTIFIER_MAIN("main"),
    IDENTIFIER_D("d"),
    IDENTIFIER("[a-zA-Z_][a-zA-Z_0-9]*"),
    OPEN_PARENTHESES("\\("),
    OPEN_INCLUDE("<"),
    OPEN_BRACE("\\{"),
    CLOSE_PARENTHESES("\\)"),
    CLOSE_INCLUDE(">"),
    CLOSE_BRACE("}"),
    INT_CONSTANT("[0-9]+"),
    EQUALS("="),
    PLUS("\\+"),
    MINUS("-"),
    MULTIPLY("\\*"),
    DIVIDE("/"),
    HASH("#"),
    DOT("."),
    COMMA(","),
    SPACE_N("\\n"),
    SEMICOLONS(";"),
    UNKNOWN("");

    public final String pattern;

    TokenT(String regex) {
        pattern = String.valueOf(Pattern.compile(regex));
    }
}

class Tokens {
    public final ArrayList<String> tokens = new ArrayList<>(); //ArrayList of tokens in current row
    public final ArrayList<TokenT> types = new ArrayList<>(); //ArrayList of their types
    private int index = -1;

    public boolean setPair(String next) {
        index++;
        tokens.add(next);
        types.add(getType(next));
        System.out.println(next + " is " + types.get(index) + " type");
        return types.get(index) != TokenT.UNKNOWN;
    }

    private TokenT getType(String next) {
        for (TokenT id : TokenT.values()) {
            if (Pattern.matches(id.pattern, next)) {
                return id;
            }
        }
        return TokenT.UNKNOWN;
    }

    public ArrayList<String> getTokens() {
        return tokens;
    }

    public ArrayList<TokenT> getTypes() {
        return types;
    }

    public int getMaxIndex() {
        return index;
    }
}
