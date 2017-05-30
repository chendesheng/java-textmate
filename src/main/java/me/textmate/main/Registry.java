package me.textmate.main;

import me.textmate.registry.SyncRegistry;
import me.textmate.theme.Theme;
import me.textmate.types.IRawGrammar;
import me.textmate.grammarReader.GrammarReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The registry that will hold all gramars.
 */
public class Registry {
	private RegistryOptions _locator;
	private SyncRegistry _syncRegistry;

	private static RegistryOptions DEFAULT_OPTIONS = new RegistryOptionsNull();

	public Registry(RegistryOptions locator) {
		this._locator = locator;
		this._syncRegistry = new SyncRegistry(Theme.createFromRawTheme(locator.theme));
	}

	public Registry() {
		this(DEFAULT_OPTIONS);
	}

	/**
		 * Change the theme. Once called, no previous `ruleStack` should be used anymore.
		 */
	public void setTheme(IRawTheme theme) {
		this._syncRegistry.setTheme(Theme.createFromRawTheme(theme));
	}

	/**
		 * Returns a lookup array for color ids.
		 */
	public String[] getColorMap() {
		return this._syncRegistry.getColorMap();
	}

	/**
		 * Load the grammar for `scopeName` and all referenced included grammars asynchronously.
		 * Please do not use language id 0.
		 */
	public IGrammar loadGrammarWithEmbeddedLanguages(String initialScopeName, int initialLanguage,
			IEmbeddedLanguagesMap embeddedLanguages) throws Exception {
		this._loadGrammar(initialScopeName);
		return this.grammarForScopeName(initialScopeName, initialLanguage, embeddedLanguages);
	}

	/**
	 * Load the grammar for `scopeName` and all referenced included grammars asynchronously.
	 */
	public IGrammar loadGrammar(final String initialScopeName) throws Exception {
		this._loadGrammar(initialScopeName);
		return this.grammarForScopeName(initialScopeName);
	}

	private void _loadGrammar(String initialScopeName) throws Exception {
		List<String> remainingScopeNames = new ArrayList<String>();
		remainingScopeNames.add(initialScopeName);

		Map<String, Boolean> seenScopeNames = new HashMap<String, Boolean>();
		seenScopeNames.put(initialScopeName, true);

		while (remainingScopeNames.size() > 0) {
			String scopeName = remainingScopeNames.remove(0);

			if (this._syncRegistry.lookup(scopeName) != null) {
				continue;
			}

			String filePath = this._locator.getFilePath(scopeName);
			if (filePath != null) {
				if (scopeName == initialScopeName) {
					throw new Exception("Unknown location for grammar <" + initialScopeName + ">");
				}
				continue;
			}

			try {
				IRawGrammar grammar = GrammarReader.readGrammarSync(filePath);
				String[] injections = this._locator.getInjections(scopeName);

				String[] deps = this._syncRegistry.addGrammar(grammar, injections);
				for (String dep : deps) {
					if (!seenScopeNames.get(dep)) {
						seenScopeNames.put(dep, true);
						remainingScopeNames.add(dep);
					}
				}
			} catch (Exception err) {
				if (scopeName == initialScopeName) {
					throw err;
				}
			}
		}
	}

	/**
	 * Load the grammar at `path` synchronously.
	 */
	public IGrammar loadGrammarFromPathSync(String path) throws Exception {
		return loadGrammarFromPathSync(path, 0, null);
	}

	public IGrammar loadGrammarFromPathSync(String path, int initialLanguage, IEmbeddedLanguagesMap embeddedLanguages)
			throws Exception {
		IRawGrammar rawGrammar = GrammarReader.readGrammarSync(path);
		String[] injections = this._locator.getInjections(rawGrammar.scopeName);
		this._syncRegistry.addGrammar(rawGrammar, injections);
		return this.grammarForScopeName(rawGrammar.scopeName, initialLanguage, embeddedLanguages);
	}

	public IGrammar grammarForScopeName(String scopeName) {
		return this.grammarForScopeName(scopeName, 0, null);
	}

	/**
	 * Get the grammar for `scopeName`. The grammar must first be created via `loadGrammar` or `loadGrammarFromPathSync`.
	 */
	public IGrammar grammarForScopeName(String scopeName, int initialLanguage, IEmbeddedLanguagesMap embeddedLanguages) {
		return this._syncRegistry.grammarForScopeName(scopeName, initialLanguage, embeddedLanguages);
	}
}