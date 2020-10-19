import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Compiler {
    private final Scanner scanner;
    private final Tokens tokens = new Tokens();

    public Compiler(File input) throws FileNotFoundException {
        scanner = new Scanner(input);
    }

    //-------------------------------------------Lexing-----------------------------------------
    public boolean startLexing() {
        boolean error;
        String currNextLine;
        String currNext;

        //ending of rows (with ";" or without it) and comments
        Pattern pattern = Pattern.compile("/{2,}|b[01]+\\b|[0-9]+|[a-zA-Z_][a-zA-Z_0-9]*|\\S");
        Matcher matcher;

        System.out.println("List of lexems");
        while (scanner.hasNextLine()) {
            currNextLine = scanner.nextLine();

            matcher = pattern.matcher(currNextLine);
            System.out.println("\n\tFor \"" + currNextLine + "\"");
            while (matcher.find()) {
                currNext = currNextLine.substring(matcher.start(), matcher.end());

                //comments lexing
                if (currNext.equals("//")) break;

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
    EnumSet<TokenT> keywordsTypes = EnumSet.of(TokenT.KEYWORD_FLOAT, TokenT.KEYWORD_INT);
    public Node startParsing() {
        if(tokens.getFirstType().equals(TokenT.KEYWORD_INT) &&
                tokens.getNextType().equals(TokenT.IDENTIFIER) &&
                tokens.getNextType().equals(TokenT.OPEN_PARENTHESES) &&
                tokens.getNextType().equals(TokenT.CLOSE_PARENTHESES) &&
                tokens.getNextType().equals(TokenT.OPEN_BRACE)) {
            Node.functionName = tokens.getToken(1);

            TokenT currTokenT = tokens.getNextType();
            Node currNode = new Node("main", 100);
            ArrayList<String> variables = new ArrayList<>();
            boolean hasType;

            while(!currTokenT.equals(TokenT.KEYWORD_RETURN)){
                hasType = false;
                if(keywordsTypes.contains(currTokenT) && currNode.getValue().equals("main")) {
                    Node child = new Node(tokens.getCurrToken(), 1);
                    currNode.addChild(child);
                    currNode = child;
                    hasType = true;
                }

                if(tokens.getNextType().equals(TokenT.IDENTIFIER)){
                    if (!hasType && currNode.getValue().equals("main") && !variables.contains(tokens.getCurrToken())){
                        System.out.println("Error! There is untyped var!");
                        return null;
                    }

                    Node var = new Node(tokens.getCurrToken(), 2);
                    variables.add(tokens.getCurrToken());
                    currNode.addChild(var);
                    currNode = var;
                    currTokenT = tokens.getNextType();

                    if(currTokenT.equals(TokenT.EQUALS)){
                        Node stat = parseStatement(currNode);
                    } else if(currTokenT.equals(TokenT.SEMICOLONS)){
                        break;
                    }

                } else{
                    System.out.println("No identifier after the type keyword!");
                    return null;
                }
                parseStatement()
            }
//            Node basicNode = new Node("return", 2);
//            Node mainNode = parseStatement(basicNode);
            assert mainNode != null;
            if (tokens.getNextType().equals(TokenT.CLOSE_BRACE) && mainNode.hasValChild()) {
                if (mainNode.equals(currNode)) {
                    return mainNode;
                } else System.out.println("\"()\" are not closed!");
            } else System.out.println("Error while parsing.");
        }
        return null;
    }

    private Node parseStatement(Node currNode) {
        TokenT currTokenT = tokens.getNextType();
        while (!currTokenT.equals(TokenT.SEMICOLONS)) {
            if (currTokenT.equals(TokenT.NEGATION)) {
                if (currNode.getValue().equals("(") || currNode.getValue().equals("return")) {
                    Node childNode = new Node("-", 1);
                    currNode.addChild(childNode);
                    currNode = childNode;
                }
                else{
                    System.out.println("Error while parsing \"-\": it is without ()");
                    return null;
                }
            }
            else if (currTokenT.equals(TokenT.DIVISION)) {
                if(currNode.hasNextChild()) {
                    Node binaryNode = new Node("/", 2);
                    Node child = currNode.removeChild();
                    binaryNode.addChild(child);
                    currNode.addChild(binaryNode);
                    currNode = binaryNode;
                } else break;
            }
            else if (currTokenT.equals(TokenT.MULTIPLICATION)) {
                if(currNode.hasNextChild()) {
                    Node binaryNode = new Node("*", 2);
                    Node child = currNode.removeChild();
                    binaryNode.addChild(child);
                    currNode.addChild(binaryNode);
                    currNode = binaryNode;
                } else break;
            }
            else if (currTokenT.equals(TokenT.OPEN_PARENTHESES)) {
                Node childNode = new Node("(", 1);
                currNode.addChild(childNode);
                currNode = childNode;
            }
            else if (currTokenT.equals(TokenT.CLOSE_PARENTHESES)) {
                try {
                    while (!currNode.getValue().equals("(")) {
                        currNode = currNode.getParent();
                    }
                } catch (NullPointerException e){
                    System.out.println("Error while parsing ()");
                    return null;
                }
                if(currNode.hasMaxChildren()) {
                    Node parent = currNode.getParent();
                    parent.replaceChild(currNode, currNode.getChild());
                    currNode = parent;
                }
            }
            else {
                String val = parseValue(currTokenT);
                if (Objects.nonNull(val)) {
                    Node childNode = new Node(val, 0);
                    currNode.addChild(childNode);
                }
                else return null;
            }
            while (currNode.hasMaxChildren() && !currNode.getValue().equals("(")) {
                currNode = currNode.getParent();
            }
            currTokenT = tokens.getNextType();
        }
        return currNode;
    }

    private String parseValue(TokenT firstVal){
        if (firstVal.equals(TokenT.INT_CONSTANT)) {
            String val = tokens.getCurrToken();
            if (!(tokens.getNextType().equals(TokenT.DOT) & tokens.getNextType().equals(TokenT.INT_CONSTANT))) {
                tokens.indexMinus(2);
            }
            return val;
        }if (firstVal.equals(TokenT.INT_BIN_CONSTANT)){
            String val = tokens.getCurrToken().substring(1);
            val = Integer.toString(Integer.parseInt(val, 2));
            return val;
        }
        System.out.println("Error while parsing value");
        return null;
    }

    //-----------------------------------------Generation---------------------------------------

    public String generator(Node node){
        String functionName = Node.functionName;

        StringBuilder code = new StringBuilder();
        code.append(".386\n")
                .append(".model flat, stdcall\n")
                .append("option casemap :none\n\n")
                .append("include C:\\masm32\\include\\kernel32.inc\n")
                .append("include C:\\masm32\\include\\user32.inc\n\n")
                .append("includelib C:\\masm32\\lib\\kernel32.lib\n")
                .append("includelib C:\\masm32\\lib\\user32.lib\n\n")
                .append(functionName).append(" PROTO\n\n")
                .append(".data\n")
                .append("msg_title db \"Результат обчислень виразу\", 0\n")
                .append("buffer db 128 dup(?)\n")
                .append("format db \"%d\",0\n\n")
                .append(".code\n")
                .append("start:\n")
                .append("\tinvoke ").append(functionName)
                .append("\n\tinvoke wsprintf, addr buffer, addr format, eax\n")
                .append("\tinvoke MessageBox, 0, addr buffer, addr msg_title, 0\n")
                .append("\tinvoke ExitProcess, 0\n\n")
                .append(functionName).append(" PROC\n");
        Node curr = node;
        boolean done = false;
        while (!done){
            if (curr.hasNextChild()) {
                curr = curr.getChild();
            } else {
                curr.codeGenerate(code);
                if (curr == node) done = true;
                else curr = curr.getParent();
            }
        }

        code.append("END start\n");
        return code.toString();
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
    BITWISE_COMPLEMENT("~"),
    LOGICAL_NEGATION("!"),
    ADDITION("\\+"),
    NEGATION("-"),
    MULTIPLICATION("\\*"),
    DIVISION("/"),
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
    private String prev = "";

    public boolean setPair(String curr) {
        index++;
        if(prev.equals("0") && getType(curr).equals(TokenT.INT_BIN_CONSTANT)){
            index --;
        }
        tokens.add(index, curr);
        types.add(index, getType(curr));
        System.out.println("   \"" + curr + "\" is " + types.get(index) + " type");
        prev = curr;
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

    public String getCurrToken(){
        return tokens.get(index);
    }

    public void indexMinus(int i){
        index = index - i;
    }

    public TokenT getFirstType(){
        index = 0;
        return types.get(index);
    }

    public TokenT getNextType(){
        index ++;
        return types.get(index);
    }

    public String getToken(int index) {
        return tokens.get(index);
    }
}

class Node {
    private Node parent;
    private final ArrayList<Node> children;
    private String value;
    private int index;
    private final int maxChildren;
    private int childrenCount;
    public static String functionName;
    private static boolean hasValue = false;

    public Node(String value, int maxChildren){
        this.children = new ArrayList<>(1);
        this.value = "";
        this.index = 0;
        this.childrenCount = 0;
        this.value = value;
        this.maxChildren = maxChildren;
        if (value.matches("[0-9]+")){
            hasValue = true;
        }
    }

    public String getValue(){
        return this.value;
    }

    public void setParent(Node p){
        this.parent = p;
    }

    public Node getParent(){
        return this.parent;
    }

    public void addChild(Node ch){
        childrenCount ++;
        this.children.add(ch);
        ch.setParent(this);
    }

    public Node getChild(){
        Node child = this.children.get(this.index);
        index ++;
        return child;
    }

    public Node removeChild(){
        Node child = this.children.remove(this.index);
        childrenCount --;
        return child;
    }

    public boolean hasNextChild(){
        return this.index < this.children.size();
    }

    public boolean hasMaxChildren(){
        return maxChildren == childrenCount;
    }

    public void replaceChild(Node child, Node newChild){
        int i = this.children.indexOf(child);
        this.children.set(i, newChild);
        newChild.setParent(this);
    }

    public void codeGenerate(StringBuilder code) {
        switch (value) {
            case "/":
                code.append("\tmov edx, 0\n")
                        .append("\tpop ECX\n")
                        .append("\tpop EAX\n")
                        .append("\tidiv ECX\n")
                        .append("\tpush EAX\n\n");
                break;
            case "*":
                code.append("\tmov edx, 0\n")
                        .append("\tpop ECX\n")
                        .append("\tpop EAX\n")
                        .append("\timul ECX\n")
                        .append("\tpush EAX\n\n");
                break;
            case "-":
                code.append("\tpop EBX\n")
                        .append("\tneg EBX\n")
                        .append("\tpush EBX\n\n");
                break;
            case "return":
                code.append("\tpop eax ;here is the result\n\n")
                        .append("\tret\n")
                        .append(functionName).append(" ENDP\n\n");
                break;
            default:
                code.append("\tpush ").append(this.value).append("\n");
                break;
        }
    }

    public boolean hasValChild() {
        return hasValue;
    }
}