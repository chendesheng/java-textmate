package me.textmate.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import me.textmate.types.ILocation;
import me.textmate.types.IRawCaptures;
import me.textmate.types.IRawGrammar;
import me.textmate.types.IRawRepository;
import me.textmate.types.IRawRule;
import me.textmate.utils.Utils;

public class RuleFactory {

  public static CaptureRule createCaptureRule(IRuleFactoryHelper helper, final ILocation location, final String name,
      final String contentName, final int retokenizeCapturedWithRuleId) {
    return (CaptureRule) helper.registerRule(new Function<Integer, Rule>() {
      public Rule apply(Integer id) {
        return new CaptureRule(location, id, name, contentName, retokenizeCapturedWithRuleId);
      }
    });
  }

  public static int getCompiledRuleId(final IRawRule desc, final IRuleFactoryHelper helper, final IRawRepository repository) {
    if (desc.id == null) {
      helper.registerRule(new Function<Integer, Rule>() {
        public Rule apply(Integer id) {
          desc.id = id;

          if (desc.match != null) {
            return new MatchRule(desc.vscodeTextmateLocation, desc.id, desc.name, desc.match,
                RuleFactory._compileCaptures(desc.captures, helper, repository));
          }

          if (desc.begin == null) {

            IRawRepository repo = repository;
            if (desc.repository != null) {
              repo = Utils.mergeObjects(repo, desc.repository);
            }

            return new IncludeOnlyRule(desc.vscodeTextmateLocation, desc.id, desc.name, desc.contentName,
                RuleFactory._compilePatterns(desc.patterns, helper, repo));
          }

          if (desc.whileq != null) {
            return new BeginWhileRule(desc.vscodeTextmateLocation, desc.id, desc.name, desc.contentName, desc.begin,
                RuleFactory._compileCaptures(desc.beginCaptures != null ? desc.beginCaptures : desc.captures, helper,
                    repository),
                desc.whileq,
                RuleFactory._compileCaptures(desc.whileCaptures != null ? desc.whileCaptures : desc.captures, helper,
                    repository),
                RuleFactory._compilePatterns(desc.patterns, helper, repository));
          }

          return new BeginEndRule(desc.vscodeTextmateLocation, desc.id, desc.name, desc.contentName, desc.begin,
              RuleFactory._compileCaptures(desc.beginCaptures != null ? desc.beginCaptures : desc.captures, helper,
                  repository),
              desc.end,
              RuleFactory._compileCaptures(desc.endCaptures != null ? desc.endCaptures : desc.captures, helper,
                  repository),
              desc.applyEndPatternLast, RuleFactory._compilePatterns(desc.patterns, helper, repository));
        }
      });
    }

    return desc.id;
  }

  private static CaptureRule[] _compileCaptures(IRawCaptures captures, IRuleFactoryHelper helper,
      IRawRepository repository) {
    CaptureRule[] r = new CaptureRule[0];
    int numericCaptureId;
    int maximumCaptureId;
    int i;

    if (captures != null) {
      // Find the maximum capture id
      maximumCaptureId = 0;

      for (String captureId : captures.keySet()) {
        numericCaptureId = Integer.parseInt(captureId, 10);
        if (numericCaptureId > maximumCaptureId) {
          maximumCaptureId = numericCaptureId;
        }
      }

      // Initialize result
      r = new CaptureRule[maximumCaptureId + 1];
      for (i = 0; i <= maximumCaptureId; i++) {
        r[i] = null;
      }

      // Fill out result
      for (String captureId : captures.keySet()) {
        numericCaptureId = Integer.parseInt(captureId, 10);
        int retokenizeCapturedWithRuleId = 0;
        if (captures.get(captureId).patterns != null) {
          retokenizeCapturedWithRuleId = RuleFactory.getCompiledRuleId(captures.get(captureId), helper, repository);
        }
        IRawRule capture = captures.get(captureId);
        r[numericCaptureId] = RuleFactory.createCaptureRule(helper, capture.vscodeTextmateLocation, capture.name,
            capture.contentName, retokenizeCapturedWithRuleId);
      }
    }

    return r;
  }

  private static ICompilePatternsResult _compilePatterns(IRawRule[] patterns, IRuleFactoryHelper helper,
      IRawRepository repository) {
    List<Integer> r = new ArrayList<Integer>();
    IRawRule pattern;
    int i;
    int len;
    int patternId;
    IRawGrammar externalGrammar;
    Rule rule;
    boolean skipRule;

    if (patterns != null) {
      for (i = 0, len = patterns.length; i < len; i++) {
        pattern = patterns[i];
        patternId = -1;

        if (pattern.include != null) {
          if (pattern.include.charAt(0) == '#') {
            // Local include found in `repository`
            IRawRule localIncludedRule = repository.get(pattern.include.substring(1));
            if (localIncludedRule != null) {
              patternId = RuleFactory.getCompiledRuleId(localIncludedRule, helper, repository);
            } else {
              // console.warn('CANNOT find rule for scopeName: ' + pattern.include + ', I am: ', repository['$base'].name);
            }
          } else if (pattern.include.equals("$base") || pattern.include.equals("$self")) {
            // Special include also found in `repository`
            patternId = RuleFactory.getCompiledRuleId(repository.get(pattern.include), helper, repository);
          } else {
            String externalGrammarName = null;
            String externalGrammarInclude = null;
            int sharpIndex = pattern.include.indexOf('#');
            if (sharpIndex >= 0) {
              externalGrammarName = pattern.include.substring(0, sharpIndex);
              externalGrammarInclude = pattern.include.substring(sharpIndex + 1);
            } else {
              externalGrammarName = pattern.include;
            }
            // External include
            externalGrammar = helper.getExternalGrammar(externalGrammarName, repository);

            if (externalGrammar != null) {
              if (externalGrammarInclude != null) {
                IRawRule externalIncludedRule = externalGrammar.repository.get(externalGrammarInclude);
                if (externalIncludedRule != null) {
                  patternId = RuleFactory.getCompiledRuleId(externalIncludedRule, helper, externalGrammar.repository);
                } else {
                  // console.warn('CANNOT find rule for scopeName: ' + pattern.include + ', I am: ', repository['$base'].name);
                }
              } else {
                patternId = RuleFactory.getCompiledRuleId(externalGrammar.repository.self(), helper,
                    externalGrammar.repository);
              }
            } else {
              // console.warn('CANNOT find grammar for scopeName: ' + pattern.include + ', I am: ', repository['$base'].name);
            }

          }
        } else {
          patternId = RuleFactory.getCompiledRuleId(pattern, helper, repository);
        }

        if (patternId != -1) {
          rule = helper.getRule(patternId);

          skipRule = false;

          if (rule instanceof IncludeOnlyRule) {
            if (((IncludeOnlyRule) rule).hasMissingPatterns && ((IncludeOnlyRule) rule).patterns.length == 0) {
              skipRule = true;
            }
          }
          if (rule instanceof BeginEndRule) {
            if (((BeginEndRule) rule).hasMissingPatterns && ((BeginEndRule) rule).patterns.length == 0) {
              skipRule = true;
            }
          }
          if (rule instanceof BeginWhileRule) {
            if (((BeginWhileRule) rule).hasMissingPatterns && ((BeginWhileRule) rule).patterns.length == 0) {
              skipRule = true;
            }
          }
          if (skipRule) {
            // console.log('REMOVING RULE ENTIRELY DUE TO EMPTY PATTERNS THAT ARE MISSING');
            continue;
          }

          r.add(patternId);
        }
      }
    }

    ICompilePatternsResult result = new ICompilePatternsResult();
    result.patterns = new int[r.size()];
    for (i = 0; i < result.patterns.length; i++) {
      result.patterns[i] = r.get(i);
    }
    result.hasMissingPatterns = ((patterns != null ? patterns.length : 0) != r.size());
    return result;
  }
}
