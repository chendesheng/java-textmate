package me.textmate.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.textmate.theme.FontStyle;
import me.textmate.theme.ThemeTrieElementRule;

public class ScopeListElement {
	// _scopeListElementBrand: void;

	public final ScopeListElement parent;
	public final String scope;
	public final int metadata;

	public ScopeListElement(ScopeListElement parent, String scope, int metadata) {
		this.parent = parent;
		this.scope = scope;
		this.metadata = metadata;
	}

	private static boolean _equals(ScopeListElement a, ScopeListElement b) {
		do {
			if (a == b) {
				return true;
			}

			if (a.scope != b.scope || a.metadata != b.metadata) {
				return false;
			}

			// Go to previous pair
			a = a.parent;
			b = b.parent;

			if (a == null && b == null) {
				// End of list reached for both
				return true;
			}

			if (a == null || b == null) {
				// End of list reached only for one
				return false;
			}

		} while (true);
	}

	public boolean equals(ScopeListElement other) {
		return ScopeListElement._equals(this, other);
	}

	private static boolean _matchesScope(String scope, String selector, String selectorWithDot) {
		return (selector == scope || scope.substring(0, selectorWithDot.length()) == selectorWithDot);
	}

	private static boolean _matches(ScopeListElement target, String[] parentScopes) {
		if (parentScopes == null) {
			return true;
		}

		int len = parentScopes.length;
		int index = 0;
		String selector = parentScopes[index];
		String selectorWithDot = selector + ".";

		while (target != null) {
			if (_matchesScope(target.scope, selector, selectorWithDot)) {
				index++;
				if (index == len) {
					return true;
				}
				selector = parentScopes[index];
				selectorWithDot = selector + '.';
			}
			target = target.parent;
		}

		return false;
	}

	public static int mergeMetadata(int metadata, ScopeListElement scopesList, ScopeMetadata source) {
		if (source == null) {
			return metadata;
		}

		int fontStyle = FontStyle.NotSet;
		int foreground = 0;
		int background = 0;

		if (source.themeData != null) {
			// Find the first themeData that matches
			for (int i = 0, len = source.themeData.length; i < len; i++) {
				ThemeTrieElementRule themeData = source.themeData[i];

				if (_matches(scopesList, themeData.parentScopes)) {
					fontStyle = themeData.fontStyle;
					foreground = themeData.foreground;
					background = themeData.background;
					break;
				}
			}
		}

		return StackElementMetadata.set(metadata, source.languageId, source.tokenType, fontStyle, foreground, background);
	}

	private static ScopeListElement _push(ScopeListElement target, Grammar grammar, String[] scopes) {
		for (int i = 0, len = scopes.length; i < len; i++) {
			String scope = scopes[i];
			ScopeMetadata rawMetadata = grammar.getMetadataForScope(scope);
			int metadata = ScopeListElement.mergeMetadata(target.metadata, target, rawMetadata);
			target = new ScopeListElement(target, scope, metadata);
		}
		return target;
	}

	public ScopeListElement push(Grammar grammar, String scope) {
		if (scope == null) {
			return this;
		}
		if (scope.indexOf(" ") >= 0) {
			// there are multiple scopes to push
			return ScopeListElement._push(this, grammar, scope.split(" "));
		}
		// there is a single scope to push
		return ScopeListElement._push(this, grammar, new String[] { scope });
	}

	private static String[] _generateScopes(ScopeListElement scopesList) {
		List<String> result = new ArrayList<String>();
		while (scopesList != null) {
			result.add(scopesList.scope);
			scopesList = scopesList.parent;
		}
		Collections.reverse(result);
		return result.toArray(new String[0]);
	}

	public String[] generateScopes() {
		return ScopeListElement._generateScopes(this);
	}
}