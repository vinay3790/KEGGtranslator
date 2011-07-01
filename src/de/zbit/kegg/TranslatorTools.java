/*
 * $Id:  TranslatorTools.java 19:03:26 wrzodek $
 * $URL: TranslatorTools.java $
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import y.base.DataMap;
import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGG2yGraph;

/**
 * This class is intended to provide various translator tools.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorTools {
  public static final transient Logger log = Logger.getLogger(TranslatorTools.class.getName());
  
  /**
   * A graph on which operations are performed.
   */
  Graph2D graph;
  
  public TranslatorTools(TranslatorPanel tp){
    this(tp.isGraphML()?(Graph2D) tp.getDocument():null);
  }
  
  public TranslatorTools(Graph2D graph){
    super();
    this.graph=graph;
    if (this.graph==null) log.warning("Graph is null!");
  }
  
  
  /**
   * Highlight all given GeneIDs in RED color. And selects these nodes.
   * @param graph translated pathway with annotated geneIDs
   * @param ncbiGeneIDs geneIDs to color in Red.
   */
  public void highlightGenes(Iterable<Integer> ncbiGeneIDs) {
    Map<Integer, List<Node>> id2node = getGeneID2NodeMap();
    for (Integer integer : ncbiGeneIDs) {
      List<Node> nList = id2node.get(integer);
      if (nList!=null) {
        for (Node node : nList) {
          graph.getRealizer(node).setFillColor(Color.RED);
          graph.getRealizer(node).setSelected(true);
        }
      } else {
        log.info("Could not get a Node for " + integer);
      }
    }
    
  }

  /**
   * Return a map from Entrez GeneID to corresponding {@link Node} for the given
   * translated pathway.
   * @param graph
   * @return map from geneID to List of nodes.
   */
  @SuppressWarnings("unchecked")
  public Map<Integer, List<Node>> getGeneID2NodeMap() {
    // Build a map from GeneID 2 Node
    Map<Integer, List<Node>> id2node = new HashMap<Integer, List<Node>>();
    
    // Get the NodeMap from entrez 2 node.
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    NodeMap entrez = null;
    if (mapDescriptionMap==null) return null;
    for (int i=0; i<graph.getRegisteredNodeMaps().length; i++) {
      NodeMap nm = graph.getRegisteredNodeMaps()[i];
      if (mapDescriptionMap.getV(nm).equals("entrezIds")) {
        entrez = nm;
        break;
      }
    }
    
    // build the resulting map
    for (Node n : graph.getNodeArray()) {
      Object entrezIds = entrez.get(n);
      if (entrezIds!=null && entrezIds.toString().length()>0) {
        String[] ids = entrezIds.toString().split(",");
        for (String id: ids) {
          try {
            // Get Node collection for gene ID
            Integer intId = Integer.parseInt(id);
            List<Node> list = id2node.get(intId);
            if (list==null) {
              list = new LinkedList<Node>();
              id2node.put(intId, list);
            }
            // Add node to list.
            list.add(n);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Could not get geneID for node.", e);
          }
        }
      }
    }
    
    return id2node;
  }
  
}