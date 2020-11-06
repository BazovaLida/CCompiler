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

    //-------------------------------------------------------Lexing----------------------------------------------------
    public boolean startLexing() {
        boolean error;
        String currNextLine;
        String currNext;

        //ending of rows (with ";" or without it) and comments
        Pattern pattern = Pattern.compile("&{2}|/{2,}|b[01]+\\b|[0-9]+|[a-zA-Z_][a-zA-Z_0-9]*|\\S");
        Matcher matcher;

        System.out.println("List of lexems");
        while (scanner.hasNextLine()) {
            currNextLine = scanner.nextLine();

            matcher = pattern.matcher(currNextLine);
            System.out.println("\n\tFor '" + currNextLine + "'");
            while (matcher.find()) {
                currNext = currNextLine.substring(matcher.start(), matcher.end());

                //comments lexing
                if (currNext.equals("//")) break;

                error = tokens.setPair(currNext);
                if (!error) {
                    System.out.println("Error while lexing the program in the row '" + currNextLine + "'");
                    return true; //stop program because of error in input file
                }
            }
        }
        return false; //Lexing of program is successful
    }

    //------------------------------------------------------Parsing----------------------------------------------------
    private ArrayList<Functions> funcArr;
    public Node startParsing() {
        ArrayList<Functions> functions = new ArrayList<>();
        funcArr = functions;
        Node node = new Node("", Integer.MAX_VALUE);
        TokenT currToken = tokens.getFirstType();
        // -------------------------------------------------------------
        Node finalNode = parseFunction(node, false, functions, currToken);
        if (finalNode == null || !(finalNode == node)) {
            return null;
        }else return node;
    }

    private Node parseFunction(Node node, boolean mainFound, ArrayList<Functions> functions, TokenT currTokenT){
        //check if code have next function
            if (!currTokenT.equals(TokenT.KEYWORD_INT))
                return parseError("Parsing error! Invalid type of function");


        currTokenT = tokens.getNextType();
        //if current function is the main
        if(currTokenT.equals(TokenT.KEYWORD_MAIN)) {
            if (!(tokens.getNextType().equals(TokenT.OPEN_PARENTHESES) &&
                    tokens.getNextType().equals(TokenT.CLOSE_PARENTHESES))){
                return parseError("Parsing error: '()' expected after the name of the 'main' function");
            }
            //syntax checking
            currTokenT = tokens.getNextType();
            if((!currTokenT.equals(TokenT.SEMICOLONS) && mainFound)) {
                return parseError("Invalid function syntax of the main function");
            } else if(!currTokenT.equals(TokenT.OPEN_BRACE)) {
                return parseError("Invalid 'main' function declaration");
            }

            //body of main function
            mainFound = true;
            Node mainFunc = new Node("main_func", 2);
            node.addChild(mainFunc);
            Node main = new Node("main_name", 0);
            mainFunc.addChild(main);
            Node mainBody = new Node("main_body", Integer.MAX_VALUE);
            mainFunc.addChild(mainBody);
            Variables vars = new Variables();

            Functions funsction = new Functions();
            if(!funsction.add("main", mainFunc, vars, false, false))
                return parseError("Parsing error: invalid function consistency.");
            functions.add(funsction);

            Node withBody = parseFuncBody(mainBody, vars);
            if (withBody == null || !withBody.equals(mainBody))
                return null;
            try {
                while (!tokens.getNextType().equals(TokenT.CLOSE_BRACE)){}
            } catch (IndexOutOfBoundsException e){
                return parseError("Invalid the end of the main function");
            }

            try {
                return parseFunction(node, mainFound, functions, tokens.getNextType());
            } catch (IndexOutOfBoundsException e) {
                //the end of recursion (without exceptions)
                return node;
            }
        }

        //not main function
        else if(!currTokenT.equals((TokenT.IDENTIFIER))){
            return parseError("Parsing error: invalid name of the function");
        }
        Variables currVars = new Variables();
        String name = tokens.currVal();
        Functions funsction = new Functions();
        Node FuncName = new Node(name + "_name", 0);

        if (!tokens.getNextType().equals(TokenT.OPEN_PARENTHESES)){
            return parseError("Parsing error: expected '(' after the name of the function");
        }
        Node paramNode = new Node(name + "_param", 10);
        Node withParam = parseFuncParam(paramNode, currVars);
        if (withParam == null || withParam != paramNode) {
            return null;
        }

        currTokenT = tokens.getNextType();
        if(currTokenT.equals(TokenT.SEMICOLONS))
        {
            //function declaration
            Node currFunc = new Node(name +"_func", 2);
            node.addChild(currFunc);
            currFunc.addChild(FuncName);
            currFunc.addChild(paramNode);
            if(!funsction.add(name, currFunc, currVars, true, !mainFound)) {
                return parseError("Parsing error: invalid function consistency.");
            }
            functions.add(funsction);
        } else if(currTokenT.equals(TokenT.OPEN_BRACE)) {
            //function definition
            Node currFunc = new Node(name + "_func", 3);
            node.addChild(currFunc);
            currFunc.addChild(FuncName);
            currFunc.addChild(paramNode);

            if (!funsction.add(name, currFunc, currVars, false, !mainFound)) {
                return parseError("Parsing error: invalid function consistency.");
            }
            functions.add(funsction);

            Node funcBody = new Node(name + "_body", Integer.MAX_VALUE);
            currFunc.addChild(funcBody);

            Node withBody = parseFuncBody(funcBody, currVars);
            if (withBody == null || !withBody.equals(funcBody))
                return null;
            try {
                while (!tokens.getNextType().equals(TokenT.CLOSE_BRACE)) {
                }
            } catch (IndexOutOfBoundsException e) {
                return parseError("Invalid the end of the '" + name + "' function");
            }
        }
        try {
            return parseFunction(node, mainFound, functions, tokens.getNextType());
        } catch (IndexOutOfBoundsException e) {
            if (functions.isEmpty())
                return parseError("Parsing error: no functions found");
            //the end of recursion (without exceptions)
            return node;
        }
    }

    private Node parseFuncParam(Node node, Variables vars){
        TokenT currTokenT = tokens.getNextType();
        if(currTokenT.equals(TokenT.CLOSE_PARENTHESES)){
            return node;
        }
        else if(!Tokens.typeKeyword.contains(currTokenT)){
            return parseError("Parsing error: type of '" + currTokenT + "' is undefined");
        }
        if(!tokens.getNextType().equals(TokenT.IDENTIFIER)){
            return parseError("Parsing error: parameter '" + currTokenT + "' is unexpected");
        }
        Node paramNode = new Node(tokens.currVal() + "_var", 0);
        paramNode.setPoint(vars.getPoint(tokens.currVal() + "_var"));
        node.addChild(paramNode);
        return parseFuncParam(node, vars);
    }

    private Node parseError(String msg) {
        System.out.println(msg);
        return null;
    }

    private Node parseFuncBody(Node currNode, Variables vars) {
        Node mainNode = currNode;
        TokenT currTokenT = tokens.getNextType();
        boolean hasType;

        while (!currTokenT.equals(TokenT.KEYWORD_RETURN)) {
            hasType = false;
            if (Tokens.typeKeyword.contains(currTokenT)) {
                hasType = true;
                currTokenT = tokens.getNextType();
            }

            if (currTokenT.equals(TokenT.IDENTIFIER)) {
                boolean canBeDecl = vars.contains(tokens.currVal() + "_var");
                boolean canBeInit = vars.totalContains(tokens.currVal() + "_var");
                if (hasType) {
                    if (canBeDecl) {
                        return parseError("The variable " + tokens.currVal() + " is declarated several times!");
                    }
                    vars.addVar(tokens.currVal() + "_var");
                } else if (!canBeInit) {
                    return parseError("Variable " + tokens.currVal() + " initialized, but not declarated");
                }

                Node var = new Node(tokens.currVal() + "_var", 2);
                currNode.addChild(var);
                currNode = var;
                currNode.setPoint(vars.getPoint(tokens.currVal() + "_var"));

                currTokenT = tokens.getNextType();
                if (currTokenT.equals(TokenT.SEMICOLONS)) {
                    currNode = currNode.getParent();
                }
                else if (currTokenT.equals(TokenT.EQUALS)) {
                    Node statNode = parseStatement(currNode, TokenT.SEMICOLONS, vars);
                    if (statNode == null || !statNode.equals(currNode))
                        return parseError("Error while parsing statement");

                    if(!vars.addVal(currNode.getValue())){
                        return parseError("Error while parsing! Variable " + currNode.getValue() + " is not initialised!");
                    }
                    currNode = currNode.getParent();
                }
                else return parseError("Error occurred after variable " + tokens.currVal());
            }
            else if(hasType){
                return parseError("Error while parsing! There is keyword and no variable after that");
            }
            else if (currTokenT.equals(TokenT.KEYWORD_IF)){
                Node childNode = new Node("if", 2);
                currNode.addChild(childNode);
                currNode = childNode;

                if(!tokens.getNextType().equals(TokenT.OPEN_PARENTHESES))
                    System.out.println("error: expected '(' after 'if'");

                Node ifNode = parseStatement(currNode, TokenT.CLOSE_PARENTHESES, vars);

                if (ifNode == null || !ifNode.equals(currNode))
                    return parseError("Error while parsing statement");
                currNode = currNode.getParent();
            }
            else if (currTokenT.equals(TokenT.KEYWORD_ELSE)){
                try {
                    if (!((currNode.getTailChild(1).getValue().equals("{") &&
                            currNode.getTailChild(1).getTailChild(1).getValue().equals("}") ||
                            currNode.getTailChild(1).getValue().matches("[a-zA-Z_][a-zA-Z_0-9]*_var")) &&
                            currNode.getTailChild(2).getValue().equals("if"))) {
                        return parseError("Error! 'else' without a previous 'if' or inappropriate value between it!");
                    }

                } catch (IndexOutOfBoundsException e){
                    return parseError("Error! 'else' without a previous 'if' or inappropriate value between it!");
                }

                Node childNode = new Node("else", 1);
                currNode.addChild(childNode);
            }
            else if(currTokenT.equals(TokenT.OPEN_BRACE)){
                vars = vars.openBrace();
                Node childNode = new Node("{", 100);
                try {
                    if (currNode.getTailChild(1).getValue().equals("else")) {
                        childNode.switchAfterElse();
                    }
                } catch (IndexOutOfBoundsException e){
                    //ok
                }
                currNode.addChild(childNode);
                currNode = childNode;
            }
            else if(currTokenT.equals(TokenT.CLOSE_BRACE)){
                vars = vars.closeBrace();
                if(!currNode.getValue().equals("{")){
                    return parseError("Error while parsing '{}'. There are '}', but no '{' before it!");

                }
                Node childNode = new Node("}", 0);
                currNode.addChild(childNode);
                currNode = currNode.getParent();
            }
            currTokenT = tokens.getNextType();
        }

        Node retNode = new Node("return", 2);
        mainNode.addChild(retNode);
        Node newRet = parseStatement(retNode, TokenT.SEMICOLONS, vars);
        if (newRet == null || !newRet.equals(retNode))
            return parseError("Error while parsing statement after 'return'");
        return mainNode;
    }

    private Node parseStatement(Node currNode, TokenT stopStopT, Variables vars) {
        EnumSet<TokenT> binaryOp = EnumSet.of(TokenT.DIVISION, TokenT.MULTIPLICATION, TokenT.LOGICAL_AND);

        TokenT currTokenT = tokens.getNextType();
        Node basic = currNode;

        while (!currTokenT.equals(stopStopT) || currNode.getValue().equals("(")) {
            if (currTokenT.equals(TokenT.NEGATION)) {
                if (currNode.getValue().equals("(") || currNode.equals(basic)) {
                    Node childNode = new Node("-", 1);
                    currNode.addChild(childNode);
                    currNode = childNode;
                } else
                    return parseError("Error while parsing '-': unary operation is without ()");
            } else if (binaryOp.contains(currTokenT)) {
                if (currTokenT.equals(TokenT.LOGICAL_AND)) {
                    while (currNode.hasMaxChildren() &&
                            !currNode.getValue().equals("(") && !currNode.equals(basic)) {
                        currNode = currNode.getParent();
                    }
                }

                if (currNode.hasNextChild()) {
                    Node binaryNode = new Node(tokens.currVal(), 2);
                    Node child = currNode.removeChild();
                    binaryNode.addChild(child);
                    currNode.addChild(binaryNode);
                    currNode = binaryNode;
                } else return parseError("Invalid binary operation syntax");
            }
            else if (currTokenT.equals(TokenT.OPEN_PARENTHESES)) {
                Node childNode = new Node("(", 1);
                currNode.addChild(childNode);
                currNode = childNode;
            } else if (currTokenT.equals(TokenT.CLOSE_PARENTHESES)) {
                try {
                    while (!currNode.getValue().equals("(")) {
                        currNode = currNode.getParent();
                    }
                } catch (NullPointerException e) {
                    return parseError("Error while parsing '()' There are ')', but no '(' before it!");
                }
                if (currNode.hasMaxChildren()) {
                    Node parent = currNode.getParent();
                    parent.replaceChild(currNode, currNode.getChild());
                    currNode = parent;
                }
            }
            else {
                currNode = parseValue(currNode, currTokenT, vars);
                if(currNode == null) return null;
            }
            try {
                while (currNode.hasMaxChildren() && !(currNode.getValue().equals("(") || currNode.getValue().equals("if"))) {
                    currNode = currNode.getParent();
                }
            } catch (NullPointerException e){
                return parseError("Error while parsing after token " + tokens.currVal());
            }
            currTokenT = tokens.getNextType();
        }
        return currNode;
    }

    private Node parseValue(Node currNode, TokenT currToken, Variables vars) {
        int index = vars.getVal(tokens.currVal() + "_var");
        Node childNode;
        if (currToken.equals(TokenT.INT_CONSTANT)) {
            String val = tokens.currVal();
            if (!(tokens.getNextType().equals(TokenT.DOT) & tokens.getNextType().equals(TokenT.INT_CONSTANT))) {
                tokens.indexMinus(2);
            }
            childNode = new Node(val, 0);
        } else if (currToken.equals(TokenT.INT_BIN_CONSTANT)) {
            String val = tokens.currVal().substring(1);
            val = Integer.toString(Integer.parseInt(val, 2));
            childNode = new Node(val, 0);
        } else if (index != -1) {
            String val =  tokens.currVal() + "_val";
            childNode = new Node(val, 0);
            childNode.setPoint(index);
        }
        else return parseError("Error while parsing value " + tokens.currVal());

        currNode.addChild(childNode);
        return currNode;
    }

    //----------------------------------------------------Generation--------------------------------------------------

    public String generator(Node node) {
        StringBuilder code = new StringBuilder();
        String funcName = node.getValue();
        code.append(".386\n")
                .append(".model flat, stdcall\n")
                .append("option casemap :none\n\n")

                .append("include C:\\masm32\\include\\kernel32.inc\n")
                .append("include C:\\masm32\\include\\user32.inc\n\n")

                .append("includelib C:\\masm32\\lib\\kernel32.lib\n")
                .append("includelib C:\\masm32\\lib\\user32.lib\n\n")

                .append(funcName).append(" PROTO\n\n")

                .append(".data\n")
                .append("msg_title db \"Result\", 0\n")
                .append("buffer db 128 dup(?)\n")
                .append("format db \"%d\",0\n\n")

                .append(".code\n")
                .append("start:\n")
                .append("\tinvoke ").append(funcName)
                .append("\n\tinvoke wsprintf, addr buffer, addr format, eax\n")
                .append("\tinvoke MessageBox, 0, addr buffer, addr msg_title, 0\n")
                .append("\tinvoke ExitProcess, 0\n\n")

                .append(funcName).append(" PROC\n")
                .append("\tpush ebp\n")
                .append("\tmov ebp, esp\n\n");

        Node curr = node;
        boolean done = false;
        while (!done) {
            if (curr.hasNextChild()) {
                curr = curr.getChild();
            } else {
                curr.codeGenerate(code);
                if (curr == node) done = true;
                else curr = curr.getParent();
            }
        }

        code.append(funcName).append(" ENDP\n")
                .append("END start\n");
        return code.toString();
    }
}

//------------------------------------------------------Auxiliary-Classes-----------------------------------------------

enum TokenT {
    KEYWORD_INCLUDE("include"),
    KEYWORD_INT("int"),
    KEYWORD_FLOAT("float"),
    KEYWORD_DOUBLE("double"),
    KEYWORD_CHAR("char"),
    KEYWORD_VOID("void"),
    KEYWORD_IF("if"),
    KEYWORD_ELSE("else"),
    KEYWORD_RETURN("return"),
    KEYWORD_MAIN("main"),
    INT_CONSTANT("[0-9]+"),
    INT_BIN_CONSTANT("b[01]+\\b"),
    IDENTIFIER("[a-zA-Z_][a-zA-Z_0-9]*"),
    OPEN_PARENTHESES("\\("),
    OPEN_INCLUDE("<"),
    DIVIDE_ASSIGN("/="),
    OPEN_BRACE("\\{"),
    CLOSE_PARENTHESES("\\)"),
    CLOSE_INCLUDE(">"),
    CLOSE_BRACE("}"),
    EQUALS("="),
    BITWISE_COMPLEMENT("~"),
    LOGICAL_AND("&&"),
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
    public static EnumSet<TokenT> typeKeyword = EnumSet.of(TokenT.KEYWORD_FLOAT, TokenT.KEYWORD_INT);
    public final ArrayList<String> tokens = new ArrayList<>(); //ArrayList of tokens in current row
    public final ArrayList<TokenT> types = new ArrayList<>(); //ArrayList of their types
    private int index = -1;
    private String prev = "";

    public boolean setPair(String curr) {
        index++;
        if (prev.equals("0") && getType(curr).equals(TokenT.INT_BIN_CONSTANT)) {
            index--;
        }
        tokens.add(index, curr);
        types.add(index, getType(curr));
        System.out.println("   '" + curr + "' is " + types.get(index) + " type");
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

    public void indexMinus(int i) {
        index = index - i;
    }

    public TokenT getFirstType() {
        index = 0;
        return types.get(index);
    }

    public TokenT getNextType() {
        index++;
        return types.get(index);
    }

    public String currVal() {
        return tokens.get(index);
    }

    public int currIndex() {
        return index;
    }
}

class Node {
    private Node parent;
    private final ArrayList<Node> children;
    private final String value;
    private int index;
    private final int maxChildren;
    private int childrenCount;
    private int point;
    private static int loopCount = 0;
    private boolean afterElse = false;
    private static int divCount = 0;

    public Node(String value, int maxChildren) {
        this.children = new ArrayList<>(1);
        this.index = 0;
        this.childrenCount = 0;
        this.value = value;
        this.maxChildren = maxChildren;
    }

    public boolean equals(Node node) {
        if(!this.value.equals(node.value))
            return false;

        try {
            for (int i = 0; i < children.size(); i++)
                if (!children.get(i).equals(node.children.get(i)))
                    return false;
        }catch(IndexOutOfBoundsException e){
            return false;
        }

        return true;
    }

    public String getValue() {
        return this.value;
    }

    private void setParent(Node p) {
        this.parent = p;
    }

    public Node getParent() {
        return this.parent;
    }

    public void addChild(Node ch) {
        ch.setParent(this);
        childrenCount++;
        this.children.add(ch);
    }

    public Node getChild() {
        Node child = this.children.get(this.index);
        index++;
        return child;
    }

    public Node removeChild() {
        Node child = this.children.remove(this.index);
        childrenCount--;
        return child;
    }

    public boolean hasNextChild() {
        return this.index < this.children.size();
    }

    public boolean hasMaxChildren() {
        return maxChildren == childrenCount;
    }

    public void replaceChild(Node child, Node newChild) {
        int i = this.children.indexOf(child);
        this.children.set(i, newChild);
        newChild.setParent(this);
    }

    public void setPoint(int val){
        point = (val + 3) * 4;
    }

    public void switchAfterElse(){
        afterElse = !afterElse;
    }

    public void codeGenerate(StringBuilder code) {
        if (value.matches("&&")) {

            code.append("\tpop ECX\n")
                    .append("\tpop EAX\n")
                    .append("\tcmp eax, 0   ; check if e1 is true\n")
                    .append("\tjne _clause2   ; e1 is not 0, so we need to evaluate clause 2\n")
                    .append("\tjmp _end\n")
                    .append("\t_clause2:\n")
                    .append("\t\tcmp ecx, 0 ; check if e2 is true\n")
                    .append("\t\tmov eax, 0\n")
                    .append("\t\tsetne al\n\n")

                    .append("\t_end:\n")
                    .append("\t\tpush eax\n\n");

        }
        else if (value.matches("\\*")) {

            code.append("\tmov edx, 0\n")
                    .append("\tpop ECX\n")
                    .append("\tpop EAX\n")
                    .append("\timul ECX\n")
                    .append("\tpush EAX\n\n");

        }
        else if (value.matches("/")) {

            code.append("\tpop ECX\n")
                    .append("\tpop EAX\n")
                    .append("\tmov EBX, EAX\n")
                    .append("\tshr EBX, 31\n")
                    .append("\tcmp EBX, 0\n")

                    .append("\tje _D" + divCount + "\n")
                    .append("\tmov edx, 0ffffffffh\n")
                    .append("\tjmp _D" + (divCount + 1) + "\n")
                    .append("_D" + divCount + ":\n")
                    .append("\tmov edx, 0\n")
                    .append("_D" + (divCount + 1) + ":\n")

                    .append("\tidiv ECX\n")
                    .append("\tpush EAX\n\n");
            divCount += 2;
        }
        else if (value.matches("-")) {

            code.append("\tpop EBX\n")
                    .append("\tneg EBX\n")
                    .append("\tpush EBX\n\n");

        }
        else if (value.matches("return")) {
            code.append("\tpop eax ;here is the result\n")
                    .append("\tmov esp, ebp  ; restore ESP; now it points to old EBP\n")
                    .append("\tpop ebp       ; restore old EBP; now ESP is where it was before prologue\n")
                    .append("\tret\n");
        }
        else if (value.matches("if")) {
            loopCount += 2;
            code.append("pop eax\t;if\n" +
                    "cmp eax, 0\n" +
                    "je _L" + loopCount + "\n\n");

        } else if (value.matches("else")) {
            code.append("jmp _L" + (loopCount + 1) + "\n" +
                    "_L" + loopCount + ":\n");

        }
        else if (value.matches("[0-9]+")) {
            code.append("\tpush ").append(value).append("\n");
        }
        else if (value.matches("[a-zA-Z_][a-zA-Z_0-9]*_var")) {
            if (this.childrenCount > 0) {
                code.append("\tpop " + "[ebp-").append(point).append("]\n");
            }
        }
        else if (value.matches("[a-zA-Z_][a-zA-Z_0-9]*_val")) {
            code.append("\tpush [ebp-").append(point).append("]     ;").append(value).append("\n");
        }
        else if(afterElse && value.matches("\\{")) {
            code.append("_L" +  (loopCount +1) + ":\n");
        }
    }

    public Node getTailChild(int place) {
        int index = this.children.size() - place;
        return this.children.get(index);
    }
}

class Variables{

    private final Map<String, Boolean> varList = new HashMap<>();
    private static final ArrayList<String> variables = new ArrayList<>();

    private Variables parrent;
    private final ArrayList<Variables> children = new ArrayList<>();

    public boolean contains(String var){
        return varList.containsKey(var);
    }

    public boolean totalContains(String var){
        return variables.contains(var);
    }

    public int getPoint(String var){
        return variables.lastIndexOf(var);
    }

    public void addVar(String var){
        varList.put(var, false);
        variables.add(var);
    }

    public boolean addVal(String var) {
        Variables currVars = this;
        try {
            while (!currVars.varList.containsKey(var)) {
                currVars = currVars.parrent;
            }
            currVars.varList.put(var, true);
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public int getVal(String var){
        return variables.lastIndexOf(var);
    }

    public Variables openBrace(){
        Variables child = new Variables();
        this.children.add(child);
        child.parrent = this;
        return child;
    }

    public Variables closeBrace(){
        for (int i = 0; i < varList.size(); i++) {
            variables.remove(variables.size() - 1);
        }
        return this.parrent;
    }
}

class Functions{
    private static final ArrayList<String> defNames = new ArrayList<>();
    private static final ArrayList<String> declNames = new ArrayList<>();
    private static int count = -1;
    private int id;
    private static final ArrayList<Node> nodes = new ArrayList<>();
    private String name;
    private Variables variables;
    private boolean beforeMain;
    private boolean definition;

    public boolean add(String name, Node paramNode, Variables vars, boolean definition, boolean beforeMain) {
        if ((defNames.contains(name) && definition) ||
                declNames.contains(name) && !definition) {
            return false;
        }
        if (definition) {
            if (declNames.contains(name)) {
                id = declNames.indexOf(name);
            } else {
                count++;
                id = count;
            }
            defNames.add(id, name);
        }
        else {
            if (defNames.contains(name)) {
                id = defNames.indexOf(name);
            } else {
                count++;
                id = count;
            }
            declNames.add(id, name);
        }
        try{
            if(!nodes.get(id).equals(paramNode))
                return false;
        } catch (IndexOutOfBoundsException e){
            nodes.add(id, paramNode);
        }
        this.name = name;
        this.definition = definition;
        this.beforeMain = beforeMain;
        this.variables = vars;
        return true;
    }
}