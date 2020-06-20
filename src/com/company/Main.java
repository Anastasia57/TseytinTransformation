package com.company;

import java.io.*;

public class Main {
    // if TypeOperation is an operation? not a variable
    public static boolean isOperation(TypeOperation t){
        return ((t != TypeOperation.undefined) &&
                (t != TypeOperation.open) &&
                (t != TypeOperation.close) &&
                (t != TypeOperation.variable));
    }

    // String representation to TypeOperation
    public static TypeOperation getOpType(String str){
        if(str.equals("")){return TypeOperation.undefined;}
        if(str.equals("&&")){return TypeOperation.conjunction;}
        if(str.equals("||")){ return TypeOperation.disjunction;}
        if(str.equals("!")){return TypeOperation.not;}
        if(str.equals("->")){return TypeOperation.implication;}
        if(str.equals("<->")){return TypeOperation.equivalence;}
        if(str.equals("(")){return TypeOperation.open;}
        if(str.equals(")")){return TypeOperation.close;}
        if(str.equals("+")){return TypeOperation.xor;}
        return TypeOperation.variable;
    }

    /*public static String getOpSymbol(TypeOperation t){
        if(t == TypeOperation.undefined){return "";}
        if(t == TypeOperation.conjunction){return "&&";}
        if(t == TypeOperation.disjunction){ return "||";}
        if(t == TypeOperation.not){return "!";}
        if(t == TypeOperation.implication){return "->";}
        if(t == TypeOperation.equivalence){return "<->";}
        if(t == TypeOperation.open){return "(";}
        if(t == TypeOperation.close){return ")";}
        if(t == TypeOperation.variable){return "";}
        if(t == TypeOperation.xor){return "";}
        return "";
    }*/

    // creates formula tree from a split text formula
    public static int makeTree(String[] formula, int start, int stop, NodeFormula root){
        int pos = start;
        if(stop - start <= 0){
            return 1;
        }
        if(stop - start == 1){
            if(getOpType(formula[pos]) == TypeOperation.variable){
                root.operation = TypeOperation.variable;
                root.varName = formula[pos];
                return 0;
            } else {
                System.out.println("Error occurred");
                return 2;
            }
        }
        if(getOpType(formula[start]) == TypeOperation.not){
            root.operation =  getOpType(formula[pos]);
            root.left = new NodeFormula();
            root.right = null;
            makeTree(formula, start + 1, stop, root.left);
            return 0;
        }
        int openCnt = 0;
        int closeCnt = 0;
        for(; pos < stop; ++pos){
            TypeOperation op = getOpType(formula[pos]);
            if(op == TypeOperation.open){
                ++openCnt;
            }
            if(op == TypeOperation.close){
                ++closeCnt;
            }
            if(isOperation(op)){
                if(openCnt == closeCnt){
                    root.operation = op;
                    root.left = new NodeFormula();
                    root.right = new NodeFormula();
                    int a = makeTree(formula, start, pos, root.left);
                    if(a != 0){
                        root.left = null;
                    }
                    if(a == 2){
                        return 2;
                    }
                    a = makeTree(formula, pos + 1, stop, root.right);
                    if(a != 0){
                        root.right = null;
                    }
                    if(a == 2){
                        return 2;
                    }
                    return 0;
                }
            }
        }
        if(openCnt == closeCnt && getOpType(formula[start]) == TypeOperation.open &&
                getOpType(formula[stop-1]) == TypeOperation.close){
            makeTree(formula, start + 1, stop - 1, root);
        }
        if(openCnt != closeCnt){
            System.out.println("Error occurred: Parentheses don't match.");
            return 2;
        }
        return 0;
    }

    // creates additional variables, which are needed for transformation
    public static int addVariables(NodeFormula root, int start){
        if(root == null){
            return start;
        }
        if(isOperation(root.operation)){
            root.addVar = "added" + start;
            ++start;
        }
        int left = addVariables(root.left, start);
        return addVariables(root.right, left);
    }
    // writes formula tree, just for checking
    static void TsTreeWalk(NodeFormula root, FileWriter Writer, int shift) throws IOException {
        if(root == null){
            return;
        }
        for(int i = 0; i < shift; ++i){
            Writer.write(" ");
        }
        Writer.write("operation = " + root.operation + " addVar = " + root.addVar + " varName = " + root.varName + "\n");
        for(int i = 0; i <shift; ++i){
            Writer.write(" ");
        }
        Writer.write("left:\n");
        TsTreeWalk(root.left, Writer, shift + 4);
        for(int i = 0; i <shift; ++i){
            Writer.write(" ");
        }
        Writer.write("right:\n");
        TsTreeWalk(root.right, Writer, shift + 4);
    }

    /*static void writeFormula(NodeFormula root, FileWriter Writer, int shift) throws IOException {
        if(root == null){
            return;
        }
        for(int i = 0; i <shift; ++i){
            Writer.write(" ");
        }
        Writer.write("operation = " + root.operation + " addVar = " + root.addVar + " varName = " + root.varName + "\n");
        for(int i = 0; i <shift; ++i){
            Writer.write(" ");
        }
        Writer.write("left:\n");
        treeWalk(root.left, Writer, shift + 4);
        for(int i = 0; i <shift; ++i){
            Writer.write(" ");
        }
        Writer.write("right:\n");
        treeWalk(root.right, Writer, shift + 4);
    }*/

    // these functions add CNF clauses(according to possible types of operations) to a new tree for CNF

    static void transformCon(NodeFormula newRoot, String C, String A, String B){
        String newFormula = "( ( ! " + A + " ) || ( ! " + B + " ) || " + C +
                " ) && ( " + A + " || ( ! " + C +
                " ) ) && ( " + B + " || ( ! " + C + " ) )";
        //System.out.println(newFormula);
        String[] str = newFormula.trim().split("\\s+");
        makeTree(str, 0, str.length, newRoot);
    }
    static void transformDis(NodeFormula newRoot, String C, String A, String B){
        String newFormula = "( " + A + " || " + B + " || ( ! " + C +
                " ) ) && ( ( ! " + A + " ) || " + C +
                " ) && ( ( ! " + B + " ) || " + C + " )";
        //System.out.println(newFormula);
        String[] str = newFormula.trim().split("\\s+");
        makeTree(str, 0, str.length, newRoot);
    }
    static void transformNot(NodeFormula newRoot, String C, String A){
        String newFormula = "( ( ! " + A + " ) || ( ! " + C +
                " ) ) && ( " + A + " || " + C + " )";
        //System.out.println(newFormula);
        String[] str = newFormula.trim().split("\\s+");
        makeTree(str, 0, str.length, newRoot);
    }
    static void transformImpl(NodeFormula newRoot, String C, String A, String B){
        String newFormula = "( " + A + " || " + B + " || " + C +
                " ) && ( " + A + " || ( ! " + B + " ) || " + C +
                " ) && ( ( ! " + A + " ) || ( ! " + B + " ) || " + C +
                " ) &&  ( ( ! " + A + " ) || ( ! " + B + " ) || ( ! " + C +
                " ) )";
        //System.out.println(newFormula);
        String[] str = newFormula.trim().split("\\s+");
        makeTree(str, 0, str.length, newRoot);
    }
    static void transformEquiv(NodeFormula newRoot, String C, String A, String B){
        String newFormula = "( " + A + " || ( ! " + B + " ) || ( ! " + C +
                " ) ) && ( ( ! " + A + " ) || " + B + " || ( ! " + C +
                " ) ) && ( ( ! " + A + " ) || ( ! " + B + " ) || " + C +
                " ) && ( " + A + " || " + B + " || " + C +
                " )";
        //System.out.println(newFormula);
        String[] str = newFormula.trim().split("\\s+");
        makeTree(str, 0, str.length, newRoot);
    }
    static void transformXor(NodeFormula newRoot, String C, String A, String B){
        String newFormula = "( ( ! " + A + " ) || ( ! " + B + " ) || ( ! " + C +
                " ) ) && ( " + A + " || " + B + " || ( ! " + C +
                " ) ) && ( " + A + " || ( ! " + B + " ) || " + C +
                " ) && ( ( ! " + A + " ) || " + B + " || " + C +
                " )";
        //System.out.println(newFormula);
        String[] str = newFormula.trim().split("\\s+");
        makeTree(str, 0, str.length, newRoot);
    }

    // gets a place for a new node in formula tree
    static NodeFormula findPlace(NodeFormula root){
        if(root == null){
            root = new NodeFormula();
            root.operation = TypeOperation.conjunction;
            return root;
        }
        if(root.left == null){
            root.left = new NodeFormula();
            root.left.operation = TypeOperation.conjunction;
            return root.left;
        } else if(root.right == null){
            root.right = new NodeFormula();
            root.right.operation = TypeOperation.conjunction;
            return root.right;
        } else {
            NodeFormula curr = root.left;
            root.left = new NodeFormula();
            root.left.left = curr;
            root.left.operation = TypeOperation.conjunction;
            root.left.right = new NodeFormula();
            root.left.right.operation = TypeOperation.conjunction;
            return root.left.right;
        }
    }

    // calls transforming functions for all operation nodes according to type
    public static void transformToCNF(NodeFormula root, NodeFormula newRoot){
        if((root == null) || (root.operation == TypeOperation.variable)){
            return;
        }
        if(!isOperation(root.operation)){
            return;
        }
        if((root.left == null) && (root.right == null)){
            System.out.println("here");
            return;
        }
        NodeFormula place = findPlace(newRoot);
        String C = root.addVar;
        String A , B ="";
        TypeOperation t = root.operation;
        if(root.left.operation == TypeOperation.variable){
            A = root.left.varName;
        } else {
            A = root.left.addVar;
        }
        if(t != TypeOperation.not){
            if(root.right.operation == TypeOperation.variable){
                B = root.right.varName;
            } else {
                B = root.right.addVar;
            }
        }
        if(t == TypeOperation.conjunction){transformCon(place, C, A, B);}
        if(t == TypeOperation.disjunction){transformDis(place, C, A, B);}
        if(t == TypeOperation.not){transformNot(place, C, A);}
        if(t == TypeOperation.implication){transformImpl(place, C, A, B);}
        if(t == TypeOperation.equivalence){transformEquiv(place, C, A, B);}
        if(t == TypeOperation.xor){transformXor(place, C, A, B);}
        transformToCNF(root.right, newRoot);
        transformToCNF(root.left, newRoot);
    }

    // gets in arguments the path to the file containing boolean formula, the formula must be written according to format to work correctly
    // All operations and variables must be divided by whitespace, (! A && B ) is not ( ! A ) && B,  (! A && B) is  ! ( A && B )
    public static void main(String[] args){
        try {
            String filePath;
            filePath = args[0];
            File file = new File(filePath);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            String line;
            while((line=br.readLine())!=null){
                sb.append(line);
                sb.append(" ");
            }
            fr.close();
            String str = sb.toString();
            String[] split = str.trim().split("\\s+");
            NodeFormula root = new NodeFormula();
            makeTree(split, 0, split.length, root);
            addVariables(root, 0);
            FileWriter Writer = new FileWriter("treeFile.txt");
            TsTreeWalk(root, Writer, 0);
            Writer.close();

            NodeFormula place = new NodeFormula();
            place.operation = TypeOperation.conjunction;
            transformToCNF(root, place);
            FileWriter WriterNew = new FileWriter("newTreeFile.txt");
            TsTreeWalk(place, WriterNew, 0);
            WriterNew.close();

        }
        catch(IOException e){
            e.printStackTrace();
        }

    }
}
