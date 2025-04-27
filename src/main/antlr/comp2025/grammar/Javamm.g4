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
IF : 'if';
ELSE : 'else';
WHILE : 'while';
FOR : 'for';
ELSEIF : 'else if';
NEW : 'new';
THIS : 'this';

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z$_][a-zA-Z0-9$_]* ;

WS : [ \t\n\r\f]+ -> skip ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

STRING : '"' (~["\r\n])* '"' ;

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
    | type name=ID op='[' op=']' ';'
    ;

type locals[boolean isArray=false]
    : name=INT '[' ']' { $isArray = true; }
    | name=INT '...' { $isArray = true; }
    | name=INT
    | name=BOOLEAN
    | name=ID '[' ']' { $isArray = true; }
    | name=ID
    ;

methodDecl locals[boolean isPublic=false, boolean isMain=false]
    : (PUBLIC {$isPublic=true;})?
      type name=ID
      '(' (param (',' param)*)? ')'
      '{' (varDecl)* (stmt)* '}'
    | (PUBLIC {$isPublic=true; $isMain=true;})?
      STATIC VOID name=ID
      '(' string=ID '[' ']' args=ID ')'
      '{' (varDecl)* (stmt)* '}'
    ;


param
    : type name=ID
    ;

stmt
    : expr ';'                        # ExprStmt
    | 'return' expr? ';'              # ReturnStmt
    | '{' (stmt)* '}'                 # BlockStmt
    | IF '(' expr ')' stmt (ELSEIF '(' expr ')' stmt )* (ELSE stmt)?  # IfStmt
    | FOR '(' stmt expr ';' expr ')' stmt  # ForStmt
    | WHILE '(' expr ')' stmt         # WhileStmt
    | name=ID '=' expr ';'            # AssignStmt
    | name=ID '[' expr ']' '=' expr ';'  # ArrayAssignStmt
    ;

expr
    : ('++' | '--') expr                            # Increment
    | '(' expr ')'                                  # Primary
    | '[' (expr (',' expr)*)? ']'                   # ArrayLiteral
    | '!' expr                                      # UnaryOp
    | NEW 'int' '[' expr ']'                        # NewArray
    | NEW name=ID '(' ')'                           # NewObject
    | expr '.' length=ID                            # LengthAccess
    | expr '.' name=ID '(' (expr (',' expr)*)? ')'  # MethodCall
    | expr '[' expr ']'                             # ArrayAccess
    | expr op=('*' | '/') expr                      # BinaryOp
    | expr op=('+' | '-') expr                      # BinaryOp
    | expr op=('<' | '>') expr                      #BinaryOp
    | expr op=('<=' | '>=' | '==' | '!=' | '+=' | '-=' | '*=' | '/=') expr #BinaryOp
    | expr op='&&' expr                             # BinaryOp
    | expr op='||' expr                             # BinaryOp
    | value=INTEGER                                 # Literal
    | value=TRUE                                    # Literal
    | value=FALSE                                   # Literal
    | value=STRING                                  # Literal
    | name=ID                                       # Identifier
    | expr ('++' | '--')                            # Increment
    | THIS                                          # ThisReference
    ;
