/**
 *
 * @author wrzodek
 */
package de.zbit.kegg;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * @author wrzodek
 * @author Andreas Dr&auml;ger
 */
public abstract interface KEGGtranslatorOptions extends KeyProvider {
  
  /*
   * Generic translation options
   */
  /**  
   * If true, remove all nodes that have no edges, before translating the pathway.
   */
  public static final Option<Boolean> REMOVE_ORPHANS = new Option<Boolean>("REMOVE_ORPHANS",Boolean.class,
      "If true, remove all nodes that have no edges, before translating the pathway.", (short) 2, "-ro", false);

  /**
   * If true, shows only short names of all KEGG entries.
   */
  public static final Option<Boolean> SHORT_NAMES = new Option<Boolean>("SHORT_NAMES",Boolean.class,
      "If true, shows only short names of all KEGG entries.", true);
  
  /**
   * If true, removes all gene-nodes in the KEGG document, which are white.
   */
  public static final Option<Boolean> REMOVE_WHITE_GENE_NODES = new Option<Boolean>("REMOVE_WHITE_GENE_NODES",Boolean.class,
      "If true, removes all gene-nodes in the KEGG document, which are white.", true);

  /**
   * If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document. 
   */
  public static final Option<Boolean> AUTOCOMPLETE_REACTIONS = new Option<Boolean>("AUTOCOMPLETE_REACTIONS",Boolean.class,
      "If true, automatically looks for missing reactants and enzymes of reactions and adds them to the document.", (short) 2, "-ar", true);
  
  /**
   * If true, no additional information will be retrieved from the KEGG-Server.
   */
  // =! retrieveKeggAnnots
  public static final Option<Boolean> OFFLINE_MODE = new Option<Boolean>("OFFLINE_MODE",Boolean.class,
      "If true, no additional information will be retrieved from the KEGG-Server.", false);

  /**
   * Define various options that are used in all translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> GENERIC_OPTIONS = new OptionGroup<Boolean>(
      "Generic translation options",
      "Define various options that are used in all translations.",
      REMOVE_ORPHANS, SHORT_NAMES, REMOVE_WHITE_GENE_NODES, AUTOCOMPLETE_REACTIONS, OFFLINE_MODE);

  /*
   * Graphical, yFiles based translations
   */
  
  /**
   * If true, merges all nodes that have exactly the same relations (sources, targets and types). 
   */
  public static final Option<Boolean> MERGE_NODES_WITH_SAME_EDGES = new Option<Boolean>("MERGE_NODES_WITH_SAME_EDGES",Boolean.class,
      "If true, merges all nodes that have exactly the same relations (sources, targets and types).", (short) 2, "--merge", false);
  
  /**
   * Define various options that are used in yFiles based translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> GRAPH_OPTIONS = new OptionGroup<Boolean>(
      "Translation options for graphical outputs",
      "Define various options that are used in yFiles based translations.",
      MERGE_NODES_WITH_SAME_EDGES);
  
  /*
   * Funcional, SBML based translations
   */
  
  /**
   * If true, adds celldesigner annotations to the SBML-XML document.
   */
  public static final Option<Boolean> CELLDESIGNER_ANNOTATIONS = new Option<Boolean>("CELLDESIGNER_ANNOTATIONS",Boolean.class,
      "If true, adds celldesigner annotations to the SBML-XML document.", (short) 2, "-cd", false);
  
  /**
   * Define various options that are used in SBML based translations.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Boolean> SBML_OPTIONS = new OptionGroup<Boolean>(
      "Translation options for SBML outputs",
      "Define various options that are used in SBML based translations.",
      CELLDESIGNER_ANNOTATIONS);
  
}