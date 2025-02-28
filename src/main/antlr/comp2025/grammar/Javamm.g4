grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
BOOLEAN : 'boolean' ;
TRUE : 'true';
FALSE : 'false';
STATIC : 'static';
VOID : 'void';
MAIN : 'main';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
NEW : 'new';
THIS : 'this';
LENGTH : 'length';

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z$_][a-zA-Z0-9$_]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : 'import' ID ('.' ID)* ';'
    ;

classDecl
  : CLASS ID ('extends' ID )?
        '{'
        (varDecl)* (methodDecl)*
        '}'
    ;

varDecl
    : type name=ID ';'
    ;

type
    : INT '[' ']'
    | INT '...'
    | INT
    | BOOLEAN
    | ID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
      type name=ID
      '(' (param (',' param)*)? ')'
      '{' (varDecl)* (stmt)* 'return' expr ';' '}'
    | (PUBLIC {$isPublic=true;})?
      STATIC VOID MAIN
      '(' ID '[' ']' ID ')'
      '{' (varDecl)* (stmt)* '}'
    ;

param
    : type name=ID
    ;

stmt
    : '{' (stmt)* '}'
    | IF '(' expr ')' stmt (ELSE stmt)?
    | WHILE '(' expr ')' stmt
    | expr ';'
    | ID '=' expr ';'
    | ID '[' expr ']' '=' expr ';'
    ;

expr
    : expr ('*'|'/') expr
    | expr ('+'|'-') expr
    | expr ('&&'|'<') expr
    | expr '[' expr ']'
    | expr '.' LENGTH
    | expr '.' ID '(' (expr (',' expr)*)? ')'
    | NEW 'int' '[' expr ']'
    | NEW ID '(' ')'
    | '!' expr
    | '(' expr ')'
    | '[' (expr (',' expr)*)? ']'
    | INTEGER
    | TRUE
    | FALSE
    | ID
    | THIS
    ;

