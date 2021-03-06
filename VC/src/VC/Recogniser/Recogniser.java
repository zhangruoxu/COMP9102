/***
 * *
 * * Recogniser.java            
 * *
 ***/
package VC.Recogniser;
import java.util.Arrays;
import java.util.HashSet;
import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;

public class Recogniser {
	static {
		exprFirstSet = new HashSet<Integer>(Arrays.asList(Token.LPAREN, Token.PLUS, Token.MINUS, Token.NOT, Token.ID, 
				Token.INTLITERAL, Token.FLOATLITERAL, Token.BOOLEANLITERAL, Token.STRINGLITERAL));
		typeFirstSet = new HashSet<Integer>(Arrays.asList(Token.VOID, Token.BOOLEAN, Token.INT, Token.FLOAT));
	}

	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token currentToken;
	private static HashSet<Integer> exprFirstSet;
	private static HashSet<Integer> typeFirstSet;

	public Recogniser (Scanner lexer, ErrorReporter reporter) {
		scanner = lexer;
		errorReporter = reporter;
		currentToken = scanner.getToken();
	}

	// match checks to see f the current token matches tokenExpected.
	// If so, fetches the next token.
	// If not, reports a syntactic error.
	void match(int tokenExpected) throws SyntaxError {
		if (currentToken.kind == tokenExpected) {
			currentToken = scanner.getToken();
		} else {
			syntacticError("\"%\" expected here", Token.spell(tokenExpected));
		}
	}

	// accepts the current token and fetches the next
	void accept() {
		currentToken = scanner.getToken();
	}

	void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
		SourcePosition pos = currentToken.position;
		errorReporter.reportError(messageTemplate, tokenQuoted, pos);
		throw(new SyntaxError());
	}

	// ========================== PROGRAMS ========================
	public void parseProgram() {
		try {
			while(currentToken.kind != Token.EOF) {
				parseCommonPrefix();
				if(currentToken.kind == Token.LPAREN) {
					parsePartFuncDecl();
				} else {
					parsePartVarDecl();
				}
			} 
			if (currentToken.kind != Token.EOF) {
				syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
			}
		} catch (SyntaxError s) {}
	}

	private void parseCommonPrefix() throws SyntaxError {
		parseType();
		parseIdent();
	}

	private void parsePartFuncDecl() throws SyntaxError {
		parseParaList();
		parseCompoundStmt();
	}

	private void parsePartVarDecl() throws SyntaxError {
		if(currentToken.kind == Token.LBRACKET) {
			accept();
			if(currentToken.kind == Token.INTLITERAL) {
				parseIntLiteral();
			}
			match(Token.RBRACKET);
		}
		if(currentToken.kind == Token.EQ) {
			accept();
			parseInitialiser();
		}
		while(currentToken.kind == Token.COMMA) {
			accept();
			parseInitDeclarator();
		}
		match(Token.SEMICOLON);
	}

	private void parseVarDecl() throws SyntaxError {
		parseType();
		parseInitDeclaratorList();
		match(Token.SEMICOLON);
	}

	private void parseInitDeclaratorList() throws SyntaxError {
		parseInitDeclarator();
		while(currentToken.kind == Token.COMMA) {
			accept();
			parseInitDeclarator();
		}
	}

	private void parseInitDeclarator() throws SyntaxError {
		parseDeclarator();
		if(currentToken.kind == Token.EQ) {
			accept();
			parseInitialiser();
		}
	}

	private void parseDeclarator() throws SyntaxError {
		parseIdent();
		if(currentToken.kind == Token.LBRACKET) {
			accept();
			if(currentToken.kind == Token.INTLITERAL) {
				parseIntLiteral();
			}
			match(Token.RBRACKET);
		}
	}

	private void parseInitialiser() throws SyntaxError {
		if(currentToken.kind == Token.LCURLY) {
			accept();
			parseExpr();
			while(currentToken.kind == Token.COMMA) {
				accept();
				parseExpr();
			}
			match(Token.RCURLY);
		} else {
			parseExpr();
		}
	}

	private void parseType() throws SyntaxError {
		if(typeFirstSet.contains(currentToken.kind)) {
			accept();
		} else {
			syntacticError("type expected here", "");
		}
	}

	// ======================= STATEMENTS ==============================
	private void parseCompoundStmt() throws SyntaxError {
		match(Token.LCURLY);
		parseStmtList();
		match(Token.RCURLY);
	}

	// Here, a new nontermial has been introduced to define { stmt } *
	private void parseStmtList() throws SyntaxError {
		while (currentToken.kind != Token.RCURLY) {
			if(typeFirstSet.contains(currentToken.kind)) {
				parseVarDecl();
			} else {
				break;
			}
		}
		while(currentToken.kind != Token.RCURLY) {
			parseStmt();
		}
	}

	private void parseStmt() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.LCURLY:
			parseCompoundStmt();
			break;
		case  Token.IF:
			parseIfStmt();
			break;
		case Token.FOR:
			parseForStmt();
			break;
		case Token.WHILE:
			parseWhileStmt();
			break;
		case Token.BREAK:
			parseBreakStmt();
			break;
		case Token.CONTINUE:
			parseContinueStmt();
			break;
		case Token.RETURN:
			parseReturnStmt();
			break;
		default:
			parseExprStmt();
			break;
		}
	}

	private void parseIfStmt() throws SyntaxError {
		accept();
		match(Token.LPAREN);
		parseExpr();
		match(Token.RPAREN);
		parseStmt();
		if(currentToken.kind == Token.ELSE) {
			match(Token.ELSE);
			parseStmt();
		}
	}

	private void parseForStmt() throws SyntaxError {
		accept();
		match(Token.LPAREN);
		if(exprFirstSet.contains(currentToken.kind)) {
			parseExpr();
		}
		match(Token.SEMICOLON);
		if(exprFirstSet.contains(currentToken.kind)) {
			parseExpr();
		}
		match(Token.SEMICOLON);
		if(exprFirstSet.contains(currentToken.kind)) {
			parseExpr();
		}
		match(Token.RPAREN);
		parseStmt();
	}

	private void parseWhileStmt() throws SyntaxError {
		accept();
		match(Token.LPAREN);
		parseExpr();
		match(Token.RPAREN);
		parseStmt();
	}

	private void parseBreakStmt() throws SyntaxError {
		accept();
		match(Token.SEMICOLON);
	}

	private void parseContinueStmt() throws SyntaxError {
		accept();
		match(Token.SEMICOLON);
	}

	private void parseReturnStmt() throws SyntaxError {
		accept();
		if(exprFirstSet.contains(currentToken.kind)) {
			parseExpr();
		}
		match(Token.SEMICOLON);
	}

	private void parseExprStmt() throws SyntaxError {
		if(exprFirstSet.contains(currentToken.kind)) {
			parseExpr();
		}
		match(Token.SEMICOLON);
	}

	// ======================= IDENTIFIERS ======================
	// Call parseIdent rather than match(Token.ID). 
	// In Assignment 3, an Identifier node will be constructed in here.
	private void parseIdent() throws SyntaxError {
		if (currentToken.kind == Token.ID) {
			currentToken = scanner.getToken();
		} else 
			syntacticError("identifier expected here", "");
	}

	// ======================= OPERATORS ======================

	// Call acceptOperator rather than accept(). 
	// In Assignment 3, an Operator Node will be constructed in here.
	private void acceptOperator() throws SyntaxError {
		currentToken = scanner.getToken();
	}

	// ======================= EXPRESSIONS ======================
	private void parseExpr() throws SyntaxError {
		parseAssignExpr();
	}

	/*
	 * left recursion
	 * A -> B | A op B
	 * 
	 * A->BA'
	 * A'-> op B | epsilon
	 * 
	 * A->B(op B)*
	 * */
	private void parseAssignExpr() throws SyntaxError {
		parseCondOrExpr();
		while(currentToken.kind == Token.EQ) {
			acceptOperator();
			parseCondOrExpr();
		}
	}
		
	private void parseCondOrExpr() throws SyntaxError {
		parseCondAndExpr();
		while(currentToken.kind == Token.OROR) {
			acceptOperator();
			parseCondAndExpr();
		}
	}

	private void parseCondAndExpr() throws SyntaxError {
		parseEqualityExpr();
		while(currentToken.kind == Token.ANDAND) {
			acceptOperator();
			parseEqualityExpr();
		}
	}

	private void parseEqualityExpr() throws SyntaxError {
		parseRelExpr();
		while(currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
			acceptOperator();
			parseRelExpr();
		}
	}

	private void parseRelExpr() throws SyntaxError {
		parseAdditiveExpr();
		while(currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ ||
				currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
			acceptOperator();
			parseAdditiveExpr();
		}
	}

	private void parseAdditiveExpr() throws SyntaxError {
		parseMultiplicativeExpr();
		while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
			acceptOperator();
			parseMultiplicativeExpr();
		}
	}

	private void parseMultiplicativeExpr() throws SyntaxError {
		parseUnaryExpr();
		while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
			acceptOperator();
			parseUnaryExpr();
		}
	}

	private void parseUnaryExpr() throws SyntaxError {
		if(currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS || 
				currentToken.kind == Token.NOT) {
			acceptOperator();
			parseUnaryExpr();
		} else {
			parsePrimaryExpr();
		}
	}

	private void parsePrimaryExpr() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.ID:
			parseIdent();
			if(currentToken.kind == Token.LPAREN) {
				parseArgList();
			} else if(currentToken.kind == Token.LBRACKET) {
				accept();
				parseExpr();
				match(Token.RBRACKET);
			} else {
				break;
			}
			break;
		case Token.LPAREN:
			accept();
			parseExpr();
			match(Token.RPAREN);
			break;
		case Token.INTLITERAL:
			parseIntLiteral();
			break;
		case Token.FLOATLITERAL:
			parseFloatLiteral();
			break;
		case Token.BOOLEANLITERAL:
			parseBooleanLiteral();
			break;
		case Token.STRINGLITERAL:
			accept();
			break;
		default:
			syntacticError("\"%\" illegal parimary expression", currentToken.spelling);
		}
	}

	// ========================== LITERALS ========================
	// Call these methods rather than accept().  In Assignment 3, 
	// literal AST nodes will be constructed inside these methods. 
	private void parseIntLiteral() throws SyntaxError {
		if (currentToken.kind == Token.INTLITERAL) {
			currentToken = scanner.getToken();
		} else 
			syntacticError("integer literal expected here", "");
	}

	private void parseFloatLiteral() throws SyntaxError {
		if (currentToken.kind == Token.FLOATLITERAL) {
			currentToken = scanner.getToken();
		} else 
			syntacticError("float literal expected here", "");
	}

	private void parseBooleanLiteral() throws SyntaxError {
		if (currentToken.kind == Token.BOOLEANLITERAL) {
			currentToken = scanner.getToken();
		} else 
			syntacticError("boolean literal expected here", "");
	}

	private void parseParaList() throws SyntaxError {
		match(Token.LPAREN);
		if(typeFirstSet.contains(currentToken.kind)) {
			parseProperParaList();
		}
		match(Token.RPAREN);
	}

	private void parseProperParaList() throws SyntaxError {
		parseParaDecl();
		while(currentToken.kind == Token.COMMA) {
			accept();
			parseParaDecl();
		}
	}

	private void parseParaDecl() throws SyntaxError {
		parseType();
		parseDeclarator();
	}

	private void parseArgList() throws SyntaxError {
		match(Token.LPAREN);
		if(exprFirstSet.contains(currentToken.kind)) {
			parseProperArgList();
		}
		match(Token.RPAREN);
	}

	private void parseProperArgList() throws SyntaxError {
		parseExpr();
		while(currentToken.kind == Token.COMMA) {
			accept();
			parseExpr();
		}
	}
}
