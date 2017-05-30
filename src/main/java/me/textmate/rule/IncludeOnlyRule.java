package me.textmate.rule;

import me.textmate.types.ILocation;

public class IncludeOnlyRule extends Rule {
	public final boolean hasMissingPatterns;
	public final int[] patterns;
	private RegExpSourceList _cachedCompiledPatterns;

	public IncludeOnlyRule(ILocation location, int id, String name, String contentName, ICompilePatternsResult patterns) {
		super(location, id, name, contentName);
		this.patterns = patterns.patterns;
		this.hasMissingPatterns = patterns.hasMissingPatterns;
		this._cachedCompiledPatterns = null;
	}

	public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
		int i;
		int len;
		Rule rule;

		for (i = 0, len = this.patterns.length; i < len; i++) {
			rule = grammar.getRule(this.patterns[i]);
			rule.collectPatternsRecursive(grammar, out, false);
		}
	}

	public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
		if (this._cachedCompiledPatterns == null) {
			this._cachedCompiledPatterns = new RegExpSourceList();
			this.collectPatternsRecursive(grammar, this._cachedCompiledPatterns, true);
		}
		return this._cachedCompiledPatterns.compile(grammar, allowA, allowG);
	}
}