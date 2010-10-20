/**
 * 
 */
package de.zbit.kegg.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.gui.GUITools;
import de.zbit.gui.VerticalLayout;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.ProgressBarSwing;

/**
 * @author Andreas Dr&auml;ger
 * 
 */
public class TranslatorUI extends JDialog implements ActionListener {

	/**
	 * This is a enumeration of all possible commands this
	 * {@link ActionListener} can process.
	 * 
	 * @author draeger
	 * 
	 */
	public static enum Command {
		/**
		 * Command to open a file.
		 */
		OPEN_FILE,
		/**
		 * Command to save the conversion result to a file.
		 */
		SAVE_FILE,
		/**
		 * Closes the program
		 */
		EXIT;

		/**
		 * Returns a human-readable name for each command.
		 * 
		 * @return
		 */
		public String getName() {
			switch (this) {
			case OPEN_FILE:
				return "Open";
			case SAVE_FILE:
				return "Save";
			case EXIT:
				return "Exit";
			default:
				return "Unknown";
			}
		}
	}
	
	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (new File(KEGGtranslator.cacheFileName).exists()
				&& new File(KEGGtranslator.cacheFileName).length() > 0) {
			KeggInfoManagement manager;
			try {
				manager = (KeggInfoManagement) KeggInfoManagement
						.loadFromFilesystem(KEGGtranslator.cacheFileName);
			} catch (IOException e) {
				e.printStackTrace();
				manager = new KeggInfoManagement();
			}
			k2s = new KEGG2jSBML(manager);
		} else {
			k2s = new KEGG2jSBML();
		}
	}

	/**
	 * Generated serial version id
	 */
	private static final long serialVersionUID = -3833481758555783529L;

	/**
	 * Speedup Kegg2SBML by loading already queried objects. Reduces network
	 * load and heavily reduces computation time.
	 */
	private static KEGG2jSBML k2s;

	/**
	 * @param args
	 *            accepted arguments are --input=<base directory to open files>
	 *            and --output=<base directory to save files>
	 * @throws SBMLException
	 */
	public static void main(String[] args) throws SBMLException {
		if (args.length > 0) {
			String infile = null, outfile = null;
			for (String arg : args) {
				if (arg.startsWith("--input=")) {
					infile = arg.split("=")[1];
				} else if (arg.startsWith("--output=")) {
					outfile = arg.split("=")[1];
				}
			}
			if (infile != null) {
				if (outfile != null) {
					new TranslatorUI(infile, outfile);
				} else {
					new TranslatorUI(infile, System.getProperty("user.dir"));
				}
			}
		} else {
			new TranslatorUI();
		}
	}

	/**
	 * Basis directory when opening files.
	 */
	private String baseOpenDir;
	/**
	 * Basis directory when saving files.
	 */
	private String baseSaveDir;
	/**
	 * The SBML document as a result of a successful conversion.
	 */
	private SBMLDocument doc;

	/**
	 * Shows a small GUI.
	 * 
	 * @throws SBMLException
	 */
	public TranslatorUI() throws SBMLException {
		this(System.getProperty("user.dir"), System.getProperty("user.dir"));
	}

	/**
	 * 
	 * @param baseDir
	 * @param saveDir
	 * @throws SBMLException
	 */
	public TranslatorUI(String baseDir, String saveDir) throws SBMLException {
		super();
		this.baseOpenDir = baseDir;
		this.baseSaveDir = saveDir;
		this.doc = translate(openFile());
		showGUI(doc);
	}

	public void actionPerformed(ActionEvent e) {
		switch (Command.valueOf(e.getActionCommand())) {
		case OPEN_FILE:
			// Translate KEGG file to SBML document.
			if (isVisible()) {
				setVisible(false);
				removeAll();
			}
			try {
				showGUI(translate(openFile()));
			} catch (Throwable exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
			break;
		case SAVE_FILE:
			saveFile();
			break;
		case EXIT:
			dispose();
			System.exit(0);
		default:
			System.err.println("unsuported action: " + e.getActionCommand());
			break;
		}
	}

	/*
      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowListener() {
        public void windowActivated(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowOpened(WindowEvent e) {}
        public void windowClosed(WindowEvent e) {}
        public void windowClosing(WindowEvent e) {
          exit();
        }
      });
	 */
	/**
	 * Creates a JMenuBar for this component that provides access to all Actions
	 * definied in the enum Command.
	 * 
	 * @return
	 */
	private JMenuBar createJMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu("File");
		for (Command com : Command.values()) {
			JMenuItem item = new JMenuItem(com.getName());
			item.setActionCommand(com.toString());
			item.addActionListener(this);
			menu.add(item);
		}
		bar.add(menu);
		return bar;
	}

	/**
	 * Open and display a KGML file.
	 */
	private File openFile() {
		File file = GUITools.openFileDialog(this, baseOpenDir, false, false,
				JFileChooser.FILES_ONLY, new FileFilterKGML());
		if (file != null) {
			baseOpenDir = file.getParent();
			return file;
		} else {
			dispose();
			System.exit(0);
		}
		return new File(baseOpenDir);
	}

	/**
	 * Save a conversion result to a file
	 */
	private void saveFile() {
		if (isVisible() && (doc != null)) {
			JFileChooser chooser = GUITools.createJFileChooser(baseSaveDir,
					false, false, JFileChooser.FILES_ONLY,
					SBFileFilter.SBML_FILE_FILTER);
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				if (!f.exists() || GUITools.overwriteExistingFile(this, f)) {
					try {
						SBMLWriter.write(doc, chooser.getSelectedFile()
								.getAbsolutePath());
					} catch (Exception exc) {
						exc.printStackTrace();
						JOptionPane.showMessageDialog(this, exc.getMessage(),
								exc.getClass().getSimpleName(),
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}

	/**
	 * Displays an overview of the result of a conversion.
	 * 
	 * @throws SBMLException
	 */
	private void showGUI(SBMLDocument doc) throws SBMLException {
		getContentPane().removeAll();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setJMenuBar(createJMenuBar());
		getContentPane().add(new SBMLModelSplitPane(doc.getModel()));
		setTitle("SBML from KEGG " + doc.getModel().getId());
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * This method translates a given KGML file into an SBMLDocument by calling
	 * the dedicated method in {@link KEGG2jSBML}.
	 * 
	 * @param f
	 * @return
	 */
	private SBMLDocument translate(File f) {
	  JDialog load = null;
		try {
		  load = createLoadingJPanel("Translating kegg pathway " +f.getName() + "...");
			this.doc = k2s.translate(f);
			return doc;
		} catch (Throwable exc) {
			GUITools.showErrorMessage(this, exc, String.format(
					"Could not read input file %s.", f.getAbsolutePath()));
		} finally {
		  if (load!=null) load.dispose();
		}
		return null;
	}
	
	/**
	 * Create and display a temporary loading panel with the given
	 * message and a progress bar.
	 * @param loadingMessage - the Message to display.
	 * @return JDialog
	 */
	private JDialog createLoadingJPanel(String loadingMessage) {
	  // Create the panel
	  Dimension panelSize = new java.awt.Dimension(400,75);
	  JPanel p = new JPanel(new VerticalLayout());
	  p.setPreferredSize(panelSize);
	  
	  // Create the label and progressBar
	  JLabel jl = new JLabel(loadingMessage);
	  Font font = new java.awt.Font("Tahoma",Font.PLAIN,12);
	  jl.setFont(font);
	  
	  JProgressBar prog = new JProgressBar();
	  prog.setPreferredSize(new Dimension(panelSize.width-20, panelSize.height/4));
	  p.add(jl, BorderLayout.NORTH);
	  p.add(prog, BorderLayout.CENTER);
	  
	  // Link the progressBar to the keggConverter
	  k2s.setProgressBar(new ProgressBarSwing(prog));
	  
	  // Display the panel in an jFrame
	  JDialog f = new JDialog();
	  f.setTitle("KEGG Translator");
    f.setSize(p.getPreferredSize());
	  f.setContentPane(p);
	  f.setPreferredSize(p.getPreferredSize());
	  f.setLocationRelativeTo(null);
	  f.setVisible(true);
	  f.setDefaultCloseOperation( DO_NOTHING_ON_CLOSE ); 
	  
	  return f;
	}

}
