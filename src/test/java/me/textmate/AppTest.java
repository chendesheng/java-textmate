package me.textmate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import me.textmate.grammar.StackElement;
import me.textmate.main.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    public List<IToken[]> tokenizeLines(IGrammar grammar, List<String> lines) {
        List<IToken[]> result = new ArrayList<IToken[]>();
        StackElement continuation = null;
        int i = 0;
        for (String line : lines) {
            ITokenizeLineResult lineTokens = grammar.tokenizeLine(line, continuation);
            IToken[] tokens = lineTokens.tokens;
            continuation = lineTokens.ruleStack;
            result.add(i, tokens);
            i++;
        }

        return result;
    }

    public void printTokenizeLines2(IGrammar grammar, List<String> lines) {
        for (IToken[] tokens : tokenizeLines(grammar, lines)) {
            for (IToken token : tokens) {
                System.out.printf("%d %d %s\n", token.startIndex, token.endIndex, String.join(" ", token.scopes));
            }
            System.out.println();
            System.out.println();
        }
    }

    public void printTokenizeLines(IGrammar grammar, List<String> lines) {
        StackElement continuation = null;
        for (String line : lines) {
            ITokenizeLineResult lineTokens = grammar.tokenizeLine(line, continuation);
            IToken[] tokens = lineTokens.tokens;
            continuation = lineTokens.ruleStack;
            System.out.println(line);

            for (int i = 0; i < tokens.length; i++) {
                IToken token = tokens[i];
                for (int j = 0; j < token.startIndex; j++) {
                    System.out.print(" ");
                }
                for (int j = token.startIndex; j < token.endIndex; j++) {
                    System.out.print("^");
                }

                System.out.println(" [" + String.join(", ", token.scopes) + "]");
            }
        }

        System.out.flush();
    }

    public void testClojure() throws Exception
    {
        Registry registry = new Registry();
        IGrammar grammar = registry.loadGrammarFromPathSync("test-cases/clojure.tmLanguage.json");
        // List<String> lines = Files.readAllLines(Paths.get("test-cases/client.cljs"));
        // printTokenizeLines(grammar, lines);
        List<String> lines = Arrays.asList(new String[]{"(ns \"a\")"});
        printTokenizeLines(grammar, lines);
    }

    /**
     * Rigourous Test :-)
     */
    // public void testJavascript() throws Exception
    // {
    //     Registry registry = new Registry();
    //     IGrammar grammar = registry.loadGrammarFromPathSync("test-cases/javascript.tmLanguage.json");
    //     List<String> lines = Arrays.asList(new String[] {
    //         "import testing;",
    //         "function yes([a, b]) {",
    //         "   return a+b+1e100;",
    //         "}",
    //         "const y = <div>",
    //         "   <span />",
    //         "   <img onclick={x=>x+1} src={`${x}`} />",
    //         "</div>",
    //         "const reg = /test/ig;",
    //         "reg.exec(\"nihao\")",
    //         "class testclass { constructor() {} }",
    //     });
    //     printTokenizeLines(grammar, lines);
    //     assertTrue( true );
    // }
}
