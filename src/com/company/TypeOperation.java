package com.company;

public enum TypeOperation {
    conjunction, // &&
    disjunction, // ||
    variable, // anything else
    undefined, // ?
    not, // !
    implication, // ->
    equivalence, // <->
    xor, // +
    open, // (
    close, // )
}