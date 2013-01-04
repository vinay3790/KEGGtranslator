/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2010-2013 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.io;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level2.Direction;
import org.biopax.paxtools.model.level2.InteractionParticipant;
import org.biopax.paxtools.model.level2.bioSource;
import org.biopax.paxtools.model.level2.biochemicalReaction;
import org.biopax.paxtools.model.level2.catalysis;
import org.biopax.paxtools.model.level2.complex;
import org.biopax.paxtools.model.level2.complexAssembly;
import org.biopax.paxtools.model.level2.conversion;
import org.biopax.paxtools.model.level2.dataSource;
import org.biopax.paxtools.model.level2.dna;
import org.biopax.paxtools.model.level2.entity;
import org.biopax.paxtools.model.level2.openControlledVocabulary;
import org.biopax.paxtools.model.level2.pathway;
import org.biopax.paxtools.model.level2.physicalEntity;
import org.biopax.paxtools.model.level2.physicalEntityParticipant;
import org.biopax.paxtools.model.level2.physicalInteraction;
import org.biopax.paxtools.model.level2.protein;
import org.biopax.paxtools.model.level2.rna;
import org.biopax.paxtools.model.level2.sequenceEntity;
import org.biopax.paxtools.model.level2.sequenceParticipant;
import org.biopax.paxtools.model.level2.smallMolecule;
import org.biopax.paxtools.model.level2.xref;

import de.zbit.kegg.Translator;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.kegg.parser.pathway.ReactionType;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.RelationType;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.kegg.parser.pathway.ext.EntryTypeExtended;
import de.zbit.util.ArrayUtils;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.Utils;

/**
 * KEGG2BioPAX level 2 converter (also called KGML2BioPAX). 
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2BioPAX_level2 extends KEGG2BioPAX {
  
  /**
   * The root {@link pathway} of our BioPAX conversion.
   */
  private pathway pathway;

  /**
   * Initialize a new {@link KEGG2BioPAX} object, using a new Cache and a new KeggAdaptor.
   */
  public KEGG2BioPAX_level2() {
    this(new KeggInfoManagement());
  }
  
  /**
   * @param manager
   */
  public KEGG2BioPAX_level2(KeggInfoManagement manager) {
    super(BioPAXLevel.L2, manager);
  }

  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#createPathwayInstance(de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  protected BioPAXElement createPathwayInstance(Pathway p) {
    pathway = model.addNew(pathway.class, p.getName());
    pathway.setAVAILABILITY(Collections.singleton(String.format("This file has been generated by %s version %s", System.getProperty("app.name"), System.getProperty("app.version"))));
    pathway.setNAME((p.getTitle())); // Paxtools escapes chars for HTML automatically
    
    // Parse Kegg Pathway information
    boolean isKEGGPathway = DatabaseIdentifiers.checkID(DatabaseIdentifiers.IdentifierDatabases.KEGG_Pathway, p.getNameForMIRIAM());
    if (isKEGGPathway) {
      xref xr = (xref)createXRef(IdentifierDatabases.KEGG_Pathway, p.getNameForMIRIAM(), 1);
      if (xr!=null) {
        pathway.addXREF(xr);
      }
    }

    // Retrieve further information via Kegg Adaptor
    pathway.setORGANISM((bioSource) createBioSource(p));
    
    // Get PW infos from KEGG Api for Description and GO ids.
    KeggInfos pwInfos = KeggInfos.get(p.getName(), manager); // NAME, DESCRIPTION, DBLINKS verwertbar
    if (pwInfos.queryWasSuccessfull()) {
      pathway.addCOMMENT((pwInfos.getDescription()));
      
      // GO IDs
      if (pwInfos.getGo_id() != null) {
        for (String goID : pwInfos.getGo_id().split("\\s")) {
          xref xr = (xref)createXRef(IdentifierDatabases.GeneOntology, goID, 2);
          if (xr!=null) {
            pathway.addXREF(xr);
          }
        }
      }
    }
    
    // Add data sources
    Collection<BioPAXElement> sources = createDataSources(p);
    for (BioPAXElement source: sources) {
      pathway.addDATA_SOURCE((dataSource) source);
    }
    
    return pathway;
  }

  
  /**
   * Provides some direct access to KEGG2JSBML functionalities.
   * @param args
   * @throws Exception 
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws XMLStreamException
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) throws Exception {
    // Speedup Kegg2SBML by loading alredy queried objects. Reduces network
    // load and heavily reduces computation time.
    AbstractKEGGtranslator<?> k2s;
    if (new File(Translator.cacheFileName).exists()
        && new File(Translator.cacheFileName).length() > 1) {
      KeggInfoManagement manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
      k2s = new KEGG2BioPAX_level2(manager);
    } else {
      k2s = new KEGG2BioPAX_level2();
    }
    // ---
    
    if (args != null && args.length > 0) {
      File f = new File(args[0]);
      if (f.isDirectory()) {
        // Directory mode. Convert all files in directory.
        BatchKEGGtranslator batch = new BatchKEGGtranslator();
        batch.setOrgOutdir(args[0]);
        if (args.length > 1)
          batch.setChangeOutdirTo(args[1]);
        batch.setTranslator(k2s);
        batch.setOutFormat(Format.BioPAX_level2);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
          args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
          + ".sbml.xml";
        if (args.length > 1) outfile = args[1];
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        try {
          k2s.translate(p, outfile);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      
      // Remember already queried objects (save cache)
      if (AbstractKEGGtranslator.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, AbstractKEGGtranslator.getKeggInfoManager());
      }
      
      return;
    }
    
    
    
    // Just a few test cases here.
    System.out.println("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      // Remember already queried objects
      if (AbstractKEGGtranslator.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, AbstractKEGGtranslator.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Conversion took "+Utils.getTimeString((System.currentTimeMillis() - start)));
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#addEntry(de.zbit.kegg.parser.pathway.Entry, de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  public BioPAXElement addEntry(Entry entry, Pathway p) {
    
    /*
     * Get the actial object to create
     */
    Class<? extends BioPAXElement> instantiate = physicalEntity.class;
    if (entry.isSetType()) {
      if (entry.getType() == EntryType.compound) {
        instantiate = smallMolecule.class;
      } else if (entry.getType() == EntryType.enzyme) {
        instantiate = protein.class;
      } else if (entry.getType() == EntryType.gene) {
        instantiate = protein.class;
      } else if (entry.getType() == EntryType.genes) {
        instantiate = complex.class;
      } else if (entry.getType() == EntryType.group) {
        instantiate = complex.class;
      } else if (entry.getType() == EntryType.map) {
        instantiate = pathway.class;
      } else if (entry.getType() == EntryType.ortholog) {
        instantiate = protein.class;
      } else if (entry.getType() == EntryType.reaction) {
        //instantiate = interaction.class;
        // Reaction-nodes usually also occur as real reactions.
        return null;
      }
    }
    // Extended object is source was a non-KGMl document
    if (entry instanceof EntryExtended) {
      if (((EntryExtended) entry).isSetGeneType()) {
        if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.dna) {
          instantiate = dna.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.dna_region) {
          instantiate = dna.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.gene) {
          instantiate = dna.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.protein) {
          instantiate = protein.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.rna) {
          instantiate = rna.class;
        } else if (((EntryExtended) entry).getGeneType() == EntryTypeExtended.rna_region) {
          instantiate = rna.class;
        }
      }
    }
    
    // Pathway references are also stored separately.
    boolean isPathwayReference = false;
    String name = entry.getName().trim();
    if ((name != null) && (name.toLowerCase().startsWith("path:") || entry.getType().equals(EntryType.map))) {
      isPathwayReference = true;
      instantiate = pathway.class;
    }
    // Eventually skip this node. It's just a label for the current pathway.
    if (isPathwayReference && (entry.hasGraphics() && entry.getGraphics().getName().toLowerCase().startsWith("title:"))) {
      return null;//Do not add a pathway for the current pathway!
    }
    
    // Create the actual element
    BioPAXElement element = model.addNew(instantiate, '#'+NameToSId(entry.getName().length()>45?entry.getName().substring(0, 45):entry.getName()));
    pathwayComponentCreated(element);
    
    // NOTE: we can cast to entity, as all used classes are derived from entity
    // Get a good name for the node
    String fullName = null;
    if (entry.hasGraphics() && entry.getGraphics().getName().length() > 0) {
      fullName = entry.getGraphics().getName(); // + " (" + name + ")"; // Append ko Id(s) possible!
      name = fullName;
    }
    // Set name to real and human-readable name (from Inet data - Kegg API).
    name = getNameForEntry(entry);
    ((entity)element).setNAME(fullName!=null?fullName:name); // Graphics name (OR (if null) same as below)
    ((entity)element).setSHORT_NAME(name); // Intenligent name
    // TODO in level 3: setStandardName( and setDisplayName( in L3
    // ---
    ((entity)element).setDATA_SOURCE(pathway.getDATA_SOURCE());
        
    // For complex:
    if (entry.hasComponents() && (element instanceof complex)) {
      // TODO: Create complexAssembly, add it to pathway?!?!? AND add components to left and complex to right.
      for (int c:entry.getComponents()) {
        Entry ce = p.getEntryForId(c);
        if (ce!=null && ce!=entry) {
          // Get current component (or create if not yet there)
          BioPAXElement ceb = (BioPAXElement) ce.getCustom();
          if (ceb==null) {
            ceb = addEntry(ce, p);
          }
          if (ceb==null) continue;
          
          physicalEntityParticipant participant = getParticipant(ceb);
          ((complex)element).addCOMPONENTS(participant);
          participant.setCOMPONENTSof(((complex)element));
        }
      }
    }
    
    // XXX: Possible to set ORGANISM on COMPLEX & sequenceEntity (& Gene in L3)
    // TODO: CellularLocation from EntryExtended in L3
    
    // Add various annotations and xrefs
    addAnnotations(entry, element);
    
    entry.setCustom(element);
    return element;
  }

  /**
   * Searches for the {@link physicalEntityParticipant} represented by the given
   * {@link BioPAXElement} <code>ceb</code>.
   * @param ceb
   * @return instance of {@link physicalEntityParticipant} 
   */
  private physicalEntityParticipant getParticipant(BioPAXElement ceb) {
    // It's stupid, but we are required to create a new participant each time!
    // else, we get an error: "Illegal attempt to reuse a PEP!".
//    physicalEntityParticipant participant = (physicalEntityParticipant) model.getByID(ceb.getRDFId() + "_participant");
//    if (participant==null) {
      Class<? extends physicalEntityParticipant> instantiate2 = (ceb instanceof sequenceEntity) ? sequenceParticipant.class : physicalEntityParticipant.class;
      physicalEntityParticipant participant = model.addNew(instantiate2, NameToSId(ceb.getRDFId() + "_participant"));
      pathwayComponentCreated(participant);
      if (ceb instanceof physicalEntity) {
        participant.setPHYSICAL_ENTITY((physicalEntity) ceb);
      }
//    }
    return participant;
  }

  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#addKGMLReaction(de.zbit.kegg.parser.pathway.Reaction, de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  public BioPAXElement addKGMLReaction(Reaction r, Pathway p) {
    
    // Check if we have a reaction, that is catalyzed by enzymes
    Collection<Entry> enzymes = p.getReactionModifiers(r.getName());
    boolean hasEnzymes = enzymes!=null&&enzymes.size()>0;
    
    Class<? extends BioPAXElement> instantiate = biochemicalReaction.class;
    if (hasEnzymes) {
      instantiate = catalysis.class;
    }
    
    // Create the actual reaction or catalysis
    BioPAXElement element = model.addNew(instantiate, '#'+NameToSId(r.getName()));
    pathwayComponentCreated(element);
    biochemicalReaction reaction;
    if ((element instanceof catalysis)) {
      // setup enzymes
      Set<BioPAXElement> addedEnzymes = new HashSet<BioPAXElement>();
      if (hasEnzymes) {
        for (Entry ce:enzymes) {
          if (ce!=null) {
            // Get current component
            BioPAXElement ceb = (BioPAXElement) ce.getCustom();
            if (ceb==null || !addedEnzymes.add(ceb)) continue;
            
            ((catalysis) element).addCONTROLLER(getParticipant(ceb));
          }
        }
      }
      
      // reversible/irreversible
      if (r.isSetType()) {
        ((catalysis) element).setDIRECTION(r.getType()==ReactionType.reversible?Direction.REVERSIBLE:Direction.IRREVERSIBLE_LEFT_TO_RIGHT);
      }
      ((catalysis) element).setDATA_SOURCE(pathway.getDATA_SOURCE());
      ((catalysis) element).setNAME(r.getName()+"_catalysis");
      
      // create actual reaction
      reaction = model.addNew(biochemicalReaction.class, '#'+NameToSId(r.getName()));
      pathwayComponentCreated(reaction);
      ((catalysis) element).addCONTROLLED(reaction);
    } else {
      reaction = (biochemicalReaction) element;
    }
    
    
    reaction.setNAME(r.getName());
    reaction.setDATA_SOURCE(pathway.getDATA_SOURCE());
    
    // Add all reaction components
    for (ReactionComponent rc : r.getSubstrates()) {
      physicalEntityParticipant participant = configureReactionComponent(p, rc);
      if (participant!=null) {
        reaction.addLEFT(participant);
      }
    }
    for (ReactionComponent rc : r.getProducts()) {
      physicalEntityParticipant participant = configureReactionComponent(p, rc);
      if (participant!=null) {
        reaction.addRIGHT(participant);
      }
    }
    
    // Add various annotations
    addAnnotations(r, reaction);
    
    return reaction;
  }

  /**
   * Configures the {@link physicalEntityParticipant}: Sets the name,
   * id, metaId, species and SBO term.
   * @param p
   * @param rc
   * @return
   */
  private physicalEntityParticipant configureReactionComponent(Pathway p, ReactionComponent rc) {
    if (!rc.isSetID() && !rc.isSetName()) {
      rc = rc.getAlt();
      if (rc==null || ((!rc.isSetID() && !rc.isSetName()))) return null;
    }
    
    // Get BioPAX element for component
    Entry ce = p.getEntryForReactionComponent(rc);
    if (ce==null || ce.getCustom()==null) return null;
    BioPAXElement ceb = (BioPAXElement) ce.getCustom();
    
    // Set the stoichiometry
    physicalEntityParticipant participant = getParticipant(ceb);
    Integer stoich = rc.getStoichiometry();
    participant.setSTOICHIOMETRIC_COEFFICIENT(stoich==null?1d:stoich);
    
    participant.addCOMMENT(rc.getName());
    return participant;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2BioPAX#addKGMLRelation(de.zbit.kegg.parser.pathway.Relation, de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  public BioPAXElement addKGMLRelation(Relation r, Pathway p) {
    /*
     * Relations:
     * - Conversion is generic with left and right,
     * - PhysicalphysicalInteraction is generic with just a pool of entities.
     */
    Collection<String> subtype = r.getSubtypesNames();
    
    // Get Participants
    Entry eOne = p.getEntryForId(r.getEntry1());
    Entry eTwo = p.getEntryForId(r.getEntry2());
    BioPAXElement qOne = eOne==null?null:(BioPAXElement) eOne.getCustom();
    BioPAXElement qTwo = eTwo==null?null:(BioPAXElement) eTwo.getCustom();
    if (qOne==null || qTwo==null) {
      // Happens, e.g. when remove_pw_references is true and there is a
      // relation to this (now removed) node.
      log.finer("Relation with unknown or removed entry: " + r);
      return null;
    }
    
    // Most relations have a left and right side => conversion as default
    Class<? extends BioPAXElement> instantiate = conversion.class;
    
    
    // Compound (only PPREL) to conversion, SKIP ALL OTHERS [IF CONSIDERREACTIONS()]
    if (considerReactions()) {
      if (subtype.contains(SubType.COMPOUND) || subtype.contains(SubType.HIDDEN_COMPOUND)) {
        if (r.isSetType() && (r.getType()==RelationType.PPrel)) {
          instantiate = conversion.class;
        } else {
          // Other compound relations are copies of reactions, so no need to translate them.
          // KGML spec says:  "shared with two successive reactions"
          return null;
        }
      }
    }
    
    
    // "binding/assoc.", "dissociation", "missing interaction" and in doubt to PhysicalInteraction
    if ((subtype.contains(SubType.ASSOCIATION) || subtype.contains(SubType.BINDING) || subtype.contains(SubType.BINDING_ASSOCIATION)) ||
        (subtype.contains(SubType.DISSOCIATION)) || subtype.contains(SubType.MISSING_INTERACTION) || subtype.size()<1) {
      // This property may get overwritten later on!
      instantiate = physicalInteraction.class; // Same as Interaction.class in L3
    }
    
    // Check if "binding/assoc." describes the formation of a complex.
    if ((eTwo.getType().equals(EntryType.group) || eTwo.getType().equals(EntryType.genes)) && 
        (subtype.contains(SubType.ASSOCIATION) || subtype.contains(SubType.BINDING) || subtype.contains(SubType.BINDING_ASSOCIATION))) {
      instantiate = complexAssembly.class;
    }
    
    // Check if "DISSOCIATION" describes the DISASSEMBLY of a complex.
    if ((eOne.getType().equals(EntryType.group) || eOne.getType().equals(EntryType.genes)) && 
        (subtype.contains(SubType.DISSOCIATION))) {
      instantiate = complexAssembly.class; // this is also used for DISASSEMBLY.
    }
    
    // Make a final check, if we are able to create a conversion
    if ((!(qOne instanceof physicalEntity) || !(qTwo instanceof physicalEntity)) && 
        (instantiate == conversion.class)) {
      log.info("Changing from conversion to physicalInteraction, because conversion requires physical entities as participants " + r);
      instantiate = physicalInteraction.class;
    }
    
    // Create the relation
    physicalInteraction bpe = (physicalInteraction) model.addNew(instantiate, '#'+NameToSId("KEGGrelation"));
    pathwayComponentCreated(bpe);
    
    // Add Annotations
    bpe.setDATA_SOURCE(pathway.getDATA_SOURCE());
    if (subtype.size()>0) {
      if (!subtype.contains(SubType.COMPOUND)) {
        bpe.addCOMMENT("LINE-TYPE: " + r.getSubtypes().iterator().next().getValue());
      }
      bpe.setNAME(ArrayUtils.implode(subtype, ", "));
      
      for (SubType st: r.getSubtypes()) {
        bpe.addINTERACTION_TYPE((openControlledVocabulary) getInteractionVocuabulary(st));
      }
    }
    
    // Add participants
    if (bpe instanceof conversion) {
      ((conversion) bpe).addLEFT(getParticipant(qOne));
      ((conversion) bpe).addRIGHT(getParticipant(qTwo));
    } else {
      bpe.addPARTICIPANTS((InteractionParticipant) qOne);
      bpe.addPARTICIPANTS((InteractionParticipant) qTwo);
    }
    
    return bpe;
  }
  
  
}
