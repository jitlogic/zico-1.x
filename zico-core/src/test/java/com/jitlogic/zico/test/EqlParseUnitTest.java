/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.eql.EqlParseException;
import com.jitlogic.zico.core.eql.EqlUtils;
import com.jitlogic.zico.core.eql.Parser;
import com.jitlogic.zico.core.eql.ast.EqlBinaryExpr;
import com.jitlogic.zico.core.eql.ast.EqlExpr;
import com.jitlogic.zico.core.eql.ast.EqlFunCall;
import com.jitlogic.zico.core.eql.ast.EqlLiteral;
import com.jitlogic.zico.core.eql.ast.EqlOp;
import com.jitlogic.zico.core.eql.ast.EqlSymbol;
import com.jitlogic.zico.core.eql.ast.EqlUnaryExpr;
import org.junit.Test;

import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EqlParseUnitTest {


    private EqlExpr expr(String expr) throws EqlParseException {
        EqlExpr rslt = new Parser().expr(expr);
        assertNotNull("Result should not be null.", rslt);
        return rslt;
    }


    private EqlLiteral lit(Object val) {
        return new EqlLiteral(val);
    }


    private EqlSymbol sym(String name) {
        return new EqlSymbol(name);
    }


    private EqlBinaryExpr bin(EqlExpr arg1, String opName, EqlExpr arg2) {
        return new EqlBinaryExpr(arg1, EqlOp.fromName(opName), arg2);
    }

    private EqlUnaryExpr una(String opName, EqlExpr arg) {
        return new EqlUnaryExpr(EqlOp.fromName(opName), arg);
    }

    private EqlFunCall fun(EqlExpr function, EqlExpr... args) {
        return new EqlFunCall(function, args);
    }


    @Test
    public void testParseSimpleLiteralExprs() throws EqlParseException {
        assertEquals(lit(123), expr("123"));
        assertEquals(lit(15), expr("0x0f"));
        assertEquals(lit(-4), expr("-4"));
        assertEquals(lit(-4), expr("-0x04"));
        assertEquals(lit(1L), expr("1L"));
        assertEquals(lit(-15L), expr("-0x0fL"));
        assertEquals(lit("abc"), expr("'abc'"));
        assertEquals(lit("ab\ncd"), expr("'ab\\ncd'"));
        assertEquals(lit(null), expr("null"));
        assertEquals(lit(true), expr("true"));
        assertEquals(lit(false), expr("false"));
    }


    @Test
    public void testParseSymbols() throws EqlParseException {
        assertEquals(sym("a"), expr("a"));
    }


    @Test
    public void testParseBinaryExpressionsBasics() throws EqlParseException {
        assertEquals(bin(lit(2), "+", lit(2)), expr("2 + 2"));
        assertEquals(bin(sym("x"), "=", lit(2)), expr("x = 2"));
        assertEquals(bin(sym("a"), "and", sym("b")), expr("a and b"));
        assertEquals(bin(bin(sym("a"), "+", sym("b")), "+", sym("c")), expr("a + b + c"));
        assertEquals(bin(sym("x"), ".", sym("y")), expr("x.y"));
        assertEquals(bin(lit(5), "%", lit(3)), expr("5 % 3"));
        assertEquals(bin(lit(1), "|", lit(2)), expr("1|2"));
        assertEquals(bin(lit(1), "&", lit(2)), expr("1&2"));
        assertEquals(bin(lit(1), "^", lit(2)), expr("1^2"));
    }


    @Test
    public void testParseBinaryExpressionsPrecedence() throws EqlParseException {
        assertEquals(bin(bin(lit(2), "*", lit(2)), "+", bin(lit(3), "*", lit(3))), expr("2 * 2 + 3 * 3"));
        assertEquals(bin(bin(lit(2), "*", lit(2)), "+", bin(lit(3), "*", lit(3))), expr("2*2+3*3"));
        assertEquals(bin(lit(2), "*", bin(lit(3), "+", lit(3))), expr("2 * (3 + 3)"));
        assertEquals(bin(lit(2), "*", bin(lit(3), "+", lit(3))), expr("2*(3+3)"));
    }


    @Test
    public void testParseFunCalls() throws Exception {
        assertEquals(fun(sym("f")), expr("f()"));
        assertEquals(fun(sym("f"), lit(1)), expr("f(1)"));
        assertEquals(fun(sym("f"), lit(1), lit(2)), expr("f(1,2)"));
        assertEquals(fun(sym("f"), lit(1), lit(2)), expr("f(1 ,2)"));
        assertEquals(fun(sym("f"), lit(1), lit(2)), expr("f(1, 2)"));
        assertEquals(fun(sym("f"), lit(1), lit(2)), expr("f(1 , 2)"));
        assertEquals(fun(sym("f"), lit(1), lit(2)), expr("f( 1 , 2)"));
        assertEquals(fun(sym("f"), lit(1), lit(2)), expr("f ( 1 , 2 ) "));
        assertEquals(fun(sym("f"), fun(sym("g"), sym("x")), sym("y")), expr("f(g(x),y)"));
    }


    @Test
    public void testParseLogicalExpr() throws Exception {
        assertEquals(bin(bin(sym("t"), "<", lit(20)), "and", bin(sym("method"), "<>", lit("create"))),
                expr("t < 20 and method <> 'create'"));
    }


    @Test
    public void testParseTimestampLiterals() throws Exception {
        assertEquals(lit(10L), expr("10ns"));
        assertEquals(lit(20000L), expr("20us"));
        assertEquals(lit(1250L), expr("1.25us"));
        assertEquals(lit(1500L), expr("1.5us"));
        assertEquals(lit(123456789L), expr("123.456789ms"));
        assertEquals(lit(1000000000L), expr("1s"));
        assertEquals(lit(120000000000L), expr("2m"));
        assertEquals(lit(86400000000000L), expr("24h"));
    }


    @Test
    public void testParseUnaryExpr() {
        assertEquals(una("not", lit(1)), expr("not 1"));
        assertEquals(bin(una("not", bin(sym("a"), "=", lit(2))), "and", una("not", bin(sym("b"), "=", sym("c")))),
                expr("not a = 2 and ! b = c"));
        assertEquals(una("not", lit(1)), expr("(not 1)"));
        assertEquals(lit(2), expr("(2)"));
    }


    @Test
    public void testParseDateTime() throws Exception {
        assertEquals("Tue Jan 01 00:00:00 CET 2013", EqlUtils.parseDate("2013-01-01").toString());
        assertEquals("Tue Jan 01 12:23:34 CET 2013", EqlUtils.parseDate("2013-01-01 12:23:34").toString());
    }
}
