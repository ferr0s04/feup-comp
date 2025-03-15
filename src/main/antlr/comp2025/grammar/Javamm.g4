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

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : 'import' name+=ID ('.' name+=ID)* ';'
    ;

classDecl
  : CLASS name=ID ('extends' extended=ID )?
        '{'
        (varDecl)* (methodDecl)*
        '}'
    ;

varDecl
    : type name=ID ';'
    ;

type locals[boolean isArray=false]
    : value=INT '[' ']' { $isArray = true; }
    | value=INT '...' { $isArray = true; }
    | value=INT
    | value=BOOLEAN
    | name=ID '[' ']' { $isArray = true; }
    | name=ID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
      type name=ID
      '(' (param (',' param)*)? ')'
      '{' (varDecl)* (stmt)* 'return' expr ';' '}'
    | (PUBLIC {$isPublic=true;})?
      STATIC VOID name=MAIN
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
    | name=ID '=' expr ';'
    | name=ID '[' expr ']' '=' expr ';'
    ;

expr
    : '(' expr ')'                                  # Primary
    | '[' (expr (',' expr)*)? ']'                   # ArrayLiteral
    | '!' expr                                      # UnaryOp
    | NEW 'int' '[' expr ']'                        # Primary
    | NEW name=ID '(' ')'                           # NewObject
    | expr '.' LENGTH                               # Primary
    | expr '.' name=ID '(' (expr (',' expr)*)? ')'  # AccessOrCall
    | expr '[' expr ']'                             # AccessOrCall
    | expr op=('*' | '/') expr                      # BinaryOp
    | expr op=('+' | '-') expr                      # BinaryOp
    | expr op='<' expr                              # BinaryOp
    | expr op='&&' expr                             # BinaryOp
    | value=INTEGER                                 # Literal
    | value=TRUE                                    # Literal
    | value=FALSE                                   # Literal
    | name=ID                                       # Identifier
    | THIS                                          # ThisReference
    ;


