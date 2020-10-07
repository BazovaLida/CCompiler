import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
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
    public Node startParsing() {
        if(tokens.getFirstType().equals(TokenT.KEYWORD_INT) && tokens.getNextType().equals(TokenT.IDENTIFIER)
        && tokens.getNextType().equals(TokenT.OPEN_PARENTHESES) && tokens.getNextType().equals(TokenT.CLOSE_PARENTHESES)
        && tokens.getNextType().equals(TokenT.OPEN_BRACE) && tokens.getNextType().equals(TokenT.KEYWORD_RETURN)) {
            Node.functionName = tokens.getToken(1);

            Node basicNode = new Node("return", 2);
            Node mainNode = parseStatement(basicNode);
            if (tokens.getNextType().equals(TokenT.CLOSE_BRACE)){
                return mainNode;
            }
        }
        System.out.println("Error while parsing");
        return null;
    }

    private Node parseStatement(Node currNode) {
        TokenT currTokenT = tokens.getNextType();
        while (!currTokenT.equals(TokenT.SEMICOLONS)) {
            if (currTokenT.equals(TokenT.NEGATION)) {
                Node childNode = new Node("-", 1);
                currNode.addChild(childNode);
                currNode = childNode;
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
            else if (currTokenT.equals(TokenT.OPEN_PARENTHESES)) {
                Node childNode = new Node("(", 1);
                currNode.addChild(childNode);
                currNode = childNode;
            }
            else if (currTokenT.equals(TokenT.CLOSE_PARENTHESES)) {
                while (!currNode.getValue().equals("(")) {
                    currNode = currNode.getParent();
                }
                Node parent = currNode.getParent();
                parent.replaceChild(currNode, currNode.getChild());
                currNode = parent;
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
//
//        currTokenT = tokens.getNextType();
//        if (currTokenT.equals(TokenT.CLOSE_PARENTHESES)) {
//            headNode.braceClose();
//            return headNode;
//        } else if (currTokenT.equals(TokenT.SEMICOLONS) && headNode.braceClosed()) {
//            tokens.indexMinus(1);
//            return headNode;
//        } else if (currTokenT.equals(TokenT.DIVISION)) {
//            Node newHeadNode = new Node(headNode);
//            newHeadNode.setValue("/");
//            Node childNode = new Node();
//            childNode = parseStatement(childNode);
//            newHeadNode.addChild(childNode);
//            return newHeadNode;
//        } else {
//        }
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
        String lineSep = System.getProperty("line.separator");

        StringBuilder code = new StringBuilder();
        code.append(".486" + lineSep +
                ".model flat, stdcall" + lineSep +
                "option casemap :none" + lineSep + lineSep +
                "include C:\\masm32\\include\\kernel32.inc" + lineSep +
                "include C:\\masm32\\include\\masm32.inc"+ lineSep +
                "include C:\\masm32\\include\\gdi32.inc"+ lineSep +
                "include C:\\masm32\\include\\user32.inc"+ lineSep + lineSep +
                "include C:\\masm32\\include\\windows.inc" + lineSep +
                "include C:\\masm32\\macros\\macros.asm" + lineSep + lineSep +
                "includelib C:\\masm32\\lib\\kernel32.lib" + lineSep +
                "includelib C:\\masm32\\lib\\masm32.lib" + lineSep +
                "includelib C:\\masm32\\lib\\gdi32.lib" + lineSep +
                "includelib C:\\masm32\\lib\\user32.lib" + lineSep + lineSep +
                functionName + " PROTO" + lineSep + lineSep +

                ".data" + lineSep + lineSep +
//                "ConsoleTitle byte \"My Title\", 0" + lineSep +
//                "Text1 byte \"Text\", 0" + lineSep +
//                "Num sdword -5" + lineSep +

                ".code" + lineSep + lineSep +
                "start:" + lineSep + lineSep +
//                "invoke StdOut, addr Text1" + lineSep +
//                "print str$(Num)" + lineSep +
//                "" + lineSep +

                "invoke " + functionName + lineSep +
                "invoke ExitProcess, 0" + lineSep + lineSep +
                functionName + " PROC" + lineSep);
        Node curr = node;
        boolean done = false;
        while (!done){
            if (curr.hasNextChild()) {
                curr = curr.getChild();
            } else {
                curr.getCode(code, lineSep);
                if (curr == node) done = true;
                else curr = curr.getParent();
            }
        }

//                "mov eax, " + returnValue + lineSep +
        code.append("END start" + lineSep);
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
        System.out.println(curr + " is " + types.get(index) + " type");
        prev = curr;
        return types.get(index) != TokenT.UNKNOWN;
    }

    public void deletePrev(){
        tokens.remove(index);
        types.remove(index);
        System.out.println("^ removed: comments");
        index --;
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
    private int maxChildren;
    private int childrenCount;
    public static String functionName;

    public Node(){
        this.children = new ArrayList<>(1);
        this.value = "";
        this.index = 0;
        this.childrenCount = 0;
    }
    public Node(String inst, int maxChildren){
        this.children = new ArrayList<>(1);
        this.value = "";
        this.index = 0;
        this.childrenCount = 0;
        this.value = inst;
        this.maxChildren = maxChildren;
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

    public void addLeftChild(Node ch){
        Node insert = ch;
        ch.setParent(this);
        Node saved;
        for(int i = 0; i < this.children.size(); i++){
            saved = this.children.get(i);
            this.children.set(i, insert);
            insert = saved;
        }
        this.addChild(insert);
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

    public boolean isEmpty(){
        return this.value.isEmpty();
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

    public void getCode(StringBuilder code, String lineSep) {
        switch (value) {
            case "/":
                code.append(lineSep + "pop BL" + lineSep + "pop AX" + lineSep + "div BL" + lineSep + "push AL" + lineSep + lineSep);
                break;
            case "-":
                code.append("pop eax" + lineSep + "neg eax" + lineSep + "push eax" + lineSep + lineSep);
                break;
            case "return":
                code.append(lineSep + "pop eax ;here is the result" + lineSep + lineSep);
                code.append("ret" + lineSep + functionName + " ENDP" + lineSep);
                break;
            default:
                code.append("push " + this.value + lineSep);
                break;
        }
    }
}