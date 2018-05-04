/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 * 
 * Copyright (c) <2018> <Markus Gärtner, Volodymyr Kushnarenko, Florian Fritze, Sibylle Hermann and Uli Hahn>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bwfdm.replaydh.ui.metadata;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * @author Florian Fritze
 *
 */

public abstract class MetadataUI<T extends MetadataSchema> implements ActionListener, DocumentListener {

	class MetadataPropertyDialog extends JDialog implements ActionListener, DocumentListener, KeyListener {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private FormBuilder builder = FormBuilder.create();

		private JLabel label = new JLabel();
		private JLabel message = new JLabel();
		private JTextField textfield = new JTextField();
		private JButton okbutton = new JButton(ResourceManager.getInstance().get("replaydh.labels.ok"));
		private JButton cancelbutton = new JButton(ResourceManager.getInstance().get("replaydh.labels.cancel"));

		private Dimension preferredSize = new Dimension();

		private String columns="pref,3dlu,pref,3dlu,pref,3dlu,pref";
		private String rows="pref,3dlu,pref,5dlu,pref";

		private FormLayout layout = new FormLayout(columns,rows);

		private List<String> listofkeys = null;

		private T metadataFE = null;

		public MetadataPropertyDialog(List<String> listofkeys, T metadataFrontEnd) {
			this.metadataFE=metadataFrontEnd;
			preferredSize=cancelbutton.getPreferredSize();
			okbutton.setPreferredSize(preferredSize);
			this.listofkeys=listofkeys;
			this.panel.setLayout(layout);
			builder.columns(columns);
			builder.rows(rows);
			builder.panel(this.panel);
			this.setTitle(ResourceManager.getInstance().get("replaydh.ui.editor.metadata.addCustomProperty"));
			this.setModal(true);
			label.setText(ResourceManager.getInstance().get("replaydh.ui.editor.metadata.metadataProperty"));
			textfield.setPreferredSize(new Dimension(300,25));
			this.setResizable(false);
			builder.add(label).xy(1, 1);
			builder.appendColumns("$glue");
			builder.appendColumns("$glue");
			builder.add(textfield).xyw(3, 1, 7);
			builder.appendRows("$lg, pref");
			builder.add(message).xyw(3, 3, 7);
			message.setForeground(Color.RED);
			message.setVisible(false);
			builder.appendRows("$lg, pref");
			this.okbutton.setEnabled(false);
			builder.add(okbutton).xy(3, 5);
			builder.add(cancelbutton).xy(7, 5);
			builder.padding(new EmptyBorder(10, 10, 10, 10));
			this.okbutton.addActionListener(this);
			this.cancelbutton.addActionListener(this);
			this.textfield.getDocument().addDocumentListener(this);
			this.okbutton.addKeyListener(this);
			builder.build();
			add(panel);
			pack();
		}

		public JButton getOkbutton() {
			return okbutton;
		}

		public void setOkbutton(JButton okbutton) {
			this.okbutton = okbutton;
		}

		public JButton getCancelbutton() {
			return cancelbutton;
		}

		public void setCancelbutton(JButton cancelbutton) {
			this.cancelbutton = cancelbutton;
		}

		private JPanel panel = new JPanel();

		public JLabel getLabel() {
			return label;
		}



		public void setLabel(JLabel label) {
			this.label = label;
		}

		public JTextField getTextfield() {
			return textfield;
		}

		public void setTextfield(JTextField textfield) {
			this.textfield = textfield;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			Object source = e.getSource();


			if(cancelbutton == source) {
				this.dispose();
				textfield.setText("");
			} else if (okbutton == source) {
				if(!(textfield.getText().equals(""))) {
					this.setVisible(false);
				}
			}

		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			String metadataproperty=textfield.getText();
			boolean found=false;
			for(String item : listofkeys) {
				if(item.equals(metadataproperty)) {
					found=true;
					message.setVisible(true);
					message.setText("Key already exists");
					okbutton.setEnabled(false);
					okbutton.setFocusable(false);
				}
			}
			if(found == false) {
				okbutton.setEnabled(true);
				okbutton.setFocusable(true);
				if(metadataFE.isAllowedName(textfield.getText())) {
					okbutton.setEnabled(true);
					okbutton.setFocusable(true);
					message.setVisible(false);
				} else {
					message.setVisible(true);
					message.setText("Invalid property");
					okbutton.setEnabled(false);
					okbutton.setFocusable(false);
				}
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			String metadataproperty=textfield.getText();
			boolean found=false;
			for(String item : listofkeys) {
				if(item.equals(metadataproperty)) {
					found=true;
					okbutton.setEnabled(false);
					okbutton.setFocusable(false);
				}
			}
			if(found == false) {
				okbutton.setEnabled(true);
				okbutton.setFocusable(true);
			}
			if(textfield.getText().equals("")) {
				okbutton.setEnabled(false);
				okbutton.setFocusable(false);
				message.setVisible(true);
				message.setText("No property entered");
			} else {
				message.setVisible(false);
				if(metadataFE.isAllowedName(textfield.getText())) {
					okbutton.setEnabled(true);
					okbutton.setFocusable(true);
				} else {
					message.setVisible(true);
					message.setText("Invalid property");
					okbutton.setEnabled(false);
					okbutton.setFocusable(false);
				}
			}
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			if((okbutton.isFocusOwner()) && (e.getKeyCode() == KeyEvent.VK_ENTER) && (!(textfield.getText().equals("")))) {
				this.setVisible(false);
			}

		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub

		}

	}

	protected T metadataFrontEnd = null;

	/**
	 * For every property name there are values in certain order of appearance. These values are stored in this Map.
	 */

	protected Map<String, List<String>> propertyvalues = null;

	/**
	 * Creating an array that holds the required values
	 */
	protected List<String> requiredkeys = null;

	/**
	 * Holds the main builder for the main JPanel
	 */
	protected FormBuilder builder = FormBuilder.create();

	/**
	 * Contains the keys(names) of the metadata properties. Is used for referencing a certain metadata property.
	 */

	protected List<String> listofkeys = null;

	/**
	 * Contains all the keys of a certain category (required or optional)
	 */

	protected Map<String, List<String>> keys = null;

	/**
	 * Contains all the properties for a certain key in listofkeys
	 */

	protected Map<String, JPanel> propertypanels = null;

	/**
	 * For every metadata property there is a wrapper class. All wrapper classes for a property are stored in an ArrayList
	 */

	protected Map<String, List<GUIElement>> elementsofproperty = null;

	/**
	 * Sets the custom width for a JTextField, during the program, it can be altered
	 */
	protected String customwidth="pref:grow";

	/**
	 * Sets the custom label width that was inferred from all the labels of a metadata schema
	 */
	protected String customlabelwidth=null;


	/**
	 * Contains all the custom added metadata properties
	 */

	protected List<String> addlistofkeys = null;

	/**
	 * Defines the preferred size of a Button
	 */

	protected Dimension shadowsize = new Dimension(17,17);

	protected Dimension preferredSize = new Dimension(17,17);

	/**
	 * Sets the needed height and width of a GUI element inferred at startup
	 */
	protected Double neededheight = null;

	protected Double neededwidth = null;

	/**
	 * Hold the current needed maximum label size
	 */
	protected Dimension currentlabelsize = null;

	protected Dimension defaultlabelsize = null;

	/**
	 * Holds an instance of the Icon Registry
	 */
	protected IconRegistry ir = null;

	/**
	 * Defining and creating instances of needed icons
	 */
	protected Icon iidel = null;
	protected Icon iiadd = null;
	protected Icon iidelcomplete = null;
	protected Icon justremove = null;

	/**
	 * Holds an instance of the Border class to paint the Border red if needed
	 */
	protected Border border = null;


	/**
	 * Holds an instance of the metadataRecord
	 */
	protected MetadataRecord record = null;

	/**
	 * Is set to "true" if a metadata property must hold a value
	 */
	protected boolean propertyrequired = false;

	public MetadataUI() {

		listofkeys= new ArrayList<>();

		addlistofkeys = new ArrayList<>();

		elementsofproperty = new HashMap<>();

		propertypanels = new HashMap<>();

		propertyvalues = new HashMap<>();

		keys = new HashMap<>();


		requiredkeys = new ArrayList<String>();

		ir = IconRegistry.getGlobalRegistry();

		iidel = ir.getIcon("list-remove-5.png");

		iiadd = ir.getIcon("list-add.png");

		iidelcomplete = ir.getIcon("edit-clear-2.png");

		justremove = ir.getIcon("application-exit-4.png");

	}


	/**
	 * Draws a certain (required or optional) part of the main gui
	 * @param key
	 * @param rowindex
	 * @return
	 */
	public int sectionDrawing(String key, int rowindex) {

		Dimension preferredSize = new Dimension(17,17);
		String columns=customlabelwidth+",3dlu,"+customwidth+",3dlu,right:pref,3dlu,right:pref";
		String rows="pref";
		String separatorrows="7dlu,pref,7dlu";
		String separatorcolumns="pref:grow";
		ImageIcon ii = new ImageIcon("src/main/resources/bwfdm/replaydh/ui/icons/list-add.png");
		FormLayout layout = new FormLayout(columns,rows);
		FormLayout separatorlayout = new FormLayout(separatorcolumns,separatorrows);
		int z=rowindex;


		for (String metadatapropertyname : keys.get(key)) {
			z++;
			boolean required=false;
			for(String propertyname : requiredkeys) {
				if (metadatapropertyname.equals(propertyname)) {
					required=true;
					break;
				}
			}
			if(metadatapropertyname.equals("requiredseparator")) {
				JPanel onepropertypanel = new JPanel();
				onepropertypanel.setLayout(separatorlayout);
				FormBuilder propertiesbuilder = FormBuilder.create();
				propertiesbuilder.columns(separatorcolumns);
				propertiesbuilder.rows(separatorrows);
				propertiesbuilder.panel(onepropertypanel);
				propertiesbuilder.addSeparator(ResourceManager.getInstance().get("replaydh.ui.editor.metadata.sepRequired")).xyw(1, 2, 1);
				builder.add(onepropertypanel).xy(1, z);
				builder.appendRows("pref");
				continue;
			} else if(metadatapropertyname.equals("optionalseparator")) {
				JPanel onepropertypanel = new JPanel();
				onepropertypanel.setLayout(separatorlayout);
				FormBuilder propertiesbuilder = FormBuilder.create();
				propertiesbuilder.columns(separatorcolumns);
				propertiesbuilder.rows(separatorrows);
				propertiesbuilder.panel(onepropertypanel);
				propertiesbuilder.addSeparator(ResourceManager.getInstance().get("replaydh.ui.editor.metadata.sepOptional")).xyw(1, 2, 1);
				builder.add(onepropertypanel).xy(1, z);
				builder.appendRows("pref");
				continue;
			}
			JPanel onepropertypanel = new JPanel();
			onepropertypanel.setLayout(layout);
			FormBuilder propertiesbuilder = FormBuilder.create();
			propertiesbuilder.columns(columns);
			propertiesbuilder.rows(rows);
			propertiesbuilder.panel(onepropertypanel);

			propertypanels.put(metadatapropertyname, onepropertypanel);
			/*
			 * Checks if the metadatapropertyname can hold more than one entry.
			 */
			if ((metadataFrontEnd.getMultiplicity(metadatapropertyname).getAllowedMaximum() == -1) || (metadataFrontEnd.getMultiplicity(metadatapropertyname).getAllowedMaximum() > 1)) {


				JLabel label=elementsofproperty.get(metadatapropertyname).get(0).getLabel();


				propertiesbuilder.add(label).xy(1, 1);

				JTextField textfield=elementsofproperty.get(metadatapropertyname).get(0).getTextfield();

				textfield.getDocument().addDocumentListener(this);

				propertiesbuilder.add(textfield).xy(3, 1);

				JButton button=elementsofproperty.get(metadatapropertyname).get(0).getButton();

				button.setPreferredSize(preferredSize);
				button.setName("plus");

				button.setIcon(ii);
				button.addActionListener(this);

				JLabel shadowlabel = new JLabel();

				shadowlabel.setPreferredSize(shadowsize);

				JButton minusbutton=elementsofproperty.get(metadatapropertyname).get(0).getMinusbutton();

				minusbutton.setPreferredSize(preferredSize);
				minusbutton.setName("minus");

				minusbutton.setIcon(justremove);
				minusbutton.addActionListener(this);

				propertiesbuilder.add(minusbutton).xy(5, 1);

				minusbutton.setVisible(false);

				propertiesbuilder.add(shadowlabel).xy(5, 1);

				propertiesbuilder.add(button).xy(7, 1);

				if(required == true) {
					textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
					textfield.setToolTipText("Property required");
				}
			} else {


				propertiesbuilder.panel(onepropertypanel);


				JLabel label=elementsofproperty.get(metadatapropertyname).get(0).getLabel();


				propertiesbuilder.add(label).xy(1, 1);

				JTextField textfield=elementsofproperty.get(metadatapropertyname).get(0).getTextfield();

				textfield.getDocument().addDocumentListener(this);

				propertiesbuilder.add(textfield).xy(3, 1);

				JLabel shadowlabelfirst = new JLabel();

				shadowlabelfirst.setPreferredSize(shadowsize);

				JButton minusbutton=elementsofproperty.get(metadatapropertyname).get(0).getMinusbutton();

				minusbutton.setPreferredSize(preferredSize);
				minusbutton.setName("minus");

				minusbutton.setIcon(justremove);
				minusbutton.addActionListener(this);

				propertiesbuilder.add(minusbutton).xy(5, 1);

				minusbutton.setVisible(false);

				propertiesbuilder.add(shadowlabelfirst).xy(5, 1);

				JLabel shadowlabelsecond = new JLabel();

				shadowlabelsecond.setPreferredSize(shadowsize);

				propertiesbuilder.add(shadowlabelsecond).xy(7, 1);

				if(required == true) {
					textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
					textfield.setToolTipText("Property required");
				}
			}


			propertiesbuilder.appendRows("$lg, pref");



			propertiesbuilder.build();

			builder.add(propertypanels.get(metadatapropertyname)).xy(1, z);
			builder.appendRows("pref");

		}
		return z;
	}


	/**
	 * Paints the standard GUI content
	 */
	public void paintGUI() {

		int rowindex=sectionDrawing("required",0);
		sectionDrawing("optional",rowindex);
		addMetadataPropertyOption();


	}


	/**
	 * Checks if the entry in the MetadataRecord differs from the actual entry in the corresponding textfield.
	 * @param metadatapropertyname
	 * @return
	 */
	public boolean hasChanged(String metadatapropertyname) {
		if ((metadataFrontEnd == null) || (propertyrequired  == true) || (record == null)) {
			return false;
		} else {
			boolean changed = false;
			if (record.hasMultipleEntries(metadatapropertyname)) {
				for(Entry entry: record.getEntries()) {
					for(GUIElement guielement : elementsofproperty.get(metadatapropertyname)) {
						changed = changed || !Objects.equals(guielement.getTextfield().getText(),entry.getValue());
					}


				}

				return changed;
			} else if (record.getEntryCount(metadatapropertyname) == 1) {
				changed = !Objects.equals(record.getEntry(metadatapropertyname).getValue(),elementsofproperty.get(metadatapropertyname).get(0).getTextfield().getText());
				return changed;
			} else {
				return false;
			}
		}


	}

	/**
	 * Inserts values into the textfields of the metadata properties and can also add more GUIElements if needed. This method is used for Resetting the stored values or loading the values from the metadataEditor
	 */
	public void refreshUI() {
		int indexnew=0;
		int componentscounter=0;


		for(String metadatapropertyname : listofkeys) {
			if ((metadatapropertyname.equals("additionalproperty")) || (metadatapropertyname.equals("requiredseparator")) || (metadatapropertyname.equals("optionalseparator"))) {
				continue;
			}
			indexnew=0;
			boolean required=false;
			for(String propertyname : requiredkeys) {
				if (metadatapropertyname.equals(propertyname)) {
					required=true;
					break;
				}
			}
			JTextField textfield=elementsofproperty.get(metadatapropertyname).get(0).getTextfield();
			if (hasChanged(metadatapropertyname)) {
					if ((record.hasMultipleEntries(metadatapropertyname)) && ((metadataFrontEnd.getMultiplicity(metadatapropertyname).getAllowedMaximum() == -1) || (metadataFrontEnd.getMultiplicity(metadatapropertyname).getAllowedMaximum() > 1))) {

						componentscounter=elementsofproperty.get(metadatapropertyname).size();
						/*
						 * for-loop is adding as much components as needed for the entries in the MetdataRecord.
						 */
						for(int i=componentscounter; i < record.getEntryCount(metadatapropertyname); i++) {
							GUIElement guielement = new GUIElement(new JLabel(), new JTextField(30), new JButton(), new JButton());
							elementsofproperty.get(metadatapropertyname).add(guielement);


						}
						componentscounter=elementsofproperty.get(metadatapropertyname).size();
						/*
						 * Inserts stored values into the corresponding GUIElement Textfield.
						 */
						if(propertyvalues.isEmpty()) {
							for(Entry entry: record.getEntries(metadatapropertyname)) {
								elementsofproperty.get(metadatapropertyname).get(indexnew).getTextfield().setText(entry.getValue());

								indexnew++;
							}
						} else {
							/*
							 * Inserts stored values into the corresponding GUIElement Textfield if there was already an Apply() call for the properties
							 */
							String value;
							for(int index=0; index < componentscounter; index++) {
								value=propertyvalues.get(metadatapropertyname).get(index);
								elementsofproperty.get(metadatapropertyname).get(index).getTextfield().setText(value);
							}
						}


						/*
						 * Redraws the gui according to the changes above.
						 */
						refreshOnePanel(metadatapropertyname);

					} else if((record.getEntryCount(metadatapropertyname) == 1) ) {
						elementsofproperty.get(metadatapropertyname).get(0).getTextfield().setText(record.getEntry(metadatapropertyname).getValue());
						componentscounter=elementsofproperty.get(metadatapropertyname).size();
						/*
						 * Removes needless components
						 */
						if(!(propertyvalues.isEmpty())) {
							for (int i=componentscounter; i > record.getEntryCount(metadatapropertyname); i--) {
								elementsofproperty.get(metadatapropertyname).remove(i-1);
							}
							/*
							 * Redraws the gui according to the changes above.
							 */
						}

						refreshOnePanel(metadatapropertyname);

					} else {
						elementsofproperty.get(metadatapropertyname).get(0).getTextfield().setText(record.getEntry(metadatapropertyname).getValue());
					}

			}

			if((required == true) && (textfield.getText().equals(""))) {
				textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
				textfield.setToolTipText("Property required");
			}
		}

	}

	/**
	 * Add the Button for adding custom metadataproperties at the end of all current metadata properties
	 */
	public void addMetadataPropertyOption() {
		int index=0;
		boolean found=false;
		for (String metadataproperty: listofkeys) {
			if (metadataproperty.equals("additionalproperty")) {
				found=true;
				break;
			}
			index++;
		}
		if (found == true) {
			listofkeys.remove(index);
			propertypanels.remove("additionalproperty");

		}

		String columns=customlabelwidth+",3dlu,"+customwidth+",3dlu,right:pref,3dlu,right:pref";
		String rows="pref,9dlu";

		FormLayout layout = new FormLayout(columns,rows);

		FormBuilder propertybuilder = FormBuilder.create();
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);

		JPanel addpropertypanel = new JPanel();

		propertybuilder.panel(addpropertypanel);

		addpropertypanel.setLayout(layout);

		propertypanels.put("additionalproperty", addpropertypanel);


		GUIElement guielement = new GUIElement(new JLabel(), new JTextField(30), new JButton(), new JButton());

		List<GUIElement> elementslist = new ArrayList<>();
		elementslist.add(guielement);

		elementsofproperty.put("additionalproperty", elementslist);


		listofkeys.add("additionalproperty");

		for(GUIElement oneguielement : elementsofproperty.get("additionalproperty")) {


			JLabel label=oneguielement.getLabel();

			label.setPreferredSize(new Dimension(neededwidth.intValue(),neededheight.intValue()));

			propertybuilder.add(label).xy(1, 1);

			JButton addbutton = oneguielement.getButton();

			addbutton.setText(ResourceManager.getInstance().get("replaydh.ui.editor.metadata.addCustomProperty"));
			
			Dimension addbuttonsize = new Dimension();
			
			addbuttonsize.setSize(addbutton.getPreferredSize().getWidth(), oneguielement.getTextfield().getPreferredSize().getHeight());
			
			addbutton.setPreferredSize(addbuttonsize);

			propertybuilder.add(addbutton).xy(3, 1);


			addbutton.setName("addproperty");

			addbutton.addActionListener(this);

			JLabel shadowlabelfirst = new JLabel();

			shadowlabelfirst.setPreferredSize(shadowsize);

			propertybuilder.add(shadowlabelfirst).xy(5, 1);

			JLabel shadowlabelsecond = new JLabel();

			shadowlabelsecond.setPreferredSize(shadowsize);

			propertybuilder.add(shadowlabelsecond).xy(7, 1);

		}

		builder.add(addpropertypanel).xy(1, listofkeys.size());
	}

	/**
	 * Refreshes one JPanel according to the specified metadata property and its position (index) in the main
	 * panelbuilder (builder)
	 * @param metadatapropertyname
	 */
	public void refreshOnePanel(String metadatapropertyname) {
		if ((metadataFrontEnd.getMultiplicity(metadatapropertyname).getAllowedMaximum() == -1) || (metadataFrontEnd.getMultiplicity(metadatapropertyname).getAllowedMaximum() > 1)) {
		int panelrow=0;
		for(String propertyname : listofkeys) {
			panelrow++;
			if (metadatapropertyname.equals(propertyname)) {
				break;
			}

		}
		boolean required=false;
		for(String propertyname : requiredkeys) {
			if (metadatapropertyname.equals(propertyname)) {
				required=true;
				break;
			}

		}

		int size=elementsofproperty.get(metadatapropertyname).size();
		for(int index=0; index < size; index++) {
			JTextField textfield=elementsofproperty.get(metadatapropertyname).get(index).getTextfield();
			textfield.getDocument().removeDocumentListener(this);
		}

		int index=0;


		String columns=customlabelwidth+",3dlu,"+customwidth+",3dlu,right:pref,3dlu,right:pref";
		String rows="pref";


		FormLayout layout = new FormLayout(columns,rows);


		JPanel onepropertypanel = propertypanels.get(metadatapropertyname);

		for(int z=0; z < onepropertypanel.getComponentCount(); z++) {
			if(onepropertypanel.getComponent(z).getClass().getTypeName().equals("javax.swing.JButton")) {
				((JButton)onepropertypanel.getComponent(z)).removeActionListener(this);
			}
		}

		onepropertypanel.removeAll();

		onepropertypanel.setLayout(layout);



		JPanel newpropertypanel = new JPanel();


		FormBuilder propertybuilder = FormBuilder.create();
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);



		propertybuilder.panel(newpropertypanel);

		propertypanels.put(metadatapropertyname, newpropertypanel);
		onepropertypanel.removeAll();
		onepropertypanel.setLayout(layout);



		int z=0;



		for(GUIElement oneguielement : elementsofproperty.get(metadatapropertyname)) {

			if (z == 0) {
				JLabel label=oneguielement.getLabel();

				label.setText(metadatapropertyname);
				label.setPreferredSize(currentlabelsize);
				propertybuilder.add(label).xy(1, (z*2)+1);
			}

			JTextField textfield=oneguielement.getTextfield();

			textfield.getDocument().addDocumentListener(this);

			propertybuilder.add(textfield).xy(3, (z*2)+1);

			JButton minusbutton=oneguielement.getMinusbutton();

			JButton button=oneguielement.getButton();


			if (index == 0) {

				minusbutton.setPreferredSize(preferredSize);
				boolean additionalpropertyfound=false;
				for(String item: addlistofkeys) {
					if(metadatapropertyname.equals(item)) {
						additionalpropertyfound=true;
						break;
					}
				}
				if((additionalpropertyfound == true) && (elementsofproperty.get(metadatapropertyname).size() == 1) && (textfield.getText().equals(""))){
					minusbutton.setName("minuscomplete");

					minusbutton.setIcon(iidelcomplete);

					button.setName("plus");

					button.setIcon(iiadd);

					propertybuilder.add(minusbutton).xy(5, (z*2)+1);

					propertybuilder.add(button).xy(7, (z*2)+1);

					minusbutton.addActionListener(this);
				}
				else if (((elementsofproperty.get(metadatapropertyname).size()-1) == 0) && ((textfield.getText().equals("")))){

					JLabel shadowlabelfirst = new JLabel();

					shadowlabelfirst.setPreferredSize(shadowsize);

					minusbutton.setPreferredSize(preferredSize);
					minusbutton.setName("minus");

					minusbutton.setIcon(justremove);
					minusbutton.addActionListener(this);

					minusbutton.setVisible(false);

					propertybuilder.add(shadowlabelfirst).xy(5, 1);

					propertybuilder.add(minusbutton).xy(5, 1);

					button.setName("plus");

					button.setIcon(iiadd);

					propertybuilder.add(button).xy(7, (z*2)+1);

					if(required == true) {
						textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
						textfield.setToolTipText("Property required");
					}
				}
				else if (((elementsofproperty.get(metadatapropertyname).size()-1) == 0) && (!(textfield.getText().equals("")))) {
					minusbutton.setPreferredSize(preferredSize);
					minusbutton.setName("minus");

					minusbutton.setIcon(justremove);
					minusbutton.addActionListener(this);
					minusbutton.setVisible(true);
					propertybuilder.add(minusbutton).xy(5, (z*2)+1);

					button.setPreferredSize(preferredSize);
					button.setName("plus");

					button.setIcon(iiadd);
					button.addActionListener(this);

					propertybuilder.add(button).xy(7, (z*2)+1);
				}
				else if ((elementsofproperty.get(metadatapropertyname).size()) > 1){
					minusbutton.setPreferredSize(preferredSize);
					minusbutton.setName("minus");

					minusbutton.setIcon(iidel);
					minusbutton.setVisible(true);
					minusbutton.addActionListener(this);
					propertybuilder.add(minusbutton).xy(5, (z*2)+1);

					JLabel shadowlabelsecond = new JLabel();

					shadowlabelsecond.setPreferredSize(shadowsize);

					propertybuilder.add(shadowlabelsecond).xy(7, 1);

					/*if((required == true) && (textfield.getText().equals(""))){
						textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
						textfield.setToolTipText("Property required");
					}*/
				}

				button.addActionListener(this);


			} else if (index > 0) {
				minusbutton.setPreferredSize(preferredSize);
				minusbutton.setName("minus");

				minusbutton.setIcon(iidel);
				minusbutton.addActionListener(this);
				propertybuilder.add(minusbutton).xy(5, (z*2)+1);


			}
			if((index+1) == elementsofproperty.get(metadatapropertyname).size()) {
				button.setPreferredSize(preferredSize);
				button.setName("plus");

				button.setIcon(iiadd);
				button.addActionListener(this);

				propertybuilder.add(button).xy(7, (z*2)+1);
			}

			index++;

			propertybuilder.appendRows("$lg, pref");
			z++;
		}
		if (elementsofproperty.get(metadatapropertyname).size() > 1) {
			propertybuilder.addSeparator("").xyw(1, ((z*2)+1), 7);
			propertybuilder.appendRows("$lg, pref");
		}
		builder.add(newpropertypanel).xy(1, panelrow);
		}
	}

	public void refreshAppend() {
		for(String metadatapropertyname : addlistofkeys) {
			refreshOnePanel(metadatapropertyname);

		}
	}

	/**
	 * Appends a custom metadata property at the end of the of the GUI
	 * @param metadatapropertyname
	 * @param rowindex
	 */
	public void appendCustomMetadataProperty(String metadatapropertyname, int rowindex) {


		addlistofkeys.add(metadatapropertyname);

		String columns=customlabelwidth+",3dlu,"+customwidth+",3dlu,right:pref,3dlu,right:pref";
		String rows="pref";


		FormLayout layout = new FormLayout(columns,rows);


		JPanel addpropertypanel = propertypanels.get("additionalproperty");

		for(int z=0; z < addpropertypanel.getComponentCount(); z++) {
			if(addpropertypanel.getComponent(z).getClass().getTypeName().equals("javax.swing.JButton")) {
				((JButton)addpropertypanel.getComponent(z)).removeActionListener(this);
			}
		}

		addpropertypanel.removeAll();

		addpropertypanel.setLayout(layout);

		JPanel onepropertypanel = new JPanel();
		onepropertypanel.setLayout(layout);
		FormBuilder propertiesbuilder = FormBuilder.create();
		propertiesbuilder.columns(columns);
		propertiesbuilder.rows(rows);
		propertiesbuilder.panel(onepropertypanel);

		propertypanels.put(metadatapropertyname, onepropertypanel);

		GUIElement guielement = new GUIElement(new JLabel(), new JTextField(30), new JButton(), new JButton());

		List<GUIElement> elementslist = new ArrayList<>();
		elementslist.add(guielement);

		elementsofproperty.put(metadatapropertyname, elementslist);


		listofkeys.add(metadatapropertyname);
		//System.out.println("row "+listofkeys.size());

		JLabel label=elementsofproperty.get(metadatapropertyname).get(0).getLabel();



		label.setText(metadatapropertyname);

		propertiesbuilder.add(label).xy(1, 1);

		JTextField textfield=elementsofproperty.get(metadatapropertyname).get(0).getTextfield();

		textfield.getDocument().addDocumentListener(this);

		propertiesbuilder.add(textfield).xy(3, 1);

		JButton button=elementsofproperty.get(metadatapropertyname).get(0).getButton();

		button.setPreferredSize(preferredSize);
		button.setName("plus");

		button.setIcon(iiadd);
		button.addActionListener(this);

		propertiesbuilder.add(button).xy(7, 1);

		JButton deletebutton=elementsofproperty.get(metadatapropertyname).get(0).getMinusbutton();

		deletebutton.setPreferredSize(preferredSize);
		deletebutton.setName("minuscomplete");

		deletebutton.setIcon(iidelcomplete);
		deletebutton.addActionListener(this);

		propertiesbuilder.add(deletebutton).xy(5, 1);

		propertiesbuilder.appendRows("$lg, pref");


		builder.add(onepropertypanel).xy(1, rowindex);
		builder.appendRows("pref");



		rowindex++;

		addMetadataPropertyOption();
		JPanel onepanel = propertypanels.get("additionalproperty");

		builder.add(onepanel).xy(1, rowindex);
		builder.appendRows("pref");

		/*for (String name : listofkeys) {
			System.out.println(name);
		}*/

		Dimension labelsize=label.getPreferredSize();

		if (labelsize.width > currentlabelsize.width) {
			for(String propertyname : listofkeys) {
				elementsofproperty.get(propertyname).get(0).getLabel().setPreferredSize(labelsize);
				currentlabelsize=labelsize;
			}
		} else {
			for(String propertyname : listofkeys) {
				elementsofproperty.get(propertyname).get(0).getLabel().setPreferredSize(currentlabelsize);
			}
		}


	  	//Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
		//parentComponent.pack();
	}

	/**
	 * Adds a TextField with its corresponding elements to the Panel.
	 * @param metadatapropertyname
	 */
	public void addElementToPanel(String metadatapropertyname) {
		int index = 0;

		int panelrow=0;
		for(String propertyname : listofkeys) {
			panelrow++;
			if (metadatapropertyname.equals(propertyname)) {
				break;
			}
		}


		int size=elementsofproperty.get(metadatapropertyname).size();
		for(int counter=0; counter < size; counter++) {
			JTextField textfield=elementsofproperty.get(metadatapropertyname).get(counter).getTextfield();
			textfield.getDocument().removeDocumentListener(this);
		}


		String columns=customlabelwidth+",3dlu,"+customwidth+",3dlu,right:pref,3dlu,right:pref";
		String rows="pref";

		FormLayout layout = new FormLayout(columns,rows);

		JPanel onepropertypanel = propertypanels.get(metadatapropertyname);

		for(int z=0; z < onepropertypanel.getComponentCount(); z++) {
			if(onepropertypanel.getComponent(z).getClass().getTypeName().equals("javax.swing.JButton")) {
				((JButton)onepropertypanel.getComponent(z)).removeActionListener(this);
			}
		}

		JPanel newpropertypanel = new JPanel();


		FormBuilder propertybuilder = FormBuilder.create();
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);



		propertybuilder.panel(newpropertypanel);

		propertypanels.put(metadatapropertyname, newpropertypanel);
		onepropertypanel.removeAll();
		onepropertypanel.setLayout(layout);

		GUIElement guielement = new GUIElement(new JLabel(), new JTextField(30), new JButton(), new JButton());

		elementsofproperty.get(metadatapropertyname).add(guielement);

		int z=0;

		for(GUIElement oneguielement : elementsofproperty.get(metadatapropertyname)) {

			if (z == 0) {
				JLabel label=oneguielement.getLabel();

				label.setText(metadatapropertyname);;
				//label.setPreferredSize(currentlabelsize);
				propertybuilder.add(label).xy(1, (z*2)+1);
			}

			JTextField textfield=oneguielement.getTextfield();

			textfield.getDocument().addDocumentListener(this);

			propertybuilder.add(textfield).xy(3, (z*2)+1);


			JButton minusbutton=oneguielement.getMinusbutton();


			minusbutton.setPreferredSize(preferredSize);
			minusbutton.setName("minus");

			minusbutton.setIcon(iidel);
			minusbutton.addActionListener(this);
			propertybuilder.add(minusbutton).xy(5, (z*2)+1);

			minusbutton.setVisible(true);

			if((elementsofproperty.get(metadatapropertyname).size()-1) == index) {
				JButton button=oneguielement.getButton();
				button.setPreferredSize(preferredSize);
				button.setName("plus");

				button.setIcon(iiadd);
				button.addActionListener(this);

				propertybuilder.add(button).xy(7, (z*2)+1);
			}


			index++;

			propertybuilder.appendRows("$lg, pref");
			z++;
		}

		propertybuilder.addSeparator("").xyw(1, ((z*2)+1), 7);
		propertybuilder.appendRows("$lg, pref");

		builder.add(newpropertypanel).xy(1, panelrow);
		//Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
		//parentComponent.pack();

	}

	/**
	 * Removes a custom property
	 * @param metadatapropertyname
	 */
	public void removeCustomProperty(String metadatapropertyname) {
		int index=0;
		int customindex=0;
		for(String propertyname : listofkeys) {

			if (metadatapropertyname.equals(propertyname)) {
				break;
			}
			index++;
		}

		int size=elementsofproperty.get(metadatapropertyname).size();
		for(int counter=0; counter < size; counter++) {
			JTextField textfield=elementsofproperty.get(metadatapropertyname).get(counter).getTextfield();
			textfield.getDocument().removeDocumentListener(this);
		}

		for(String propertyname : addlistofkeys) {

			if (metadatapropertyname.equals(propertyname)) {
				break;
			}
			customindex++;
		}

		String columns=customlabelwidth+",3dlu,"+customwidth+",3dlu,right:pref,3dlu,right:pref";
		String rows="pref";

		FormLayout layout = new FormLayout(columns,rows);

		JPanel onepropertypanel = propertypanels.get(metadatapropertyname);

		for(int z=0; z < onepropertypanel.getComponentCount(); z++) {
			if(onepropertypanel.getComponent(z).getClass().getTypeName().equals("javax.swing.JButton")) {
				((JButton)onepropertypanel.getComponent(z)).removeActionListener(this);
			}
		}


		onepropertypanel.removeAll();
		onepropertypanel.setLayout(layout);

		elementsofproperty.remove(metadatapropertyname);
		listofkeys.remove(index);
		propertypanels.remove(metadatapropertyname);

		addlistofkeys.remove(customindex);


		if(addlistofkeys.size() > 0) {
			String property=addlistofkeys.get(0);
			JLabel templabel = new JLabel();
			templabel.setText(property);
			Dimension labelsize=templabel.getPreferredSize();
			currentlabelsize=labelsize;

			for(int i=1; i < addlistofkeys.size(); i++) {
				String anotherproperty=addlistofkeys.get(i);
				Dimension anotherlabelsize=elementsofproperty.get(anotherproperty).get(0).getLabel().getPreferredSize();
				if(labelsize.getWidth() > anotherlabelsize.getWidth()) {
					currentlabelsize=labelsize;
				} else {
					currentlabelsize=anotherlabelsize;
				}
			}
		}



		for(String propertyname : listofkeys) {
			for(GUIElement oneguielement : elementsofproperty.get(propertyname)) {
				if(addlistofkeys.isEmpty()) {
					oneguielement.getLabel().setPreferredSize(defaultlabelsize);
					oneguielement.getLabel().setSize(defaultlabelsize);
					currentlabelsize=defaultlabelsize;
				} else if (addlistofkeys.size() > 0) {
					oneguielement.getLabel().setPreferredSize(currentlabelsize);
					oneguielement.getLabel().setSize(currentlabelsize);
				}

			}
		}

		//Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
		//parentComponent.pack();
	}
}
