/**
 * Checker.java   
 * Sun Apr 26 13:41:38 AEST 2015
 **/

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Checker implements Visitor {

	private String errMesg[] = {
			"*0: main function is missing",                            
			"*1: return type of main is not int",

			// defined occurrences of identifiers
			// for global, local and parameters
			"*2: identifier redeclared",                             
			"*3: identifier declared void",                         
			"*4: identifier declared void[]",                      

			// applied occurrences of identifiers
			"*5: identifier undeclared",                          

			// assignments
			"*6: incompatible type for =",                       
			"*7: invalid lvalue in assignment",                 

			// types for expressions 
			"*8: incompatible type for return",                
			"*9: incompatible type for this binary operator", 
			"*10: incompatible type for this unary operator",

			// scalars
			"*11: attempt to use an array/fuction as a scalar", 

			// arrays
			"*12: attempt to use a scalar/function as an array",
			"*13: wrong type for element in array initialiser",
			"*14: invalid initialiser: array initialiser for scalar",   
			"*15: invalid initialiser: scalar initialiser for array",  
			"*16: excess elements in array initialiser",              
			"*17: array subscript is not an integer",                
			"*18: array size missing",                              

			// functions
			"*19: attempt to reference a scalar/array as a function",

			// conditional expressions in if, for and while
			"*20: if conditional is not boolean",                    
			"*21: for conditional is not boolean",                  
			"*22: while conditional is not boolean",               

			// break and continue
			"*23: break must be in a while/for",                  
			"*24: continue must be in a while/for",              

			// parameters 
			"*25: too many actual parameters",                  
			"*26: too few actual parameters",                  
			"*27: wrong type for actual parameter",           

			// reserved for errors that I may have missed (J. Xue)
			"*28: misc 1",
			"*29: misc 2",

			// the following two checks are optional 
			"*30: statement(s) not reached",     
			"*31: missing return statement",    
	};

	private SymbolTable idTable;
	private static SourcePosition dummyPos = new SourcePosition();
	private ErrorReporter reporter;
	private final static Ident dummyI = new Ident("x", dummyPos);
	// Checks whether the source program, represented by its AST, 
	// satisfies the language's scope rules and type rules.
	// Also decorates the AST as follows:
	//  (1) Each applied occurrence of an identifier is linked to
	//      the corresponding declaration of that identifier.
	//  (2) Each expression and variable is decorated by its type.
	public Checker (ErrorReporter reporter) {
		this.reporter = reporter;
		this.idTable = new SymbolTable ();
		establishStdEnvironment();
	}

	public void check(AST ast) {
		ast.visit(this, null);
	}

	// auxiliary methods
	private void declareVariable(Ident ident, Decl decl) {
		IdEntry entry = idTable.retrieveOneLevel(ident.spelling);
		if (entry == null) {
			; // no problem
		} else {
			reporter.reportError(errMesg[2] + ": %", ident.spelling, ident.position);
		}
		idTable.insert(ident.spelling, decl);
	}

	// Programs
	public Object visitProgram(Program ast, Object o) {
		ast.FL.visit(this, null);
		return null;
	}

	// Declarations
	// check if it is a main function. If it is, return type must be integer
	public Object visitFuncDecl(FuncDecl funcDecl, Object o) {
		idTable.insert (funcDecl.I.spelling, funcDecl); 
		if(funcDecl.I.spelling.equals("main")) {
			if(!funcDecl.T.isIntType()) {
				// return type of main function is not integer
				reporter.reportError(errMesg[1], funcDecl.I.spelling, funcDecl.position);
			}
		}
		funcDecl.S.visit(this, funcDecl);
		return null;
	}

	public Object visitDeclList(DeclList ast, Object o) {
		ast.D.visit(this, null);
		ast.DL.visit(this, null);
		return null;
	}

	public Object visitGlobalVarDecl(GlobalVarDecl globalVarDecl, Object o) {
		declareVariable(globalVarDecl.I, globalVarDecl);
		return null;
		// fill the rest
	}

	/*
	 * not finish yet
	 * */
	public Object visitLocalVarDecl(LocalVarDecl localVarDecl, Object o) {
		declareVariable(localVarDecl.I, localVarDecl);
		if(localVarDecl.T.isVoidType()) {
			// declaration cannot be void type
			reporter.reportError(errMesg[3], localVarDecl.I.spelling, localVarDecl.position);
		}
		if(localVarDecl.T.isArrayType()) {
			if(((ArrayType)localVarDecl.T).T.isVoidType()) {
				// array cannot be void type
				reporter.reportError(errMesg[4], localVarDecl.I.spelling, localVarDecl.position);
			}
			if(((ArrayType)localVarDecl.T).E.isEmptyExpr() && !(localVarDecl.E instanceof InitExpr)) {
				// array length is not specified explicitly and without array initialization list  
				reporter.reportError(errMesg[18], localVarDecl.I.spelling, localVarDecl.position);
			}
		}
		
		Object exprTypeOrInitLength = localVarDecl.E.visit(this, localVarDecl.T);
		if(localVarDecl.T.isArrayType()) {
			if(localVarDecl.E instanceof InitExpr) {
				Integer initListLength = (Integer)exprTypeOrInitLength;
				if(((ArrayType)localVarDecl.T).E.isEmptyExpr()) {
					((ArrayType)localVarDecl.T).E = new IntExpr(new IntLiteral(initListLength.toString(), dummyPos), dummyPos);
				} else {
					Integer size = Integer.parseInt(((IntExpr)((ArrayType)localVarDecl.T).E).IL.spelling);
					if(size < initListLength) {
						// length of array init is larger than array size
						reporter.reportError(errMesg[16], "", localVarDecl.E.position);
					}
				}
			}
			if(!localVarDecl.E.isEmptyExpr()) {
				reporter.reportError(errMesg[15], "", localVarDecl.position);
			}
		}
				
		return null;
		// fill the rest
	}

	// Statements
	@Override
	public Object visitIfStmt(IfStmt ifStmt, Object o) {
		Type exprType = (Type)ifStmt.E.visit(this, null);
		if(!exprType.isBooleanType()) {
			// not a boolean expression 
			reporter.reportError(errMesg[20], exprType + " appears here.", ifStmt.position);
		}
		ifStmt.S1.visit(this, null);
		ifStmt.S2.visit(this, null);
		return null;
	}

	@Override
	public Object visitForStmt(ForStmt forStmt, Object o) {
		forStmt.E1.visit(this, null);
		Type exprType = (Type)forStmt.E2.visit(this,  null);
		if(!exprType.isBooleanType()) {
			// not a boolean expression
			reporter.reportError(errMesg[21], exprType + " appears here.", forStmt.position);
		}
		forStmt.E3.visit(this, null);
		forStmt.S.visit(this, null);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt whileStmt, Object o) {
		Type exprType = (Type)whileStmt.E.visit(this, null);
		if(!exprType.isBooleanType()) {
			// not a boolean expression 
			reporter.reportError(errMesg[22], exprType + " appears here.", whileStmt.position);
		}
		whileStmt.S.visit(this, null);
		return null;
	}

	private boolean isInWhileOrFor(AST currentParent) {
		boolean isFound = false;
		while(true) {
			if(currentParent instanceof WhileStmt || currentParent instanceof ForStmt) {
				isFound = true;
				break;
			} else {
				currentParent = currentParent.parent;
			}
		}
		return isFound;
	}

	// find whether break is in while or for statements along parent pointer
	@Override
	public Object visitBreakStmt(BreakStmt breakStmt, Object o) {
		boolean isFound = isInWhileOrFor(breakStmt.parent);
		if(!isFound) {
			reporter.reportError(errMesg[23], null, breakStmt.position);
		}
		return null;
	}

	@Override
	public Object visitContinueStmt(ContinueStmt continueStmt, Object o) {
		boolean isFound = isInWhileOrFor(continueStmt.parent);
		if(!isFound) {
			reporter.reportError(errMesg[24], "", continueStmt.position);
		}
		return null;
	}

	// check type
	@Override
	public Object visitReturnStmt(ReturnStmt ast, Object o) {
		return null;
	}

	@Override
	public Object visitCompoundStmt(CompoundStmt compoundStmt, Object o) {
		idTable.openScope();
		compoundStmt.DL.visit(this, null);
		compoundStmt.SL.visit(this, null);
		idTable.closeScope();
		return null;
	}

	@Override
	public Object visitStmtList(StmtList ast, Object o) {
		ast.S.visit(this, o);
		if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList) {
			// Unreachable statements after return statement
			reporter.reportError(errMesg[30], "", ast.SL.position);
		}
		ast.SL.visit(this, o);
		return null;
	}

	public Object visitExprStmt(ExprStmt ast, Object o) {
		ast.E.visit(this, o);
		return null;
	}

	@Override
	public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
		return null;
	}
	
	public Object visitEmptyStmt(EmptyStmt ast, Object o) {
		return null;
	}

	@Override
	public Object visitEmptyExprList(EmptyExprList ast, Object o) {
		return null;
	}
	
	public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
		return null;
	}

	public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
		return null;
	}

	public Object visitEmptyParaList(EmptyParaList ast, Object o) {
		return null;
	}

	public Object visitIntExpr(IntExpr ast, Object o) {
		ast.type = StdEnvironment.intType;
		return ast.type;
	}

	public Object visitFloatExpr(FloatExpr ast, Object o) {
		ast.type = StdEnvironment.floatType;
		return ast.type;
	}

	@Override
	public Object visitBooleanExpr(BooleanExpr ast, Object o) {
		ast.type = StdEnvironment.booleanType;
		return ast.type;
	}

	@Override
	public Object visitStringExpr(StringExpr ast, Object o) {
		ast.type = StdEnvironment.stringType;
		return ast.type;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr ast, Object o) {
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr ast, Object o) {
		return null;
	}

	/*
	 * Object o is the type of parameter
	 * if o is not array type, report error
	 * return value is the length of init list
	 * */
	@Override
	public Object visitInitExpr(InitExpr initExpr, Object o) {
		Type declType = (Type)o;
		if(!declType.isArrayType()) {
			// array initializer for scalar
			reporter.reportError(errMesg[14], "", initExpr.position);
			initExpr.type = StdEnvironment.errorType;
		}
		return initExpr.IL.visit(this, ((ArrayType)declType).T);
	}

	/*
	 * array initialization list. TODO: type check, type coercion, calculate size
	 * return the length of expression list 
	 * */
	@Override
	public Object visitExprList(ExprList exprList, Object o) {
		Type elementTpye = (Type)o;
		exprList.E.visit(this, null);
		if(!exprList.E.type.equals(elementTpye)) {
			if (exprList.E.type.assignable(elementTpye)){
				exprList.E = i2f(exprList.E);
			} else {
				reporter.reportError(errMesg[13], "", exprList.E.position);
			}
		}
		if((exprList.EL instanceof ExprList)) {
			return new Integer((Integer)exprList.EL.visit(this, o)) + 1;
		}
		return new Integer(1);
	}

	//
	@Override
	public Object visitArrayExpr(ArrayExpr arrayExpr, Object o) {
		return null;
	}

	@Override
	public Object visitVarExpr(VarExpr varExpr, Object o) {
		varExpr.type = (Type)varExpr.visit(this, null);
		return varExpr.type;
	}

	@Override
	public Object visitCallExpr(CallExpr call, Object o) {
		Decl funcDecl = idTable.retrieve(call.I.spelling);
		if(funcDecl == null) {
			// cannot find symbol
			reporter.reportError(errMesg[5], call.I.spelling, call.position);
			call.type = StdEnvironment.errorType;
		} else if (funcDecl.isFuncDecl()) {
			// fetch formal parameter list from function declaration and pass it to actual parameters
			call.AL.visit(this, ((FuncDecl)funcDecl).PL);
			call.type = funcDecl.T;
		} else {
			// use scalar or array as a function
			reporter.reportError(errMesg[19], call.I.spelling, call.position);
			call.type = StdEnvironment.errorType;
		}
		return call.type;
	}

	private boolean isValidLvalue(Expr lvalue) {
		return false;
	}

	// here is not finished yet
	@Override
	public Object visitAssignExpr(AssignExpr assign, Object o) {
		Expr lvalue = assign.E1;
		Expr rvalue = assign.E2;
		if(isValidLvalue(lvalue)) {
			Type ltype = (Type)lvalue.visit(this, null);
			Type rtype = (Type)rvalue.visit(this, null);
			// array cannot be lvalue and rvalue 
			if(ltype.isArrayType() || rtype.isArrayType()) {
				// use array as a scalar
				reporter.reportError(errMesg[11], "", assign.position);
				assign.type = StdEnvironment.errorType;
			} 
		} else {
			reporter.reportError(errMesg[7], "", assign.position);
		}
		return null;
	} 

	@Override
	public Object visitEmptyExpr(EmptyExpr ast, Object o) {
		ast.type = StdEnvironment.errorType;
		return ast.type;
	}


	// Literals, Identifiers and Operators
	@Override
	public Object visitIntLiteral(IntLiteral IL, Object o) {
		return StdEnvironment.intType;
	}

	@Override
	public Object visitFloatLiteral(FloatLiteral IL, Object o) {
		return StdEnvironment.floatType;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
		return StdEnvironment.booleanType;
	}

	@Override
	public Object visitStringLiteral(StringLiteral IL, Object o) {
		return StdEnvironment.stringType;
	}

	@Override
	public Object visitIdent(Ident I, Object o) {
		Decl binding = idTable.retrieve(I.spelling);
		if (binding != null) {
			I.decl = binding;
		}
		return binding;
	}

	@Override
	public Object visitOperator(Operator O, Object o) {
		return null;
	}

	//Parameters
	@Override
	public Object visitParaList(ParaList ast, Object o) {
		ast.P.visit(this, null);
		ast.PL.visit(this, null);
		return null;
	}

	// check formal parameters
	@Override
	public Object visitParaDecl(ParaDecl ast, Object o) {
		declareVariable(ast.I, ast);
		if (ast.T.isVoidType()) {
			// formal parameter cannot be void type
			reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
		} else if (ast.T.isArrayType()) {
			if (((ArrayType) ast.T).T.isVoidType()) {
				// formal parameter can be array type, but array cannot be void type
				reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
			}
		}
		return null;
	}

	// Arguments
	/*
	 * check type of actual parameters and formal parameters are compatible or not
	 * object o is the formal parameter list
	 * */
	@Override
	public Object visitArgList(ArgList argList, Object o) {
		List formalParaList = (ParaList)o;
		/*
		 * If this function is invoked, that means this function invocation has at least one actual argument.
		 * If formal parameter list is an empty list, report too many actual argument here.
		 * */
		if(formalParaList.isEmptyParaList()) {
			reporter.reportError(errMesg[25], "", argList.position);
		}
		argList.A.visit(this, ((ParaList)formalParaList).P);
		argList.AL.visit(this, ((ParaList)formalParaList).PL);
		return null;
	}

	/*
	 * check the compatibility between the type of formal parameter and actual argument.
	 * Object o is a formal parameter.
	 * If formal parameter and actual argument are array type, the type of array should be assignable.
	 * */
	@Override
	public Object visitArg(Arg arg, Object o) {
		Decl formalParam = (Decl)o;
		Type formalType = formalParam.T;
		Type actualType = (Type)arg.E.visit(this, null);
		boolean isMatch = false;
		if(formalType.isArrayType()) {
			if(actualType.isArrayType()) {
				Type formalArrayType = ((ArrayType)formalType).T;
				Type actualArrayType = ((ArrayType)actualType).T;
				if(actualArrayType.assignable(formalArrayType)) {
					isMatch = true;
				}
			}
		} else {
			if(actualType.assignable(formalType)) {
				isMatch = true;
			}
		}
		if(!isMatch) {
			reporter.reportError(errMesg[27], "", arg.E.position);
		}
		if(actualType.equals(StdEnvironment.intType) && formalType.equals(StdEnvironment.floatType)) {
			// type coercion here
			arg.E = i2f(arg.E);
		}
		return null;
	}
	
	/*
	 * Check the too few actual argument
	 * object o is formal parameter list. If it is not empty, report error.
	 * */
	@Override
	public Object visitEmptyArgList(EmptyArgList emptyArgList, Object o) {
		List formalParaList = (List)o;
		if(formalParaList instanceof EmptyParaList) {
			reporter.reportError(errMesg[26], "", emptyArgList.position);
		}
		return null;
	}

	@Override
	public Object visitVoidType(VoidType ast, Object o) {
		return StdEnvironment.voidType;
	}

	@Override
	public Object visitBooleanType(BooleanType ast, Object o) {
		return StdEnvironment.booleanType;
	}

	@Override
	public Object visitIntType(IntType ast, Object o) {
		return StdEnvironment.intType;
	}

	@Override
	public Object visitFloatType(FloatType ast, Object o) {
		return StdEnvironment.floatType;
	}

	@Override
	public Object visitStringType(StringType ast, Object o) {
		return StdEnvironment.stringType;
	}

	@Override
	public Object visitArrayType(ArrayType array, Object o) {
		return array;
	}

	@Override
	public Object visitErrorType(ErrorType ast, Object o) {
		return StdEnvironment.errorType;
	}

	/*
	 * variable should be declared
	 * variable cannot be a function name
	 * array name cannot be used alone except that it is a function actual argument
	 * */
	@Override
	public Object visitSimpleVar(SimpleVar simpleVar, Object o) {
		Decl decl = idTable.retrieve(simpleVar.I.spelling);
		simpleVar.type = StdEnvironment.errorType;
		if(decl == null) {
			// undeclared identifier
			reporter.reportError(errMesg[5], simpleVar.I.spelling, simpleVar.position);
		} else if (decl instanceof FuncDecl) {
			// identifier collides with function name
			reporter.reportError(errMesg[11], simpleVar.I.spelling, simpleVar.position);
		} else {
			simpleVar.type = decl.T;
		}
		// if array name are not used as a actual argument
		if(decl.T.isArrayType() && simpleVar.parent.parent instanceof Arg) {
			reporter.reportError(errMesg[11], decl.I.spelling, simpleVar.position);
		}
		return simpleVar.type;
	}

	// Creates a small AST to represent the "declaration" of each built-in
	// function, and enters it in the symbol table.
	private FuncDecl declareStdFunc (Type resultType, String id, List pl) {
		FuncDecl binding;
		binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl, 
				new EmptyStmt(dummyPos), dummyPos);
		idTable.insert (id, binding);
		return binding;
	}
	
	private Expr i2f(Expr currentExpr) {
		Expr newExpr = new UnaryExpr(new Operator("i2f", currentExpr.position), currentExpr, currentExpr.position);
		newExpr.type = StdEnvironment.floatType;
		newExpr.parent = currentExpr.parent;
		currentExpr.parent = newExpr;
		return newExpr;
	}
	
	// Creates small ASTs to represent "declarations" of all 
	// build-in functions.
	// Inserts these "declarations" into the symbol table.
	private void establishStdEnvironment () {
		// Define four primitive types
		// errorType is assigned to ill-typed expressions
		StdEnvironment.booleanType = new BooleanType(dummyPos);
		StdEnvironment.intType = new IntType(dummyPos);
		StdEnvironment.floatType = new FloatType(dummyPos);
		StdEnvironment.stringType = new StringType(dummyPos);
		StdEnvironment.voidType = new VoidType(dummyPos);
		StdEnvironment.errorType = new ErrorType(dummyPos);
		// enter into the declarations for built-in functions into the table
		StdEnvironment.getIntDecl = declareStdFunc( StdEnvironment.intType,
				"getInt", new EmptyParaList(dummyPos)); 
		StdEnvironment.putIntDecl = declareStdFunc( StdEnvironment.voidType,
				"putInt", new ParaList(
						new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos)); 
		StdEnvironment.putIntLnDecl = declareStdFunc( StdEnvironment.voidType,
				"putIntLn", new ParaList(
						new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos)); 
		StdEnvironment.getFloatDecl = declareStdFunc( StdEnvironment.floatType,
				"getFloat", new EmptyParaList(dummyPos));
		StdEnvironment.putFloatDecl = declareStdFunc( StdEnvironment.voidType,
				"putFloat", new ParaList(
						new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putFloatLnDecl = declareStdFunc( StdEnvironment.voidType,
				"putFloatLn", new ParaList(
						new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putBoolDecl = declareStdFunc( StdEnvironment.voidType,
				"putBool", new ParaList(
						new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putBoolLnDecl = declareStdFunc( StdEnvironment.voidType,
				"putBoolLn", new ParaList(
						new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putStringLnDecl = declareStdFunc( StdEnvironment.voidType,
				"putStringLn", new ParaList(
						new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putStringDecl = declareStdFunc( StdEnvironment.voidType,
				"putString", new ParaList(
						new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putLnDecl = declareStdFunc( StdEnvironment.voidType,
				"putLn", new EmptyParaList(dummyPos));
	}
}
