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
        System.out.println("---------Lexing started---------");
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
            if(!currNextLine.startsWith("//"))
                System.out.println("\n\n\tFor '" + currNextLine + "'");
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
    private Functions functions = new Functions();
    public Node startParsing() {
        System.out.println("---------Parsing started---------");
        Variables vars = new Variables();
        Node node = new Node("", Integer.MAX_VALUE);
        TokenT currToken = tokens.getFirstType();

        Node finalNode = parseFunction(node,  currToken, vars);
        if (finalNode == null || !(finalNode == node))
            return null;
        String errorFunc = functions.finalCheck();
        if(errorFunc != null)
            return parseError("Parsing error: function '" + errorFunc + "' called, but undefined!");
        if(!functions.containsMain())
            return parseError("Parsing error: there is no main function in the program!");
        return node;
    }

    private Node parseFunction(Node node, TokenT currTokenT, Variables variables){
        String name;
        Variables currVars = variables.openBrace();
        Variables prevVars;
        //check if code have next function
        if (currTokenT.equals(TokenT.KEYWORD_INT)) {
            currTokenT = tokens.getNextType();

            //if current function is the main
            if (currTokenT.equals(TokenT.KEYWORD_MAIN)) {

                //syntax checking
                if (tokens.getNextType().equals(TokenT.OPEN_CAST) &&
                        tokens.getNextType().equals(TokenT.CLOSE_CAST)) {//  '()'
                    currTokenT = tokens.getNextType();
                    if (currTokenT.equals(TokenT.SEMICOLONS) ) {// ';'
                        prevVars = currVars.skipCurBrace(0);
                        if (tokens.hasNext()) {
                            return parseFunction(node, tokens.getNextType(), prevVars);
                        } else return node;
                    }
                    else if (!currTokenT.equals(TokenT.OPEN_BRACE)) {
                        return parseError("Invalid 'main' function declaration");
                    }
                }
                else return parseError("Parsing error: '()' expected after the name of the 'main' function");

                //body of main function
                name = "main";
                Node mainFunc = new Node("main_func", Integer.MAX_VALUE);
                node.addChild(mainFunc);
                mainFunc.addChild(new Node("main_name", 0));

                boolean containsCheck =  functions.foundFunc(name, 0);
                boolean noSuchBodyCheck = functions.functionBody(name, 0);
                if(!(containsCheck && noSuchBodyCheck))
                    return parseError("Parsing error: There are 2 main functions");

                Node withBody = parseFuncBody(mainFunc, currVars);
                if (withBody == null || !withBody.equals(mainFunc))
                    return null;
            }

            //not main function
            else if (currTokenT.equals((TokenT.IDENTIFIER))) {
                name = tokens.currVal();
                if (tokens.getNextType().equals(TokenT.OPEN_CAST)) {
                    Node paramNode = new Node(name + "_param", 10);
                    Node withParam = parseFuncParam(paramNode, currVars);
                    if (withParam == null || withParam != paramNode)
                        return null;

                    int paramCount = withParam.getChildrenCount();
                    boolean containsCheck =  functions.foundFunc(name, paramCount);

                    currTokenT = tokens.getNextType();
                    if (currTokenT.equals(TokenT.SEMICOLONS)) {
                        //function declaration
                        prevVars = currVars.skipCurBrace(paramCount);
                        return safeRecursion(node, prevVars);
                    }
                    else if (currTokenT.equals(TokenT.OPEN_BRACE)) {
                        //function definition
//                        node.newFunc();
                        Node currFunc = new Node(name + "_func", 3);
                        node.addChild(currFunc);
                        currFunc.addChild(new Node(name + "_name", 0));
                        currFunc.addChild(paramNode);
                        Node funcBody = new Node(name + "_body", Integer.MAX_VALUE);
                        currFunc.addChild(funcBody);

                        boolean noSuchBodyCheck = functions.functionBody(name, paramCount);
                        if(!noSuchBodyCheck)
                            return parseError("Parsing error: There are 2 '" + name + "' functions");
                        if(!containsCheck)
                            return parseError("Parsing error: Function '" + name  + "' has different list of parameters");

                        Node withBody = parseFuncBody(funcBody, currVars);
                        if (withBody == null || !withBody.equals(funcBody))
                            return null;
                    }
                    else return parseError("Parsing error: invalid syntax after parameters of the '" + name + "' function");
                } else return parseError("Parsing error: expected '(' after the name of the function");
            }
            else return parseError("Parsing error: invalid name of the function");
            prevVars = currVars.closeBrace();
        }
        else return parseError("Parsing error! Invalid type of function");

        boolean finalBrace = false;
        while (tokens.hasNext())
            if(tokens.getNextType().equals(TokenT.CLOSE_BRACE)){
                finalBrace = true;
                break;
            }
        if(!finalBrace)
            return parseError("Invalid the end of the " + name + " function");
        else return safeRecursion(node, prevVars);
    }

    private Node safeRecursion(Node node, Variables variables){
        if(tokens.hasNext()){
            return parseFunction(node, tokens.getNextType(), variables);
        }
        //the end of recursion (without exceptions)
        else return node;
    }

    private Node parseFuncParam(Node node, Variables vars){
        TokenT currTokenT = tokens.getNextType();
        while (!currTokenT.equals(TokenT.CLOSE_CAST)){
            if(!Tokens.typeKeyword.contains(currTokenT)){
                return parseError("Parsing error: type '" + currTokenT + "' is undefined");
            }
            if(!tokens.getNextType().equals(TokenT.IDENTIFIER)){
                return parseError("Parsing error: parameter '" + currTokenT + "' is unexpected");
            }
            vars.addVar(tokens.currVal() + "_var");
            Node paramNode = new Node(tokens.currVal() + "_var", 0);
            paramNode.isParam();
            paramNode.setPoint(vars.getPoint(tokens.currVal() + "_var"));
            node.addChild(paramNode);

            currTokenT = tokens.getNextType();
            if(currTokenT.equals(TokenT.COMMA)) {
                currTokenT = tokens.getNextType();
                continue;
            }else if(!currTokenT.equals(TokenT.CLOSE_CAST))
                return parseError("Parsing error: type of '" + currTokenT + "' is undefined");
        }
        return node;

    }

    private Node parseError(String msg) {
        System.out.println(msg);
        System.out.println("Current lexem is " + tokens.currVal());
        return null;
    }

    private Node callFunction(String name, Node currNode, Variables vars){
        Node callNode = new Node(name + "_call", 10);
        currNode.addChild(callNode);
        TokenT currTokenT = tokens.getNextType();
        int argumentsCount = 0;
        try {
            while (!currTokenT.equals(TokenT.CLOSE_CAST)) {
                argumentsCount ++;
                Node pargNode = parseStatement(callNode, TokenT.COMMA, vars, currTokenT, false);
                if (pargNode == null || !pargNode.equals(callNode))
                    return parseError("Error while parsing function argument");
                currTokenT = tokens.getNextType();
            }
        }catch (NullPointerException e){
            return parseError("Parsing error: there is wrong arguments syntax while calling '" + name + "' function");
        }
        if(!functions.activateFunction(name, argumentsCount))
            return parseError("Parsing error: '"+ name + "' with " + argumentsCount + " parameters was not declared!");
        return currNode;
    }

    private Node parseFuncBody(Node currNode, Variables vars) {
        Node mainNode = currNode;
        TokenT currTokenT = tokens.getNextType();
        boolean hasType;
        int forPoint = 0;

        while (!currTokenT.equals(TokenT.KEYWORD_RETURN)) {
            hasType = false;
            if (Tokens.typeKeyword.contains(currTokenT)) {
                hasType = true;
                currTokenT = tokens.getNextType();
            }
            if (currTokenT.equals(TokenT.IDENTIFIER)) {
                Node withIdentif = parseIdentifier(vars, hasType, currNode, TokenT.SEMICOLONS);
                if (withIdentif == null || !withIdentif.equals(currNode))
                    return null;
            }
            else if (hasType) {
                return parseError("Error while parsing! There is keyword and no variable after that");
            }
            else if (currTokenT.equals(TokenT.KEYWORD_FOR)) {
                forPoint++;
                vars = vars.openBrace();
                if (!tokens.getNextType().equals(TokenT.OPEN_CAST))
                    return parseError("Parsing error: there is no '(' after 'for'");

                // initial clause
                //int i = 0;
                //i = 0;
                //;
                currTokenT = tokens.getNextType();
                if (Tokens.typeKeyword.contains(currTokenT)) {
                    hasType = true;
                    currTokenT = tokens.getNextType();
                }
                if (currTokenT.equals(TokenT.IDENTIFIER)) {
                    Node withIdentif = parseIdentifier(vars, hasType, currNode, TokenT.SEMICOLONS);
                    if (withIdentif == null || !withIdentif.equals(currNode))
                        return parseError("Error while parsing identifier for 'for' loop");
                } else if (!currTokenT.equals(TokenT.SEMICOLONS)) {
                    return parseError("Parsing error: expected initial clause after 'for(' or nothing before ';'");
                }

                Node forNode = new Node("for", 4);
                currNode.addChild(forNode);
                forNode.addChild(new Node("for_start" + forPoint, 0));

                // controlling expression
                // i <= 0
                // ;
                // statement
                currTokenT = tokens.getNextType();
                TokenT nextTokenT = tokens.getNextType();
                tokens.indexMinus(1);
                if (currTokenT.equals(TokenT.IDENTIFIER) && vars.totalContains(tokens.currVal() + "_var") &&
                        nextTokenT.equals(TokenT.LESS_THAN)) {
                    Node identNode = new Node(tokens.currVal() + "_val_for", 2);
                    identNode.setPoint(vars.getVal(tokens.currVal() + "_var"));
                    forNode.addChild(identNode);
                    tokens.getNextType();
                    currTokenT = tokens.getNextType();
                    if (currTokenT.equals(TokenT.EQUALS)) {
                        forNode.addChild(new Node("minusOne", 0));
                        currTokenT = tokens.getNextType();
                    }

                    Node lessNode = new Node("less" + forPoint, 2); //2 for statement function
                    currNode.addChild(lessNode);
                    Node withStat = parseStatement(lessNode, TokenT.SEMICOLONS, vars, currTokenT, false);
                    if (withStat == null || !withStat.equals(lessNode))
                        return parseError("Error while parsing statement after '<' in 'for' loop");
                } else if (currTokenT.equals(TokenT.IDENTIFIER) && vars.totalContains(tokens.currVal() + "_var") &&
                        nextTokenT.equals(TokenT.MORE_THAN)) {
                    Node identNode = new Node(tokens.currVal() + "_val_for", 2);
                    identNode.setPoint(vars.getVal(tokens.currVal() + "_var"));
                    forNode.addChild(identNode);
                    tokens.getNextType();
                    currTokenT = tokens.getNextType();
                    if (currTokenT.equals(TokenT.EQUALS)) {
                        forNode.addChild(new Node("plusOne", 0));
                        currTokenT = tokens.getNextType();
                    }

                    Node moreNode = new Node("more" + forPoint, 2); //2 for statement function
                    forNode.addChild(moreNode);
                    Node withStat = parseStatement(moreNode, TokenT.SEMICOLONS, vars, currTokenT, false);
                    if (withStat == null || !withStat.equals(moreNode))
                        return parseError("Error while parsing statement after '>' in 'for' loop");
                } else if (currTokenT.equals(TokenT.SEMICOLONS)) {
                    forNode.addChild(new Node("1", 0));
                } else {
                    Node statNode = parseStatement(forNode, TokenT.SEMICOLONS, vars, currTokenT, false);
                    if (statNode == null || !statNode.equals(forNode))
                        return parseError("Parsing error: expected ';' after controlling expression with '>' in 'for' loop");
                }

                // post-expression
                Node childNode = new Node(forPoint + "for_{", 2);
                currNode.addChild(childNode);
                Node forBodyNode = new Node(forPoint + "for_body", 100);
                childNode.addChild(forBodyNode);
                currTokenT = tokens.getNextType();
                if (currTokenT.equals(TokenT.IDENTIFIER)) {
                    Node withPostExpr = parseIdentifier(vars, false, childNode, TokenT.CLOSE_CAST);
                    if (withPostExpr == null || !withPostExpr.equals(childNode)) {
                        return parseError("Error while parsing 'for' cycle: unexpected statement in 'post-expression' section");
                    }
                } else if (!currTokenT.equals(TokenT.CLOSE_CAST)) {
                    return parseError("Parsing error! Unexpected post-expression in 'for' cycle");
                }
                if (!tokens.getNextType().equals(TokenT.OPEN_BRACE)) {
                    return parseError("Parsing error! Expected '{' after 'for(...)'");
                }
                currNode = forBodyNode;
            }
            else if (currTokenT.equals(TokenT.KEYWORD_BREAK)) {
                currNode.addChild(new Node("BREAK", 0));
                if (!tokens.getNextType().equals(TokenT.SEMICOLONS))
                    return parseError("Parsing error! Unexpected symbol '" + tokens.currVal() + "' after keyword 'break'");
            }
            else if (currTokenT.equals(TokenT.KEYWORD_CONTINUE)) {
                currNode.addChild(new Node("CONTINUE", 0));
                if (!tokens.getNextType().equals(TokenT.SEMICOLONS))
                    return parseError("Parsing error! Unexpected symbol '" + tokens.currVal() + "' after keyword 'continue'");
            }
            else if (currTokenT.equals(TokenT.KEYWORD_IF)) {
                if (!tokens.getNextType().equals(TokenT.OPEN_CAST)) {
                    System.out.println("error: expected '(' after 'if'");
                }

                Node lastNode = new Node("if", 2);
                currNode.addChild(lastNode);

                Node node1 = new Node("if_pop", 2);
                Node statNode = parseStatement(node1, TokenT.CLOSE_CAST, vars, tokens.getNextType(), true);
                if (statNode == null || !statNode.equals(node1))
                    return parseError("Error while parsing statement");
                Node node2 = null, node3;
                boolean shouldHaveConst = false;

                currTokenT = tokens.getNextType(); //< > = or )
                if (currTokenT.equals(TokenT.CLOSE_CAST)) {
                    node2 = new Node("if_with_0", 1);
                    node3 = new Node("if_neq", 1);
                }
                else if (currTokenT.equals(TokenT.LESS_THAN)) {
                    node3 = new Node("if_less", 1);
                    shouldHaveConst = true;
                }
                else if (currTokenT.equals(TokenT.MORE_THAN)) {
                    node3 = new Node("if_more", 1);
                    shouldHaveConst = true;
                }
                else if (currTokenT.equals(TokenT.EQUALS)) {
                    if (!tokens.getNextType().equals(TokenT.EQUALS))
                        return parseError("Parsing error! There is only one '=' in cast after 'if'!");
                    node3 = new Node("if_eq", 1);
                    shouldHaveConst = true;
                } else return parseError("Parsing error occurs while parsing second statement in cast after 'if'!");

                if(shouldHaveConst){
                    currTokenT = tokens.getNextType();
                    String name = tokens.currVal();
                    int index = vars.getVal( name + "_var");

                    if (currTokenT.equals(TokenT.INT_CONSTANT)){
                        node2 = new Node("if_with_" + tokens.currVal(), 1);
                    } else if (index != -1) {
                        index = (index + 3) * 4;
                        node2 = new Node("if_with_[ebp-" + index + "]", 1);
                    }  else return parseError("Parsing error occurs while parsing second statement in cast after 'if'!");
                }

                node2.addChild(node1);
                node3.addChild(node2);
                lastNode.addChild(node3);
                if(!tokens.getNextType().equals(TokenT.CLOSE_CAST)){
                    return parseError("Parsing error occurs in cast after 'if'. There is no close cast");
                }
            }
            else if (currTokenT.equals(TokenT.KEYWORD_ELSE)){
                try {
                    if (!((currNode.getTailChild(1).getValue().equals("{") &&
                            currNode.getTailChild(1).getTailChild(1).getValue().equals("if_else") ||
                            currNode.getTailChild(1).getValue().matches("[a-zA-Z_][a-zA-Z_0-9]*_var")) &&
                            currNode.getTailChild(2).getValue().equals("if"))) {
                        return parseError("Error! 'else' without a previous 'if' or inappropriate value between it!");
                    }

                } catch (IndexOutOfBoundsException e){
                    return parseError("Error! 'else' without a previous 'if' or inappropriate value between it!");
                }
                currNode.addChild(new Node("else", 0));
            }
            else if(currTokenT.equals(TokenT.OPEN_BRACE)) {
                vars = vars.openBrace();
                Node childNode = new Node("{", 100);
                try {
                    if (currNode.getTailChild(1).getValue().equals("else")) {
                        childNode.switchAfterElse();
                    }
                } catch (IndexOutOfBoundsException e){/*ok*/}
                currNode.addChild(childNode);
                currNode = childNode;
            }
            else if(currTokenT.equals(TokenT.CLOSE_BRACE)){
                vars = vars.closeBrace();
                Node childNode;
                String prevVal = currNode.getParent().getTailChild(2).getValue();
                if(prevVal.equals("if")){
                    currTokenT = tokens.getNextType();
                    if(currTokenT.equals(TokenT.KEYWORD_ELSE))
                        childNode = new Node ("if_else", 0);
                    else
                        childNode = new Node ("if_end", 0);
                    tokens.indexMinus(1);
                } else if(prevVal.equals("else")){
                    childNode = new Node ("else_end", 0);
                } else childNode = new Node("}", 0);
                currNode.addChild(childNode);

                if(currNode.getValue().equals("{")){
                    currNode = currNode.getParent();
                } else if(currNode.getValue().matches("[0-9]for_body")){
                    currNode = currNode.getParent().getParent();
                } else return parseError("Error while parsing '{}'. There are '}', but no '{' before it!");
            } else return parseError("Parsing error! Unrecognised symbol '" + currTokenT +"'.");
            currTokenT = tokens.getNextType();
        }

        Node retNode = new Node("return", 2);
        mainNode.addChild(retNode);
        Node newRet = parseStatement(retNode, TokenT.SEMICOLONS, vars, tokens.getNextType(), false);
        if (newRet == null || !newRet.equals(retNode)) {
            return parseError("Error while parsing statement after 'return'");
        }
        return mainNode;
    }

    private Node parseIdentifier(Variables vars, boolean hasType, Node currNode, TokenT lastTokenT){
        String name = tokens.currVal();
        TokenT currTokenT = tokens.getNextType();
        if (currTokenT.equals(TokenT.OPEN_CAST)) {
            if(hasType)
                return parseError("Parsing error: there is an identifier before function call!");

            Node callNode = callFunction(name, currNode, vars);
            if (callNode == null || !callNode.equals(currNode))
                return null;
            if(!tokens.getNextType().equals(TokenT.SEMICOLONS))
                return parseError("Error after parsing call of '" + name + "' function");
        } else {
            boolean canBeDecl = vars.contains(name + "_var");
            boolean canBeInit = vars.totalContains(name + "_var");
            if (hasType) {
                if (canBeDecl) {
                    return parseError("The variable " + name + " is declarated several times!");
                }
                vars.addVar(name + "_var");
            } else if (!canBeInit) {
                return parseError("Variable " + name + " initialized, but not declarated");
            }

            Node var = new Node( name + "_var", 2);
            currNode.addChild(var);
            currNode = var;
            currNode.setPoint(vars.getPoint(name + "_var"));

            if (currTokenT.equals(TokenT.SEMICOLONS)) {
                currNode = currNode.getParent();
            } else if (currTokenT.equals(TokenT.EQUALS)) {
                Node statNode = parseStatement(currNode, lastTokenT, vars, tokens.getNextType(), false);
                if (statNode == null || !statNode.equals(currNode))
                    return parseError("Error while parsing statement");

                if (!vars.addVal(currNode.getValue())) {
                    return parseError("Error while parsing! Variable " + currNode.getValue() + " is not initialised!");
                }
                currNode = currNode.getParent();
            } else if(currTokenT.equals(TokenT.DIVISION)){
                tokens.getNextType();
                Node divNode = new Node("/", 2);
                currNode.addChild(divNode);

                Node childNode = new Node(name + "_val", 0);
                int index = vars.getVal( name + "_var");
                childNode.setPoint(index);
                divNode.addChild(childNode);

                Node dividerNode = new Node("((", 1);
                divNode.addChild(dividerNode);
                Node statNode = parseStatement(dividerNode, TokenT.SEMICOLONS, vars, tokens.getNextType(), false);
                if (statNode == null || !statNode.equals(dividerNode))
                    return parseError("Error while parsing statement");

                if (!vars.addVal(currNode.getValue())) {
                    return parseError("Error while parsing! Variable " + currNode.getValue() + " is not initialised!");
                }
                currNode = currNode.getParent();

            }else return parseError("Error occurred after variable " + name);
        }
        return currNode;
    }

    private Node parseStatement(Node currNode, TokenT stopTokenT, Variables vars, TokenT currTokenT, boolean inIf) {
        EnumSet<TokenT> binaryOp = EnumSet.of(TokenT.DIVISION, TokenT.MULTIPLICATION, TokenT.LOGICAL_AND, TokenT.ADDITION, TokenT.MODULO);
        Node basic = currNode;

        while (!currTokenT.equals(stopTokenT) || currNode.getValue().equals("(")) {
            if (currTokenT.equals(TokenT.NEGATION)) {
                if (currNode.getValue().equals("(") || currNode.equals(basic)) {
                    Node childNode = new Node("-", 1);
                    currNode.addChild(childNode);
                    currNode = childNode;
                } else
                    return parseError("Error while parsing '-': unary operation is without ()");
            }
            else if (binaryOp.contains(currTokenT)) {
                if (currTokenT.equals(TokenT.ADDITION)) {
                    while (currNode.getTailChild(1).getValue().equals("&&") &&
                            !currNode.getValue().equals("(")) {
                        currNode = currNode.getTailChild(1);
                    }
                } else if (currTokenT.equals(TokenT.DIVISION) || currTokenT.equals(TokenT.MODULO)) {
                    while ((currNode.getTailChild(1).getValue().equals("&&") || currNode.getTailChild(1).getValue().equals("+"))&&
                            !currNode.getValue().equals("(")) {
                        currNode = currNode.getTailChild(1);
                    }
                }
                if (currNode.getChildrenCount() != 0) {
                    Node binaryNode = new Node(tokens.currVal(), 2);
                    Node child = currNode.removeLastChild();
                    binaryNode.addChild(child);
                    currNode.addChild(binaryNode);
                    currNode = binaryNode;
                } else return parseError("Invalid binary operation syntax");
            }
            else if (currTokenT.equals(TokenT.OPEN_CAST)) {
                Node childNode = new Node("(", 1);
                currNode.addChild(childNode);
                currNode = childNode;
            }
            else if (currTokenT.equals(TokenT.CLOSE_CAST)) {
                try {
                    while (!currNode.getValue().equals("(")) {
                        currNode = currNode.getParent();
                    }
                } catch (NullPointerException e) {
                    if(stopTokenT.equals(TokenT.COMMA)) {
                        tokens.indexMinus(1);
                        return basic;
                    }
                    else return parseError("Error while parsing '()' There are ')', but no '(' before it!");
                }
                if (currNode.hasMaxChildren()) {
                    Node parent = currNode.getParent();
                    parent.replaceChild(currNode, currNode.getTailChild(1));
                    currNode = parent;
                }
            }
            else if(inIf && (currTokenT.equals(TokenT.EQUALS) || currTokenT.equals(TokenT.LESS_THAN) || currTokenT.equals(TokenT.MORE_THAN))){
                if(!currNode.equals(basic)){
                    return parseError("Error while parsing statement in 'if' condition! There is open cast  one '=' symbol.");
                }
                tokens.indexMinus(1);
                return currNode;
            }
            else {
                currNode = parseValue(currNode, currTokenT, vars);
                if(currNode == null) return null;
            }
            try {
                while (currNode.hasMaxChildren() && !(currNode.getValue().equals("(")  ||
                        currNode.getValue().equals("((") || currNode.getValue().equals("if"))) {
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
        String name = tokens.currVal();
        int index = vars.getVal( name + "_var");
        Node childNode;
        if (currToken.equals(TokenT.INT_CONSTANT)) {
            if (!(tokens.getNextType().equals(TokenT.DOT) & tokens.getNextType().equals(TokenT.INT_CONSTANT))) {
                tokens.indexMinus(2);
            }
            childNode = new Node(name, 0);
            currNode.addChild(childNode);
        } else if (currToken.equals(TokenT.INT_BIN_CONSTANT)) {
            String val = name.substring(1);
            val = Integer.toString(Integer.parseInt(val, 2));
            childNode = new Node(val, 0);
            currNode.addChild(childNode);
        }
        else if (index != -1) {
            String val =  tokens.currVal() + "_val";
            childNode = new Node(val, 0);
            childNode.setPoint(index);
            currNode.addChild(childNode);
        } else if(functions.containsFunc(name) && tokens.getNextType().equals(TokenT.OPEN_CAST)){
            Node callNode = callFunction(name, currNode, vars);
            if (callNode == null || !callNode.equals(currNode))
                return null;
        }
        else return parseError("Error while parsing value " + tokens.currVal());

        return currNode;
    }

    //----------------------------------------------------Generation--------------------------------------------------

    public String generator(Node node) {
        node.setMainLast();
        System.out.println("---------Generation started---------");
        StringBuilder code = new StringBuilder();
        code.append(".386\n")
                .append(".model flat, stdcall\n")
                .append("option casemap :none\n\n")

                .append("include C:\\masm32\\include\\kernel32.inc\n")
                .append("include C:\\masm32\\include\\user32.inc\n\n")

                .append("includelib C:\\masm32\\lib\\kernel32.lib\n")
                .append("includelib C:\\masm32\\lib\\user32.lib\n\n")

                .append("main PROTO\n\n")

                .append(".data\n")
                .append("msg_title db \"Result\", 0\n")
                .append("buffer db 128 dup(?)\n")
                .append("format db \"%d\",0\n\n")

                .append(".code\n")
                .append("start:\n")
                .append("\tinvoke main")
                .append("\n\tinvoke wsprintf, addr buffer, addr format, eax\n")
                .append("\tinvoke MessageBox, 0, addr buffer, addr msg_title, 0\n")
                .append("\tinvoke ExitProcess, 0\n\n");

        boolean success = Node.startGenerate(node, code);
        if(!success) return null;

        code.append("main ENDP\n")
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
    KEYWORD_FOR("for"),
    KEYWORD_BREAK("break"),
    KEYWORD_CONTINUE("continue"),
    INT_CONSTANT("[0-9]+"),
    INT_BIN_CONSTANT("b[01]+\\b"),
    IDENTIFIER("[a-zA-Z_][a-zA-Z_0-9]*"),
    OPEN_CAST("\\("),
    DIVIDE_ASSIGN("/="),
    OPEN_BRACE("\\{"),
    CLOSE_CAST("\\)"),
    CLOSE_BRACE("}"),
    EQUALS("="),
    MORE_THAN(">"),
    LESS_THAN("<"),
    BITWISE_COMPLEMENT("~"),
    LOGICAL_AND("&&"),
    LOGICAL_NEGATION("!"),
    ADDITION("\\+"),
    NEGATION("-"),
    MULTIPLICATION("\\*"),
    DIVISION("/"),
    MODULO("%"),
    BINARY_NOT("%"),
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
        System.out.print("" + types.get(index) + ", ");
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

    public boolean hasNext(){
        return types.size() > index + 1;
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
    private static int andCount = 0;
    private boolean isParam = false;
    private static int curr_param_count = 0;
    private static int forCycleCount = 0;

    public Node(String value, int maxChildren) {
        this.children = new ArrayList<>(1);
        this.index = 0;
        this.childrenCount = 0;
        this.value = value;
        this.maxChildren = maxChildren;
    }

    private boolean codeGenerate(StringBuilder code) {
        if (value.equals("return")) {
            code.append("\tpop eax ;here is the result\n")
                    .append("\tmov esp, ebp\t;restore ESP\n")
                    .append("\tpop ebp\t;restore old EBP\n")
                    .append("\tret " + curr_param_count + "\n\n");
        }
        else if(value.equals("for")){
            forCycleCount ++;
        }
        else if(value.equals("CONTINUE")){
            if(forCycleCount < 1){
                System.out.println("Error while generation code! Keyword 'continue' is out of the cycle!");
                return false;
            }
            code.append("jmp _for_start" + forCycleCount + "\t;continue\n");
        }else if(value.equals("BREAK")){
            if(forCycleCount < 1){
                System.out.println("Error while generation code! Keyword 'break' is out of the cycle!");
                return false;
            }
            code.append("jmp _for_end" + forCycleCount + "\t;break\n");
        }
        else if(value.matches("for_start[0-9]")){
            code.append("_" + value + ":\n");
        }
        else if(value.matches("[0-9]for_\\{")){
            forCycleCount --;
            code.append("\tjmp _for_start" + value.charAt(0) + "\n");
            code.append("_for_end" + value.charAt(0) + ":\n");
        }
        else if(value.matches("less[0-9]")){
            code.append("\tpop eax\t;expression\n")
                    .append("\tcmp eax, ebx\n")
                    .append("\tjle _for_end" + value.charAt(4) + "\t;if value less than expr\n\n");
        }
        else if(value.matches("more[0-9]")){
            code.append("\tpop eax\t;expression\n")
                    .append("\tcmp ebx, eax\n")
                    .append("\tjle _for_end" + value.charAt(4) + "\t;if value more than expr\n\n");
        }
        else if(value.equals("minusOne")){
            code.append("\tsub ebx, 1\n");
        }
        else if(value.equals("plusOne")){
            code.append("\tadd ebx, 1\n");
        }
        else if (value.equals("&&")) {
            andCount ++;
            code.append("\tpop ECX\n")
                    .append("\tpop EAX\n")
                    .append("\tcmp eax, 0   ; check if e1 is true\n")
                    .append("\tjne _clause" + andCount + "\t;e1 is not 0, evaluate clause 2\n")
                    .append("\tjmp _end_and" + andCount + "\n")
                    .append("\t_clause" + andCount + ":\n")
                    .append("\t\tcmp ecx, 0 ; check if e2 is true\n")
                    .append("\t\tmov eax, 0\n")
                    .append("\t\tsetne al\n\n")

                    .append("\t_end_and" + andCount + ":\n")
                    .append("\t\tpush eax\n\n");

        }
        else if (value.equals("*")) {

            code.append("\tmov edx, 0\n")
                    .append("\tpop ECX\n")
                    .append("\tpop EAX\n")
                    .append("\timul ECX\n")
                    .append("\tpush EAX\n\n");

        }
        else if (value.equals("%")) {

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
                    .append("\tpush EDX\n\n");
            divCount += 2;
        }
        else if (value.equals("/")) {

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
        else if (value.equals("-")) {

            code.append("\tpop EBX\n")
                    .append("\tneg EBX\n")
                    .append("\tpush EBX\n\n");

        }
        else if (value.equals("+")) {

            code.append("\tpop EAX\n")
                    .append("\tpop EBX\n")
                    .append("\tadd EAX, EBX\n")
                    .append("\tpush EAX\n");

        }
        else if (value.equals("if_pop")) {
            loopCount += 2;
            code.append("pop eax\t;if\n" +
                    "cmp eax, ");
        }
        else if (value.matches("if_with_[0-9]+") || value.matches("if_with_\\[ebp-[0-9]+]")) {
            code.append(value.substring(8) + "\n");
        }
        else if (value.equals("if_eq")) {
            code.append("jne _L" + loopCount + "\n\n");
        }
        else if (value.equals("if_less")) {
            code.append("jge _L" + loopCount + "\n\n");
        }
        else if (value.equals("if_more")) {
            code.append("jle _L" + loopCount + "\n\n");
        }
        else if (value.equals("if_neq")) {
            code.append("je _L" + loopCount + "\n\n");
        }
        else if (value.equals("if_else")) {
            code.append("\tjmp _L" + (loopCount + 1) + "\n_L" + loopCount + ":\n");
        }
        else if (value.equals("if_end")) {
            code.append("_L" + loopCount + ":\n");
        }
        else if (value.equals("else_end")) {
            code.append("_L" + (loopCount + 1) + ":\n");
        }
        else if (value.matches("[0-9]+")) {
            code.append("\tpush ").append(value).append("\n");
        }
        else if (value.matches("[a-zA-Z_][a-zA-Z_0-9]*_var")) {
            if (this.childrenCount > 0) {
                code.append("\tpop " + "[ebp-").append(point).append("];\t" + value +"\n");
            }
            if(isParam){
                code.append("\tmov eax, [ebp+" + (point-4) + "]\n");
                code.append("\tmov [ebp-" + point + "], eax\t;" + value + "\n");
            }
        }
        else if (value.matches("[a-zA-Z_][a-zA-Z_0-9]*_val")) {
            code.append("\tpush [ebp-").append(point).append("]     ;").append(value).append("\n");
        }
        else if (value.matches("[a-zA-Z_][a-zA-Z_0-9]*_val_for")) {
            code.append("\tmov ebx, [ebp-" + point + "]\n");
        }
//        else if(afterElse && value.matches("\\{")) {
//            code.append("_L" +  (loopCount +1) + ":\n");
//        }
        else if(value.matches("[a-zA-Z_][a-zA-Z_0-9]*_name")){
            if(value.matches("main_name"))
                curr_param_count = 0;
            else {
                ArrayList<Node> params = parent.children.get(1).children;
                Collections.reverse(params);
            }
            code.append(value.substring(0, value.lastIndexOf("_")) + " proc\n")
                    .append("\tpush ebp\n\tmov ebp, esp\n");
        }
        else if(value.matches("[a-zA-Z_][a-zA-Z_0-9]*_call")){
            code.append("call " + value.substring(0, value.lastIndexOf("_")) + "\n")
                    .append("\tpush eax\n");

        }
        else if(value.matches("[a-zA-Z_][a-zA-Z_0-9]*_body")){
            code.append(value.substring(0, value.lastIndexOf("_")) + " endp\n");
        }
        else if(value.matches("[a-zA-Z_][a-zA-Z_0-9]*_param")){
            curr_param_count = this.children.size();
            code.append(";" + value + " \n");
        }
        return true;
    }
    public void isParam(){
        isParam = true;
    }
    public int getChildrenCount(){
        return childrenCount;
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
    public boolean hasMaxChildren() {
        return maxChildren == childrenCount;
    }
    public void replaceChild(Node child, Node newChild) {
        int i = this.children.indexOf(child);
        this.children.set(i, newChild);
        newChild.setParent(this);
    }
    public void setMainLast(){
        for (Node childNode :
                children) {
            if(childNode.getValue().equals("main_func")) {
                children.remove(childNode);
                children.add(childNode);
                return;
            }
        }
    }
    public void switchAfterElse(){
        afterElse = !afterElse;
    }
    public Node removeLastChild() {
        childrenCount--;
        return this.children.remove(childrenCount);
    }
    public void setPoint(int val){
        point = (val + 3) * 4;
    }
    public Node getTailChild(int place) {
        int index = this.children.size() - place;
        return this.children.get(index);
    }

    public static boolean startGenerate(Node node, StringBuilder code){
        Node curr = node;
        boolean done = false;
        while (!done) {
            if (curr.hasNextChild()) {
                curr = curr.getChild();
            } else {
                if(!curr.codeGenerate(code)) return false;
                if (curr == node) done = true;
                else curr = curr.getParent();
            }
        }
        return true;
    }
    private Node getChild() {
        Node child = this.children.get(this.index);
        index++;
        return child;
    }
    private boolean hasNextChild() {
        return this.index < this.children.size();
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

    public Variables skipCurBrace(int count){
        int varIndex = variables.size() - 1;
        for (int i = 0; i < count; i++) {
            variables.remove(varIndex);
            varIndex --;
        }
        int index = this.parrent.children.size() - 1;
        this.parrent.children.remove(index);
        return this.parrent;
    }

    public Variables closeBrace(){
        for (int i = 0; i < varList.size(); i++) {
            variables.remove(variables.size() - 1);
        }
        return this.parrent;
    }
}

class Functions{
    private final Map<String, Integer> allFunc = new HashMap<>();
    private final ArrayList<String> defNames = new ArrayList<>();//has a body
    private final ArrayList<String> activatedFunc = new ArrayList<>();//has a body

    public boolean foundFunc(String name, int arguments){
        if(allFunc.containsKey(name)){
            return allFunc.get(name) == arguments;
        }
        else {
            allFunc.put(name, arguments);
            return true;
        }
    }

    public boolean functionBody(String name, int arguments){
        if(defNames.contains(name))
            return false;
        else
            return defNames.add(name);
    }

    public boolean containsFunc(String name){
        return allFunc.containsKey(name);
    }

    public boolean activateFunction(String name, int arguments){
        activatedFunc.add(name);
        return allFunc.containsKey(name) && allFunc.get(name) == arguments;
    }

    public boolean containsMain() {
        return defNames.contains("main");
    }

    public String finalCheck() {
        for (String currName : activatedFunc) {
            if (!defNames.contains(currName)) {
                return currName;
            }
        }
        return null;
    }
}