package me.textmate.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import me.textmate.types.IRawGrammar;

import me.textmate.types.IRawRepository;
import me.textmate.types.IRawRule;
import me.textmate.utils.Utils;
import me.textmate.main.IEmbeddedLanguagesMap;
import me.textmate.main.IGrammar;
import me.textmate.main.IToken;
import me.textmate.main.ITokenizeLineResult;
import me.textmate.main.ITokenizeLineResult2;
import me.textmate.matcher.MatcherCreator;
import me.oniguruma.IOnigCaptureIndex;
import me.oniguruma.IOnigNextMatchResult;
import me.textmate.rule.BeginEndRule;
import me.textmate.rule.BeginWhileRule;
import me.textmate.rule.CaptureRule;
import me.textmate.rule.ICompiledRule;
import me.textmate.rule.IRuleFactoryHelper;
import me.textmate.rule.MatchRule;
import me.textmate.rule.Rule;
import me.textmate.rule.RuleFactory;
import me.textmate.theme.ThemeTrieElementRule;
import me.textmate.Predicate;

public class Grammar implements IGrammar, IRuleFactoryHelper {
  /**
   * Fill in `result` all external included scopes in `patterns`
   */
  public static void _extractIncludedScopesInPatterns(IScopeNameSet result, IRawRule[] patterns) {
    for (int i = 0, len = patterns.length; i < len; i++) {

      if (patterns[i].patterns != null) {
        _extractIncludedScopesInPatterns(result, patterns[i].patterns);
      }

      String include = patterns[i].include;

      if (include == null) {
        continue;
      }

      if (include.equals("$base") || include.equals("$self")) {
        // Special includes that can be resolved locally in this grammar
        continue;
      }

      if (include.charAt(0) == '#') {
        // Local include from this grammar
        continue;
      }

      int sharpIndex = include.indexOf('#');
      if (sharpIndex >= 0) {
        result.put(include.substring(0, sharpIndex), true);
      } else {
        result.put(include, true);
      }
    }
  }

  /**
   * Fill in `result` all external included scopes in `repository`
   */
  public static void _extractIncludedScopesInRepository(IScopeNameSet result, IRawRepository repository) {
    for (IRawRule rule : repository.values()) {
      if (rule.patterns != null) {
        _extractIncludedScopesInPatterns(result, rule.patterns);
      }

      if (rule.repository != null) {
        _extractIncludedScopesInRepository(result, rule.repository);
      }
    }
  }

  /**
  * Collects the list of all external included scopes in `grammar`.
  */
  public static void collectIncludedScopes(IScopeNameSet result, IRawGrammar grammar) {
    if (grammar.patterns != null) {
      _extractIncludedScopesInPatterns(result, grammar.patterns);
    }

    if (grammar.repository != null) {
      _extractIncludedScopesInRepository(result, grammar.repository);
    }

    // remove references to own scope (avoid recursion)
    result.remove(grammar.scopeName);
  }

  public static void collectInjections(ArrayList<Injection> result, String selector, IRawRule rule,
      IRuleFactoryHelper ruleFactoryHelper, IRawGrammar grammar) {

    final Predicate<String, String> scopesAreMatching = new Predicate<String, String>() {
      public boolean call(String thisScopeName, String scopeName) {
        if (thisScopeName == null) {
          return false;
        }
        if (thisScopeName == scopeName) {
          return true;
        }
        int len = scopeName.length();
        return thisScopeName.length() > len && thisScopeName.substring(0, len) == scopeName
            && thisScopeName.charAt(len) == '.';
      }
    };

    String[] subExpressions = selector.split(",");
    for (String subExpression : subExpressions) {
      String expressionString = subExpression.replaceAll("L:", "");
      Injection injection = new Injection();
      injection.matcher = (new MatcherCreator<StackElement>()).createMatcher(expressionString,
          new Predicate<String[], StackElement>() {
            public boolean call(String[] identifiers, StackElement stackElements) {
              String[] scopes = stackElements.contentNameScopesList.generateScopes();
              int lastIndex = 0;
              // is every identifier res == true
              for (String identifier : identifiers) {
                boolean res = false;
                for (int i = lastIndex; i < scopes.length; i++) {
                  if (scopesAreMatching.call(scopes[i], identifier)) {
                    lastIndex = i;
                    res = true;
                    break;
                  }
                }
                if (!res) {
                  return false;
                }
              }
              return true;
            }
          });
      injection.ruleId = RuleFactory.getCompiledRuleId(rule, ruleFactoryHelper, grammar.repository);
      injection.grammar = grammar;
      injection.priorityMatch = expressionString.length() < subExpression.length();
      result.add(injection);
    }
  }

  public static boolean IN_DEBUG_MODE = false;

  /**
   * Walk the stack from bottom to top, and check each while condition in this order.
   * If any fails, cut off the entire stack above the failed while condition. While conditions
   * may also advance the linePosition.
   */
  private static IWhileCheckResult _checkWhileConditions(Grammar grammar, String lineText, boolean isFirstLine,
      int linePos, StackElement stack, LineTokens lineTokens) {
    int anchorPosition = -1;
    List<IWhileStack> whileRules = new ArrayList<IWhileStack>();
    for (StackElement node = stack.pop(); node != null; node = node.pop()) {
      Rule nodeRule = node.getRule(grammar);
      if (nodeRule instanceof BeginWhileRule) {
        whileRules.add(new IWhileStack(node, (BeginWhileRule) nodeRule));
      }
    }

		for (int i = whileRules.size() - 1; i >= 0; i--) {
			IWhileStack whileRule = whileRules.get(i);

      ICompiledRule ruleScanner = whileRule.rule.compileWhile(grammar, whileRule.stack.endRule, isFirstLine,
          anchorPosition == linePos);
      IOnigNextMatchResult r = ruleScanner.scanner._findNextMatchSync(lineText, linePos);
      if (IN_DEBUG_MODE) {
        System.out.println("  scanning for while rule");
        System.out.println(debugCompiledRuleToString(ruleScanner));
      }

      if (r != null) {
        int matchedRuleId = ruleScanner.rules[r.getIndex()];
        if (matchedRuleId != -2) {
          // we shouldn't end up here
          stack = whileRule.stack.pop();
          break;
        }
        IOnigCaptureIndex[] captureIndices = r.getCaptureIndices();
        if (captureIndices != null && captureIndices.length > 0) {
          lineTokens.produce(whileRule.stack, captureIndices[0].getStart());
          handleCaptures(grammar, lineText, isFirstLine, whileRule.stack, lineTokens, whileRule.rule.whileCaptures,
              captureIndices);
          lineTokens.produce(whileRule.stack, captureIndices[0].getEnd());
          anchorPosition = captureIndices[0].getEnd();
          if (captureIndices[0].getEnd() > linePos) {
            linePos = captureIndices[0].getEnd();
            isFirstLine = false;
          }
        }
      } else {
        stack = whileRule.stack.pop();
        break;
      }
    }

    return new IWhileCheckResult(stack, linePos, anchorPosition, isFirstLine);
  }

  private static IRawGrammar initGrammar(IRawGrammar grammar, IRawRule base) {
    grammar = (IRawGrammar) Utils.clone(grammar);
    if (grammar.repository == null)
      grammar.repository = new IRawRepository();
    grammar.repository.self(new IRawRule());
    grammar.repository.self().patterns = grammar.patterns;
    grammar.repository.self().name = grammar.scopeName;

    grammar.repository.base(base);
    if (base == null)
      grammar.repository.base(grammar.repository.self());

    return grammar;
  }

  private static void handleCaptures(Grammar grammar, String lineText, boolean isFirstLine, StackElement stack,
      LineTokens lineTokens, CaptureRule[] captures, IOnigCaptureIndex[] captureIndices) {
    if (captures.length == 0) {
      return;
    }

    int len = Math.min(captures.length, captureIndices.length);
    Stack<LocalStackElement> localStack = new Stack<LocalStackElement>();
    int maxEnd = captureIndices[0].getEnd();

    int i;
    for (i = 0; i < len; i++) {

      CaptureRule captureRule = captures[i];
      if (captureRule == null) {
        // Not interested
        continue;
      }

      IOnigCaptureIndex captureIndex = captureIndices[i];

      if (captureIndex.getLength() == 0) {
        // Nothing really captured
        continue;
      }

      if (captureIndex.getStart() > maxEnd) {
        // Capture going beyond consumed string
        break;
      }

      // pop captures while needed
      while (!localStack.isEmpty() && localStack.peek().endPos <= captureIndex.getStart()) {
        // pop!
        lineTokens.produceFromScopes(localStack.peek().scopes, localStack.peek().endPos);
        localStack.pop();
      }

      if (!localStack.isEmpty()) {
        lineTokens.produceFromScopes(localStack.peek().scopes, captureIndex.getStart());
      } else {
        lineTokens.produce(stack, captureIndex.getStart());
      }

      if (captureRule.retokenizeCapturedWithRuleId != null && captureRule.retokenizeCapturedWithRuleId != 0) {
        // the capture requires additional matching
        String scopeName = captureRule.getName(lineText, captureIndices);
        ScopeListElement nameScopesList = stack.contentNameScopesList.push(grammar, scopeName);
        String contentName = captureRule.getContentName(lineText, captureIndices);
        ScopeListElement contentNameScopesList = nameScopesList.push(grammar, contentName);

        StackElement stackClone = stack.push(captureRule.retokenizeCapturedWithRuleId, captureIndex.getStart(), null,
            nameScopesList, contentNameScopesList);

        _tokenizeString(grammar, lineText.substring(0, captureIndex.getEnd()), (isFirstLine && captureIndex.getStart() == 0),
            captureIndex.getStart(), stackClone, lineTokens);
        continue;
      }

      String captureRuleScopeName = captureRule.getName(lineText, captureIndices);
      if (captureRuleScopeName != null) {
        // push
        ScopeListElement base = localStack.size() > 0 ? localStack.peek().scopes : stack.contentNameScopesList;
        ScopeListElement captureRuleScopesList = base.push(grammar, captureRuleScopeName);
        localStack.push(new LocalStackElement(captureRuleScopesList, captureIndex.getEnd()));
      }
    }

    while (!localStack.isEmpty()) {
      // pop!
      lineTokens.produceFromScopes(localStack.peek().scopes, localStack.peek().endPos);
      localStack.pop();
    }
  }

  private static String debugCompiledRuleToString(ICompiledRule ruleScanner) {
    String[] r = new String[ruleScanner.rules.length];
    for (int i = 0, len = ruleScanner.rules.length; i < len; i++) {
      r[i] = "   - " + ruleScanner.rules[i] + ": " + ruleScanner.debugRegExps[i];
    }
    return String.join("\n", r);
  }

  private static IMatchInjectionsResult matchInjections(Injection[] injections, Grammar grammar, String lineText,
      boolean isFirstLine, int linePos, StackElement stack, int anchorPosition) {
    // The lower the better
    int bestMatchRating = Integer.MAX_VALUE;
    IOnigCaptureIndex[] bestMatchCaptureIndices = null;
    int bestMatchRuleId = 0;
    boolean bestMatchResultPriority = false;

    for (int i = 0, len = injections.length; i < len; i++) {
      Injection injection = injections[i];
      ICompiledRule ruleScanner = grammar.getRule(injection.ruleId).compile(grammar, null, isFirstLine,
          linePos == anchorPosition);
      IOnigNextMatchResult matchResult = ruleScanner.scanner._findNextMatchSync(lineText, linePos);
      if (IN_DEBUG_MODE) {
        System.out.println("  scanning for injections");
        System.out.println(debugCompiledRuleToString(ruleScanner));
      }

      if (matchResult == null) {
        continue;
      }

      int matchRating = matchResult.getCaptureIndices()[0].getStart();

      if (matchRating > bestMatchRating) {
        continue;
      } else if (matchRating == bestMatchRating && (!injection.priorityMatch || bestMatchResultPriority)) {
        continue;
      }

      bestMatchRating = matchRating;
      bestMatchCaptureIndices = matchResult.getCaptureIndices();
      bestMatchRuleId = ruleScanner.rules[matchResult.getIndex()];
      bestMatchResultPriority = injection.priorityMatch;

      if (bestMatchRating == linePos && bestMatchResultPriority) {
        // No more need to look at the rest of the injections
        break;
      }
    }

    if (bestMatchCaptureIndices != null) {
      return new IMatchInjectionsResult(bestMatchResultPriority, bestMatchCaptureIndices, bestMatchRuleId);
    }

    return null;
  }

  private static IMatchResult matchRule(Grammar grammar, String lineText, boolean isFirstLine, int linePos,
      StackElement stack, int anchorPosition) {
    Rule rule = stack.getRule(grammar);
    ICompiledRule ruleScanner = rule.compile(grammar, stack.endRule, isFirstLine, linePos == anchorPosition);
    IOnigNextMatchResult r = ruleScanner.scanner._findNextMatchSync(lineText, linePos);
    if (IN_DEBUG_MODE) {
      System.out.println("  scanning for");
      System.out.println(debugCompiledRuleToString(ruleScanner));
    }

    if (r != null) {
      return new IMatchResult(r.getCaptureIndices(), ruleScanner.rules[r.getIndex()]);
    }
    return null;
  }

  private static IMatchResult matchRuleOrInjections(Grammar grammar, String lineText, boolean isFirstLine, int linePos,
      StackElement stack, int anchorPosition) {
    // Look for normal grammar rule
    IMatchResult matchResult = matchRule(grammar, lineText, isFirstLine, linePos, stack, anchorPosition);

    // Look for injected rules
    Injection[] injections = grammar.getInjections(stack);
    if (injections.length == 0) {
      // No injections whatsoever => early return
      return matchResult;
    }

    IMatchInjectionsResult injectionResult = matchInjections(injections, grammar, lineText, isFirstLine, linePos, stack,
        anchorPosition);
    if (injectionResult == null) {
      // No injections matched => early return
      return matchResult;
    }

    if (matchResult == null) {
      // Only injections matched => early return
      return injectionResult;
    }

    // Decide if `matchResult` or `injectionResult` should win
    int matchResultScore = matchResult.captureIndices[0].getStart();
    int injectionResultScore = injectionResult.captureIndices[0].getStart();

    if (injectionResultScore < matchResultScore
        || (injectionResult.priorityMatch && injectionResultScore == matchResultScore)) {
      // injection won!
      return injectionResult;
    }
    return matchResult;
  }

  private static StackElement _tokenizeString(Grammar grammar, String lineText, boolean isFirstLine, int linePos,
      StackElement stack, LineTokens lineTokens) {
    final int lineLength = lineText.length();

    boolean STOP = false;

    IWhileCheckResult whileCheckResult = _checkWhileConditions(grammar, lineText, isFirstLine, linePos, stack, lineTokens);
    stack = whileCheckResult.stack;
    linePos = whileCheckResult.linePos;
    isFirstLine = whileCheckResult.isFirstLine;
    int anchorPosition = whileCheckResult.anchorPosition;

    while (!STOP) {
      if (IN_DEBUG_MODE) {
        System.out.println("");
        System.out.println("@@scanNext: |" + lineText.replaceAll("\\n$", "\\\\n").substring(linePos) + "|");
      }
      IMatchResult r = matchRuleOrInjections(grammar, lineText, isFirstLine, linePos, stack, anchorPosition);

      if (r == null) {
        if (IN_DEBUG_MODE) {
          System.out.println("  no more matches.");
        }
        // No match
        lineTokens.produce(stack, lineLength);
        STOP = true;
        break;
      }

      IOnigCaptureIndex[] captureIndices = r.captureIndices;
      int matchedRuleId = r.matchedRuleId;

      boolean hasAdvanced = (captureIndices != null && captureIndices.length > 0) ? (captureIndices[0].getEnd() > linePos)
          : false;

      if (matchedRuleId == -1) {
        // We matched the `end` for this rule => pop it
        BeginEndRule poppedRule = (BeginEndRule) stack.getRule(grammar);

        if (IN_DEBUG_MODE) {
          System.out.println("  popping " + poppedRule.debugName() + " - " + poppedRule.debugEndRegExp());
        }

        lineTokens.produce(stack, captureIndices[0].getStart());
        stack = stack.setContentNameScopesList(stack.nameScopesList);
        handleCaptures(grammar, lineText, isFirstLine, stack, lineTokens, poppedRule.endCaptures, captureIndices);
        lineTokens.produce(stack, captureIndices[0].getEnd());

        // pop
        StackElement popped = stack;
        stack = stack.pop();

        if (!hasAdvanced && popped.getEnterPos() == linePos) {
          // Grammar pushed & popped a rule without advancing
          System.err.println("[1] - Grammar is in an endless loop - Grammar pushed & popped a rule without advancing");

          // See https://github.com/Microsoft/vscode-textmate/issues/12
          // Let's assume this was a mistake by the grammar author and the intent was to continue in this state
          stack = popped;

          lineTokens.produce(stack, lineLength);
          STOP = true;
          break;
        }
      } else {
        // We matched a rule!
        Rule _rule = grammar.getRule(matchedRuleId);

        lineTokens.produce(stack, captureIndices[0].getStart());

        StackElement beforePush = stack;
        // push it on the stack rule
        String scopeName = _rule.getName(lineText, captureIndices);
        ScopeListElement nameScopesList = stack.contentNameScopesList.push(grammar, scopeName);
        stack = stack.push(matchedRuleId, linePos, null, nameScopesList, nameScopesList);

        if (_rule instanceof BeginEndRule) {
          BeginEndRule pushedRule = (BeginEndRule) _rule;
          if (IN_DEBUG_MODE) {
            System.out.println("  pushing " + pushedRule.debugName() + " - " + pushedRule.debugBeginRegExp());
          }

          handleCaptures(grammar, lineText, isFirstLine, stack, lineTokens, pushedRule.beginCaptures, captureIndices);
          lineTokens.produce(stack, captureIndices[0].getEnd());
          anchorPosition = captureIndices[0].getEnd();

          String contentName = pushedRule.getContentName(lineText, captureIndices);
          ScopeListElement contentNameScopesList = nameScopesList.push(grammar, contentName);
          stack = stack.setContentNameScopesList(contentNameScopesList);

          if (pushedRule.endHasBackReferences) {
            stack = stack.setEndRule(pushedRule.getEndWithResolvedBackReferences(lineText, captureIndices));
          }

          if (!hasAdvanced && beforePush.hasSameRuleAs(stack)) {
            // Grammar pushed the same rule without advancing
            System.err.println("[2] - Grammar is in an endless loop - Grammar pushed the same rule without advancing");
            stack = stack.pop();
            lineTokens.produce(stack, lineLength);
            STOP = true;
            break;
          }
        } else if (_rule instanceof BeginWhileRule) {
          BeginWhileRule pushedRule = (BeginWhileRule) _rule;
          if (IN_DEBUG_MODE) {
            System.out.println("  pushing " + pushedRule.debugName());
          }

          handleCaptures(grammar, lineText, isFirstLine, stack, lineTokens, pushedRule.beginCaptures, captureIndices);
          lineTokens.produce(stack, captureIndices[0].getEnd());
          anchorPosition = captureIndices[0].getEnd();
          String contentName = pushedRule.getContentName(lineText, captureIndices);
          ScopeListElement contentNameScopesList = nameScopesList.push(grammar, contentName);
          stack = stack.setContentNameScopesList(contentNameScopesList);

          if (pushedRule.whileHasBackReferences) {
            stack = stack.setEndRule(pushedRule.getWhileWithResolvedBackReferences(lineText, captureIndices));
          }

          if (!hasAdvanced && beforePush.hasSameRuleAs(stack)) {
            // Grammar pushed the same rule without advancing
            System.err.println("[3] - Grammar is in an endless loop - Grammar pushed the same rule without advancing");
            stack = stack.pop();
            lineTokens.produce(stack, lineLength);
            STOP = true;
            break;
          }
        } else {
          MatchRule matchingRule = (MatchRule) _rule;
          if (IN_DEBUG_MODE) {
            System.out.println("  matched " + matchingRule.debugName() + " - " + matchingRule.debugMatchRegExp());
          }

          handleCaptures(grammar, lineText, isFirstLine, stack, lineTokens, matchingRule.captures, captureIndices);
          lineTokens.produce(stack, captureIndices[0].getEnd());

          // pop rule immediately since it is a MatchRule
          stack = stack.pop();

          if (!hasAdvanced) {
            // Grammar is not advancing, nor is it pushing/popping
            System.err
                .println("[4] - Grammar is in an endless loop - Grammar is not advancing, nor is it pushing/popping");
            stack = stack.safePop();
            lineTokens.produce(stack, lineLength);
            STOP = true;
            break;
          }
        }
      }

      if (captureIndices[0].getEnd() > linePos) {
        // Advance stream
        linePos = captureIndices[0].getEnd();
        isFirstLine = false;
      }
    }

    return stack;
  }

  private int _rootId;
  private int _lastRuleId;
  private final ArrayList<Rule> _ruleId2desc;
  private final Map<String, IRawGrammar> _includedGrammars;
  private final IGrammarRepository _grammarRepository;
  private final IRawGrammar _grammar;
  private ArrayList<Injection> _injections;
  private final ScopeMetadataProvider _scopeMetadataProvider;

  public Grammar(IRawGrammar grammar, int initialLanguage, IEmbeddedLanguagesMap embeddedLanguages,
      IGrammarRepositoryAndIThemeProvider grammarRepository) {
    this._scopeMetadataProvider = new ScopeMetadataProvider(initialLanguage, grammarRepository, embeddedLanguages);

    this._rootId = -1;
    this._lastRuleId = 0;
    this._ruleId2desc = new ArrayList<Rule>();
    this._includedGrammars = new HashMap<String, IRawGrammar>();
    this._grammarRepository = grammarRepository;
    this._grammar = initGrammar(grammar, null);
  }

  public void onDidChangeTheme() {
    this._scopeMetadataProvider.onDidChangeTheme();
  }

  public ScopeMetadata getMetadataForScope(String scope) {
    return this._scopeMetadataProvider.getMetadataForScope(scope);
  }

  public Injection[] getInjections(StackElement states) {
    if (this._injections == null) {
      this._injections = new ArrayList<Injection>();
      // add injections from the current grammar
      Map<String, IRawRule> rawInjections = this._grammar.injections;
      if (rawInjections != null) {
        for (String expression : rawInjections.keySet()) {
          collectInjections(this._injections, expression, rawInjections.get(expression), this, this._grammar);
        }
      }

      // add injection grammars contributed for the current scope
      if (this._grammarRepository != null) {
        String[] injectionScopeNames = this._grammarRepository.injections(this._grammar.scopeName);
        if (injectionScopeNames != null) {
          for (String injectionScopeName : injectionScopeNames) {
            IRawGrammar injectionGrammar = this.getExternalGrammar(injectionScopeName);
            if (injectionGrammar != null) {
              String selector = injectionGrammar.injectionSelector;
              if (selector != null) {
                collectInjections(this._injections, selector, null, this, injectionGrammar);
              }
            }
          }
        }
      }
    }
    if (this._injections.size() == 0) {
      return new Injection[0];
    }

    ArrayList<Injection> result = new ArrayList<Injection>();
    for (Injection injection : this._injections) {
      if (injection.matcher.call(states)) {
        result.add(injection);
      }
    }
    return result.toArray(new Injection[0]);
  }

  public Rule registerRule(Function<Integer, Rule> factory) {
    int id = (++this._lastRuleId);
    Rule result = factory.apply(id);

    for (int i = this._ruleId2desc.size(); i <= id; i++) {
      this._ruleId2desc.add(null);
    }

    this._ruleId2desc.set(id, result);
    return result;
  }

  public Rule getRule(int patternId) {
    if (patternId <= 0 || patternId >= this._ruleId2desc.size())
      return null;
    return this._ruleId2desc.get(patternId);
  }

  public IRawGrammar getExternalGrammar(String scopeName) {
    return this.getExternalGrammar(scopeName, null);
  }

  public IRawGrammar getExternalGrammar(String scopeName, IRawRepository repository) {
    if (this._includedGrammars.containsKey(scopeName)) {
      return this._includedGrammars.get(scopeName);
    } else if (this._grammarRepository != null) {
      IRawGrammar rawIncludedGrammar = this._grammarRepository.lookup(scopeName);
      if (rawIncludedGrammar != null) {
        // console.log('LOADED GRAMMAR ' + pattern.include);
        this._includedGrammars.put(scopeName,
            initGrammar(rawIncludedGrammar, repository != null ? repository.base() : null));
        return this._includedGrammars.get(scopeName);
      }
    }

    return null;
  }

  public ITokenizeLineResult tokenizeLine(String lineText, StackElement prevState) {
    TokenizeReturn r = this._tokenize(lineText, prevState, false);

    ITokenizeLineResult result = new ITokenizeLineResult();

    result.tokens = r.lineTokens.getResult(r.ruleStack, r.lineLength);
    result.ruleStack = r.ruleStack;
    return result;
  }

  public ITokenizeLineResult2 tokenizeLine2(String lineText, StackElement prevState) {
    TokenizeReturn r = this._tokenize(lineText, prevState, true);
    ITokenizeLineResult2 result = new ITokenizeLineResult2();
    result.tokens = r.lineTokens.getBinaryResult(r.ruleStack, r.lineLength);
    result.ruleStack = r.ruleStack;
    return result;
  }

  private TokenizeReturn _tokenize(String lineText, StackElement prevState, boolean emitBinaryTokens) {
    if (this._rootId == -1) {
      this._rootId = RuleFactory.getCompiledRuleId(this._grammar.repository.self(), this, this._grammar.repository);
    }

    boolean isFirstLine;
    if (prevState == null || prevState == StackElement.NULL) {
      isFirstLine = true;
      ScopeMetadata rawDefaultMetadata = this._scopeMetadataProvider.getDefaultMetadata();
      ThemeTrieElementRule defaultTheme = rawDefaultMetadata.themeData[0];
      int defaultMetadata = StackElementMetadata.set(0, rawDefaultMetadata.languageId, rawDefaultMetadata.tokenType,
          defaultTheme.fontStyle, defaultTheme.foreground, defaultTheme.background);

      String rootScopeName = this.getRule(this._rootId).getName(null, null);
      ScopeMetadata rawRootMetadata = this._scopeMetadataProvider.getMetadataForScope(rootScopeName);
      int rootMetadata = ScopeListElement.mergeMetadata(defaultMetadata, null, rawRootMetadata);

      ScopeListElement scopeList = new ScopeListElement(null, rootScopeName, rootMetadata);

      prevState = new StackElement(null, this._rootId, -1, null, scopeList, scopeList);
    } else {
      isFirstLine = false;
      prevState.reset();
    }

    lineText = lineText + '\n';
    String onigLineText = lineText;
    int lineLength = lineText.length();
    LineTokens lineTokens = new LineTokens(emitBinaryTokens, lineText);
    StackElement nextState = _tokenizeString(this, onigLineText, isFirstLine, 0, prevState, lineTokens);

    TokenizeReturn result = new TokenizeReturn();
    result.lineLength = lineLength;
    result.lineTokens = lineTokens;
    result.ruleStack = nextState;
    return result;
  }
}

class IWhileStack {
  public StackElement stack;
  public BeginWhileRule rule;

  public IWhileStack(StackElement stack, BeginWhileRule rule) {
    this.stack = stack;
    this.rule = rule;
  }
}

class IWhileCheckResult {
  public StackElement stack;
  public int linePos;
  public int anchorPosition;
  public boolean isFirstLine;

  public IWhileCheckResult(StackElement stack, int linePos, int anchorPosition, boolean isFirstLine) {
    this.stack = stack;
    this.linePos = linePos;
    this.anchorPosition = anchorPosition;
    this.isFirstLine = isFirstLine;
  }
}

class IMatchResult {
  public IOnigCaptureIndex[] captureIndices;
  public int matchedRuleId;

  public IMatchResult(IOnigCaptureIndex[] captureIndices, int matchedRuleId) {
    this.captureIndices = captureIndices;
    this.matchedRuleId = matchedRuleId;
  }
}

class IMatchInjectionsResult extends IMatchResult {
  public boolean priorityMatch;

  public IMatchInjectionsResult(boolean priorityMatch, IOnigCaptureIndex[] captureIndices, int matchedRuleId) {
    super(captureIndices, matchedRuleId);
    this.priorityMatch = priorityMatch;
  }
}

class TokenizeReturn {
  public int lineLength;
  public LineTokens lineTokens;
  public StackElement ruleStack;
}

class LineTokens {

  private final boolean _emitBinaryTokens;
  /**
   * defined only if `IN_DEBUG_MODE`.
   */
  private final String _lineText;
  /**
   * used only if `_emitBinaryTokens` is false.
   */
  private final ArrayList<IToken> _tokens;
  /**
   * used only if `_emitBinaryTokens` is true.
   */
  private final ArrayList<Integer> _binaryTokens;

  private int _lastTokenEndIndex;

  public static boolean IN_DEBUG_MODE = false;

  public LineTokens(boolean emitBinaryTokens, String lineText) {
    this._emitBinaryTokens = emitBinaryTokens;
    if (IN_DEBUG_MODE) {
      this._lineText = lineText;
    } else {
      this._lineText = null;
    }

    if (this._emitBinaryTokens) {
      this._binaryTokens = new ArrayList<Integer>();
      this._tokens = null;
    } else {
      this._binaryTokens = null;
      this._tokens = new ArrayList<IToken>();
    }
    this._lastTokenEndIndex = 0;
  }

  public void produce(StackElement stack, int endIndex) {
    this.produceFromScopes(stack.contentNameScopesList, endIndex);
  }

  public void produceFromScopes(ScopeListElement scopesList, int endIndex) {
    if (this._lastTokenEndIndex >= endIndex) {
      return;
    }

    if (this._emitBinaryTokens) {
      int metadata = scopesList.metadata;
      if (this._binaryTokens.size() > 0 && this._binaryTokens.get(this._binaryTokens.size() - 1) == metadata) {
        // no need to push a token with the same metadata
        this._lastTokenEndIndex = endIndex;
        return;
      }

      this._binaryTokens.add(this._lastTokenEndIndex);
      this._binaryTokens.add(metadata);

      this._lastTokenEndIndex = endIndex;
      return;
    }

    String[] scopes = scopesList.generateScopes();

    if (IN_DEBUG_MODE) {
      System.out.println(
          "  token: |" + this._lineText.substring(this._lastTokenEndIndex, endIndex).replaceAll("\n$", "\\n") + '|');
      for (int k = 0; k < scopes.length; k++) {
        System.out.println("      * " + scopes[k]);
      }
    }

    this._tokens.add(new IToken(this._lastTokenEndIndex, endIndex, scopes));

    this._lastTokenEndIndex = endIndex;
  }

  public IToken[] getResult(StackElement stack, int lineLength) {
    if (this._tokens.size() > 0 && this._tokens.get(this._tokens.size() - 1).startIndex == lineLength - 1) {
      // pop produced token for newline
      this._tokens.remove(this._tokens.size() - 1);
    }

    if (this._tokens.size() == 0) {
      this._lastTokenEndIndex = -1;
      this.produce(stack, lineLength);
      this._tokens.get(this._tokens.size() - 1).startIndex = 0;
    }

    return this._tokens.toArray(new IToken[0]);
  }

  public int[] getBinaryResult(StackElement stack, int lineLength) {
    if (this._binaryTokens.size() > 0 && this._binaryTokens.get(this._binaryTokens.size() - 2) == lineLength - 1) {
      // pop produced token for newline
      this._binaryTokens.remove(this._binaryTokens.size() - 1);
      this._binaryTokens.remove(this._binaryTokens.size() - 1);
    }

    if (this._binaryTokens.size() == 0) {
      this._lastTokenEndIndex = -1;
      this.produce(stack, lineLength);
      this._binaryTokens.set(this._binaryTokens.size() - 2, 0);
    }

    int[] result = new int[this._binaryTokens.size()];
    for (int i = 0, len = this._binaryTokens.size(); i < len; i++) {
      result[i] = this._binaryTokens.get(i);
    }

    return result;
  }
}
