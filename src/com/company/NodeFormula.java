package com.company;

public class NodeFormula { // node in cnf
    TypeOperation operation = TypeOperation.undefined;
    int var; // variable id
    String varName;
    NodeFormula left;
    NodeFormula right;
    String addVar; // additional variable
};