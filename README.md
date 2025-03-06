# Compiler Project

Contains a reference implementation for the compiler project.

## Members - Group 2A
- Afonso Castro (up202208026)
- Gonçalo Ferros (up202207592)
- Leonor Couto (up202205796)

## Checklist
**The Java-- Language**
- [x] Complete the Java-- grammar in ANTLR format  
    - Import declarations
    - Class declaration (structure, fields and methods)
    - Statements (assignments, if-else, while, etc.)
    - Expressions (binary expressions, literals, method calls, etc.)
- [ ] Setup node names for the AST (e.g. “binaryOp” instead of “expr” for binary expressions)
- [ ] Annotate nodes in the AST with relevant information (e.g. id, values, etc.)
- [x] Used interfaces: JmmParser, JmmNode and JmmParserResult

**Symbol Table**
- [ ] Imported classes
- [ ] Declared class
- [ ] Fields inside the declared class
- [ ] Methods inside the declared class
- [ ] Parameters and return type for each method
- [ ] Local variables for each method
- [ ] Include type in each symbol (e.g. a local variable “a” is of type X. Also, is “a” array?)
- [ ] Used interfaces: SymbolTable, AJmmVisitor (the latter is optional)

**Types and Declarations Verification**
- [ ] Verify if identifiers used in the code have a corresponding declaration, either as a local variable,
a method parameter, a field of the class or an imported class
- [ ] Operands of an operation must have types compatible with the operation (e.g. int + boolean
is an error because + expects two integers.)
- [ ] Array cannot be used in arithmetic operations (e.g. array1 + array2 is an error)
- [ ] Array access is done over an array
- [ ] Array access index is an expression of type integer
- [ ] Type of the assignee must be compatible with the assigned (an_int = a_bool is an error)
- [ ] Expressions in conditions must return a boolean (if(2+3) is an error)
- [ ] “this” expression cannot be used in a static method
- [ ] “this” can be used as an “object” (e.g. A a; a = this; is correct if the declared class is A or
the declared class extends A)
- [ ] A vararg type when used, must always be the type of the last parameter in a method declaration. Also, only one parameter can be vararg, but the method can have several parameters
- [ ] Variable declarations, field declarations and method returns cannot be vararg
- [ ] Array initializer (e.g., [1, 2, 3]) can be used in all places (i.e., expressions) that can accept
an array of integers

**Method Verification**
- [ ] When calling methods of the class declared in the code, verify if the types of arguments of the
call are compatible with the types in the method declaration
- [ ] If the calling method accepts varargs, it can accept both a variable number of arguments of
the same type as an array, or directly an array
- [ ] In case the method does not exist, verify if the class extends an imported class and report an
error if it does not.
– If the class extends another class, assume the method exists in one of the super classes,
and that is being correctly called
- [ ] When calling methods that belong to other classes other than the class declared in the code,
verify if the classes are being imported.
  - As explained in Section 1.2, if a class is being imported, assume the types of the expression
  where it is used are correct. For instance, for the code bool a; a = M.foo();, if M is an
  imported class, then assume it has a method named foo without parameters that returns
  a boolean.
