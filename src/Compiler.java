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

    //-------------------------------------------Lexing-----------------------------------------
    public boolean startLexing() {
        boolean error;
        boolean comments = false;
        boolean prev0 = false; //for binary numbers search
        String currNextLine;
        String currNext;

        //ending of rows (with ";" or without it) and comments
        Pattern pattern = Pattern.compile("b[01]+\\b|[0-9]+|[a-zA-Z_][a-zA-Z_0-9]*|\\S");
        Matcher matcher;

        System.out.println("List of lexems");
        while (scanner.hasNextLine()) {
            currNextLine = scanner.nextLine();

            matcher = pattern.matcher(currNextLine);
            System.out.println("\n\tFor \"" + currNextLine + "\"");
            while (matcher.find()) {
                currNext = currNextLine.substring(matcher.start(), matcher.end());

                //comments lexing
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
        }
        return false; //Lexing of program is successful
    }

    //-------------------------------------------Parsing-----------------------------------------

    private int i = 0;//current index

    private ParseAST parseAST = new ParseAST();

    public boolean startParsing() {
        TokenT currT = tokens.getTypes().get(i);
        TokenT returnType = currT; //return type
        if (currT.equals(TokenT.KEYWORD_INT)) {
            parseAST.setReturnType(returnType);
            currT = tokens.getTypes().get(++i);
            if (currT.equals(TokenT.IDENTIFIER)) {
                parseAST.setFunctionName(tokens.getTokens().get(i));
                currT = tokens.getTypes().get(++i);
                if (currT.equals(TokenT.OPEN_PARENTHESES)) {
                    currT = tokens.getTypes().get(++i);
                    if (currT.equals(TokenT.CLOSE_PARENTHESES)) {
                        currT = tokens.getTypes().get(++i);
                        if (currT.equals(TokenT.OPEN_BRACE)) {
                            i++;
                            if (parseFunctionBody(returnType)) {
                                currT = tokens.getTypes().get(i);
                                if (currT.equals(TokenT.CLOSE_BRACE)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return parsingError();
    }

    private static boolean parsingError() {
        System.out.println("Error while parsing");
        return false;
    }

    private boolean parseFunctionBody(TokenT returnType) {
        TokenT currT = tokens.getTypes().get(i);
        if (currT.equals(TokenT.KEYWORD_RETURN)) {
            currT = tokens.getTypes().get(++i);

            if (currT.equals(TokenT.INT_CONSTANT)) {
                String prevToken = tokens.getTokens().get(i);
                currT = tokens.getTypes().get(++i);

                if (currT.equals(TokenT.SEMICOLONS)) {
                    int decToken = Integer.parseInt(tokens.getTokens().get(i - 1));
                    int binToken = Integer.parseInt(Integer.toBinaryString(decToken));
                    parseAST.setReturnValue(binToken);
                    System.out.println("Parsed int value: " + decToken);
                    i++;
                    return true;
                } else if (currT.equals(TokenT.DOT)) {
                    int decToken = Integer.parseInt(tokens.getTokens().get(i - 1));
                    int binToken = Integer.parseInt(Integer.toBinaryString(decToken));
                    parseAST.setReturnValue(binToken);
                    currT = tokens.getTypes().get(++i);
                    if (currT.equals(TokenT.INT_CONSTANT)) {
                        currT = tokens.getTypes().get(++i);
                        if (currT.equals(TokenT.SEMICOLONS)) {
                            i ++;
                            System.out.println("Parsed float -> int value: " + decToken);
                            return true;
                        }
                    }
                } else if (prevToken.equals("0") && currT.equals(TokenT.INT_BIN_CONSTANT)){
                    currT = tokens.getTypes().get(++i);
                    if (currT.equals(TokenT.SEMICOLONS)) {
                        String binToken = tokens.getTokens().get(i - 1);
                        int value = Integer.parseInt(binToken.substring(1));
                        parseAST.setReturnValue(value);
                        System.out.println("Parsed binary int value: " + value);
                        i++;
                        return true;
                    }
                }
            }
        }
        return parsingError();
    }

    //-----------------------------------------Generation---------------------------------------

    public String generator(){
        String functionName = parseAST.getFunctionName();
//        TokenT returnType = parseAST.getReturnType();
        int returnValue = parseAST.getReturnValue();
        String lineSep = System.getProperty("line.separator");


        String code = ".386" + lineSep +
                ".model flat, stdcall" + lineSep +
                "option casemap :none" + lineSep + lineSep +
                "include C:\\masm32\\include\\kernel32.inc" + lineSep +
                "includelib C:\\masm32\\lib\\kernel32.lib" + lineSep +
                functionName + " PROTO" + lineSep + lineSep +
                ".data" + lineSep + lineSep +
                ".code" + lineSep + lineSep +
                "start:" + lineSep + lineSep +
                "invoke " + functionName + lineSep +
                "invoke ExitProcess,0" + lineSep + lineSep +
                functionName + " PROC" + lineSep +
                "mov eax, " + returnValue + lineSep +
                "ret" + lineSep +
                functionName + " ENDP" + lineSep +
                "END start" + lineSep;
        return code;
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
    INT_CONSTANT("[0-9]+"),
    INT_BIN_CONSTANT("b[01]+\\b"),
    IDENTIFIER("[a-zA-Z_][a-zA-Z_0-9]*"),
    OPEN_PARENTHESES("\\("),
    OPEN_INCLUDE("<"),
    OPEN_BRACE("\\{"),
    CLOSE_PARENTHESES("\\)"),
    CLOSE_INCLUDE(">"),
    CLOSE_BRACE("}"),
    EQUALS("="),
    PLUS("\\+"),
    MINUS("-"),
    MULTIPLY("\\*"),
    DIVIDE("/"),
    HASH("#"),
    DOT("\\."),
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
}


class ParseAST {

    private String functionName;
    private TokenT returnType;
    private int returnValue;

    public void setFunctionName(String functionN){
        functionName = functionN;
    }

    public void setReturnType(TokenT returnT){
        returnType = returnT;
    }

    public void setReturnValue(int returnV){
        returnValue = returnV;
    }

    public String getFunctionName(){
        return functionName;
    }

//    public TokenT getReturnType(){
//        return returnType;
//    }

    public int getReturnValue(){
        return returnValue;
    }
}