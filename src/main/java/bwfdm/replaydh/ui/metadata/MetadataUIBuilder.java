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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.EditorControl;
import bwfdm.replaydh.utils.Label;

/**
 * @author Florian Fritze
 *
 */

public class MetadataUIBuilder extends MetadataUI<MetadataBuilder> implements Editor<MetadataBuilder> , ActionListener, DocumentListener, ComponentListener {

	private MetadataRecord metadatarecord = null;

	/**
	 * Main JPanel as the basis for other panels
	 */

	private JPanel panel = null;

	private EditorControl editorcontrol = null;

	private boolean disableneeded=false;


	public MetadataUIBuilder() {

		this.panel = new JPanel();

		//this.setControl(editorcontrol);

		this.panel.addComponentListener(this);

	}


	/**
	 * Setting of the variables that hold the GUI components (JTextField and JLabel, JButton)
	 */
	public void setVariables() {

		/*
		 * Retrieving the required values
		 */
		for(Label item: metadataFrontEnd.getRequiredNames()) {
			requiredkeys.add(item.getLabel());
		}

		/*
		 * Creating an array that holds the allowed values
		 */
		List<String> optionalkeys = new ArrayList<String>();

		/*
		 * Retrieving the allowed values
		 */

		for(Label item: metadataFrontEnd.getAllowedNames()) {
			optionalkeys.add(item.getLabel());
		}
		List<String> redundantkeys = new ArrayList<String>();
		for(String requireditem : requiredkeys) {
			for(String optionalitem : optionalkeys) {
				if(optionalitem.equals(requireditem)) {
					redundantkeys.add(optionalitem);
				}
			}
		}
		for(String removeitem : redundantkeys) {
			optionalkeys.remove(removeitem);
		}

		requiredkeys.sort(String::compareToIgnoreCase);
		requiredkeys.add(0,"requiredseparator");

		listofkeys.addAll(requiredkeys);
		optionalkeys.sort(String::compareToIgnoreCase);
		optionalkeys.add(0,"optionalseparator");
		listofkeys.addAll(optionalkeys);

		keys.put("required", requiredkeys);
		keys.put("optional", optionalkeys);

		for(String metadatapropertyname: listofkeys) {
			GUIElement guielement = new GUIElement(new JLabel(metadatapropertyname), new JTextField(30), new JButton(), new JButton());

			List<GUIElement> elementslist = new ArrayList<>();
			elementslist.add(guielement);

			elementsofproperty.put(metadatapropertyname, elementslist);


		}

		for(String metadatapropertyname: listofkeys) {
			GUIElement guielement = elementsofproperty.get(metadatapropertyname).get(0);
			JLabel firstlabel = guielement.getLabel();
			JTextField textfield = guielement.getTextfield();
			border=textfield.getBorder();
			Dimension dimension = firstlabel.getPreferredSize();
			neededheight = dimension.getHeight();
			break;
		}

		Double secondwidth=null;
		Double resultwidth=null;

		for(String metadatapropertyname : listofkeys) {
			if ((metadatapropertyname.equals("additionalproperty")) || (metadatapropertyname.equals("requiredseparator")) || (metadatapropertyname.equals("optionalseparator"))) {
				continue;
			}
			GUIElement guielement = elementsofproperty.get(metadatapropertyname).get(0);
			JLabel onelabel=guielement.getLabel();
			onelabel.setText(metadatapropertyname);
			resultwidth=onelabel.getPreferredSize().getWidth();
			for(String propertyname : listofkeys) {
				if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
					continue;
				}
				GUIElement secondguielement = elementsofproperty.get(propertyname).get(0);
				JLabel secondlabel=secondguielement.getLabel();
				secondlabel.setText(propertyname);
				secondwidth=secondlabel.getPreferredSize().getWidth();
				if(resultwidth >= secondwidth) {
					continue;
				} else {
					resultwidth=secondwidth;
				}
			}
			break;
		}
		neededwidth=resultwidth+20;
		currentlabelsize=new Dimension(neededwidth.intValue(),neededheight.intValue());
		defaultlabelsize=new Dimension(neededwidth.intValue(),neededheight.intValue());
		customlabelwidth="max("+neededwidth.intValue()+"px;pref)";
	}


	/**
	 * Checks if there are existing values for one property
	 */
	public void checkForValues(String property) {
		boolean found=false;
		for(String propertyname : requiredkeys) {
			if(property.equals(propertyname)) {
				found=true;
			}
			if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
				continue;
			}
		}
		if(found == true) {
			boolean onepropertyexists=false;
			int size=elementsofproperty.get(property).size();
			for(int index=0; index < size; index++) {
				JTextField textfield=elementsofproperty.get(property).get(index).getTextfield();
				if(textfield.getText().equals("")) {
					if(size == 1) {
						elementsofproperty.get(property).get(0).getMinusbutton().setVisible(false);
					}
				} else {
					elementsofproperty.get(property).get(0).getMinusbutton().setVisible(true);
					onepropertyexists=true;
				}
			}
			if(onepropertyexists == false) {
				JTextField textfield=elementsofproperty.get(property).get(0).getTextfield();
				textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
				textfield.setToolTipText("Property required");
				disableneeded=true;
			}
		}
	}


	/**
	 * Removes lines that hold one GUIElement if they are empty.
	 */
	public void clearGUI() {

		int size=0;
		close();
		for(String metadatapropertyname : listofkeys) {
			if ((metadatapropertyname.equals("additionalproperty")) || (metadatapropertyname.equals("requiredseparator")) || (metadatapropertyname.equals("optionalseparator"))) {
				continue;
			}
			size=elementsofproperty.get(metadatapropertyname).size();

			if (size > 1) {
				for (int z=size; z > 1; z--) {
					if (elementsofproperty.get(metadatapropertyname).get(z-1).getTextfield().getText().equals("")) {
						elementsofproperty.get(metadatapropertyname).remove(z-1);
						refreshOnePanel(metadatapropertyname);
					}
				}
			}

		}
		//Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
		//parentComponent.pack();
	}




	private void ensureUI() {
		//TODO setup our panel and other ui stuff

		builder.padding(new EmptyBorder(10, 10, 10, 10));



		builder.columns("fill:pref:grow");
		builder.rows("pref");

		builder.panel(panel);


		paintGUI();

	}

	@Override
	public JPanel getEditorComponent() {
		// TODO Auto-generated method stub
		ensureUI();

		return builder.build();
	}


	@Override
	public void resetEdit() {
		// TODO Auto-generated method stub

		clearGUI();

		record=metadatarecord;

		refreshUI();

		Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
		parentComponent.pack();

	}

	@Override
	public void applyEdit() {
		boolean isduplicate=false;
		propertyrequired=false;
		metadataFrontEnd.reset();

		// TODO Auto-generated method stub
		if (metadataFrontEnd == null) {
			throw new IllegalStateException("No metadata record to edit");
		}
		else {
			//record=metadataBuilder.build();
			/*
			 * Begins with the iteration of all allowed metadata property values
			 */
			for(String metadatapropertyname : listofkeys) {
				List<String> list = new ArrayList<>();
				propertyvalues.put(metadatapropertyname, list);
				if ((metadatapropertyname.equals("additionalproperty") || (metadatapropertyname.equals("requiredseparator")) || (metadatapropertyname.equals("optionalseparator")))) {
					continue;
				}
				/*
				 * Iterates over all ingested entries in the components (textfields)
				 */
				propertyvalues.get(metadatapropertyname).clear();
				for(GUIElement guielement : elementsofproperty.get(metadatapropertyname) ) {

					isduplicate=false;
					/*
					 * Checks if the current JTextField is not empty
					 */
					if (!(guielement.getTextfield().getText().equals(""))) {
						if(!(metadatarecord == null)) {
							if(metadatarecord.getEntryCount(metadatapropertyname) > 0) {
								for(Entry entry : metadatarecord.getEntries(metadatapropertyname)) {
									/*
									 * Checks for a duplicate entry
									 */
									if(guielement.getTextfield().getText().equals(entry.getValue())) {
										isduplicate=true;
									}
								}
								/*
								 * If there is a duplicate entry the entry will not be ingested in the metadata record.
								 */
								if (isduplicate) {
									continue;
								}
							}
						}


						/*
						 * Checks if there are restrictions for the input of values in the MetadataRecord
						 */
						if(metadataFrontEnd.isValuesLimited(metadatapropertyname)) {
							/*
							 * Iterates over all allowed values for the MetadataRecord for one certain metadatapropertyname.
							 */
							for(Label allowed : metadataFrontEnd.getAllowedValues(metadatapropertyname)) {
							/*
							 * Checks if there is an allowed value in the JTextField and allows to save it
							 * (if it is) in the MetadataRecord
							 */
								if(allowed.getLabel().equals(guielement.getTextfield().getText())) {
									String value=guielement.getTextfield().getText();
									metadataFrontEnd.addEntry(metadatapropertyname,value);

									propertyvalues.get(metadatapropertyname).add(value);
								}
							}
							/*
							 * Condition is true if there are no input restrictions
							 */
						} else {
							String value=guielement.getTextfield().getText();
							metadataFrontEnd.addEntry(metadatapropertyname,value);

							propertyvalues.get(metadatapropertyname).add(value);
						}
					}
				}
			}
//			metadatarecord=metadataFrontEnd.build();
		}
	}

	@Override
	public boolean hasChanges() {
		// TODO Auto-generated method stub
		propertyrequired=false;
		for(String propertyname : requiredkeys) {
			if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
				continue;
			}
			if(elementsofproperty.get(propertyname).get(0).getTextfield().getText().trim().equals("")) {
				propertyrequired=true;
				break;
			}
		}

		if ((metadataFrontEnd == null) || (propertyrequired == true) || (metadatarecord == null)) {
			return false;
		} else {

			boolean changed = false;
			/*
			 * Begins with the iteration of all allowed metadata property values
			 */
			for(String metadatapropertyname : listofkeys) {
				if ((metadatapropertyname.equals("additionalproperty")) || (metadatapropertyname.equals("requiredseparator")) || (metadatapropertyname.equals("optionalseparator"))) {
					continue;
				}
				if (metadatarecord.hasMultipleEntries(metadatapropertyname)) {
					for(Entry entry: metadatarecord.getEntries()) {
						for(GUIElement guielement : elementsofproperty.get(metadatapropertyname)) {
							changed = changed || (!Objects.equals(guielement.getTextfield().getText(),entry.getValue())) ? true : false;
						}
					}
				}
				else if ((metadatarecord.hasEntries(metadatapropertyname)) && (elementsofproperty.get(metadatapropertyname).size() > 1)) {
					for(int index=0; index < elementsofproperty.get(metadatapropertyname).size(); index++) {
						changed = changed || (!Objects.equals(elementsofproperty.get(metadatapropertyname).get(index).getTextfield().getText(),metadatarecord.getEntry(metadatapropertyname).getValue())) ? true : false;
					}

				}
				else if (metadatarecord.getEntryCount(metadatapropertyname) == 1) {
					changed = changed || (!Objects.equals(elementsofproperty.get(metadatapropertyname).get(0).getTextfield().getText(),metadatarecord.getEntry(metadatapropertyname).getValue())) ? true : false;
				}
				else if (!(elementsofproperty.get(metadatapropertyname).get(0).getTextfield().getText().equals(""))) {
					changed = changed || (!(elementsofproperty.get(metadatapropertyname).get(0).getTextfield().getText().equals(""))) ? true : false;
				}
				for(String propertyname : addlistofkeys) {
					if(propertyname.equals(metadatapropertyname)) {
						for(int index=0; index < elementsofproperty.get(propertyname).size(); index++) {
							changed = changed || (!(elementsofproperty.get(propertyname).get(index).getTextfield().getText().equals(""))) ? true : false;
						}
					}
				}
			}
			return changed;
		}

	}
	/*
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * Checks which button is pressed and adds a label and a textfield in the corresponding component and labelmaps.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		Object source = e.getSource();

		JButton buttonpressed = null;
		JButton minusbuttonpressed = null;
		int lastindex;
		int numberofguielements;
		for (String propertyname : listofkeys) {
			lastindex=elementsofproperty.get(propertyname).size()-1;
			buttonpressed=elementsofproperty.get(propertyname).get(lastindex).getButton();
			minusbuttonpressed=elementsofproperty.get(propertyname).get(0).getMinusbutton();
			/*
			 * Beginning of Non-Minus-Button actions
			 */
			if (source == buttonpressed) {
				if(buttonpressed.getName().toString().equals("plus")) {
					addElementToPanel(propertyname);
					Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
					parentComponent.pack();
					break;
				} else if (buttonpressed.getName().toString().equals("addproperty")) {

					MetadataPropertyDialog dialog = new MetadataPropertyDialog(listofkeys, metadataFrontEnd);

					dialog.setLocationRelativeTo(this.panel);
					dialog.setVisible(true);

					if (!(dialog.getTextfield().getText().equals(""))) {
						boolean found=false;
						String metadataproperty="";
						int row=listofkeys.size();;
						metadataproperty=dialog.getTextfield().getText();
						for(String property : listofkeys) {
							if(property.equals(metadataproperty)) {
								found=true;

								break;

							}

						}
						if (found == false) {
							refreshAppend();
							appendCustomMetadataProperty(metadataproperty.trim(),row);
							Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
							parentComponent.pack();
							break;
						}
					}

					break;
				}
			}
			if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
				continue;
			}
			boolean completeminuspressed=false;
			for (int i=0; i< elementsofproperty.get(propertyname).size(); i++) {
				numberofguielements=elementsofproperty.get(propertyname).size();
				lastindex=elementsofproperty.get(propertyname).size()-1;
				minusbuttonpressed=elementsofproperty.get(propertyname).get(i).getMinusbutton();
				/*
				 * Beginning of MinusButton actions
				 */
				if(source == minusbuttonpressed) {
					/*
					 * Removes the last GUIElement of a property if there are more than GUIElements for this property
					 */
					if((numberofguielements > 1) && (i == 0)) {
						elementsofproperty.get(propertyname).remove(i);
						refreshOnePanel(propertyname);
						checkForValues(propertyname);
						if(disableneeded == true) {
							setApplyEnabled(false);
						} else {
							setApplyEnabled(true);
						}
						Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
						parentComponent.pack();
						break;
					}
					/*
					 * Removes a certain GUIElement at position i
					 */
					else if((numberofguielements > 1) && (i > 0)){
						elementsofproperty.get(propertyname).remove(i);
						refreshOnePanel(propertyname);
						checkForValues(propertyname);
						if(disableneeded == true) {
							setApplyEnabled(false);
						} else {
							setApplyEnabled(true);
						}
						Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
						parentComponent.pack();
						break;
					}
					/*
					 * Removes a custom metadata property
					 */
					else if(minusbuttonpressed.getName().equals("minuscomplete")) {
						//metadataEditor.removeAllEntries(propertyname);
						removeCustomProperty(propertyname);
						completeminuspressed=true;
						Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
						parentComponent.pack();
						break;
					} else
					/*
					 * Just removes the text of the last Textfield of a property
					 */
					{
						JTextField textfield=elementsofproperty.get(propertyname).get(0).getTextfield();
						textfield.setText("");
						refreshOnePanel(propertyname);
						Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, panel);
						parentComponent.pack();
						break;
					}
				}
			}
			if(completeminuspressed == true) {
				break;
			}
		}



	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		for (String metadatapropertyname : listofkeys) {
			if ((metadatapropertyname.equals("additionalproperty")) || (metadatapropertyname.equals("requiredseparator")) || (metadatapropertyname.equals("optionalseparator"))) {
				continue;
			}
			for(GUIElement guielement : elementsofproperty.get(metadatapropertyname)) {
				guielement.getTextfield().setText("");
				guielement.getTextfield().setBorder(border);
			}
		}
	}


	@Override
	public void insertUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		for(String propertyname: addlistofkeys) {
			int size=elementsofproperty.get(propertyname).size();
			if((!(elementsofproperty.get(propertyname).get(0).getTextfield().getText().equals(""))) && (size > 1)) {
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setName("minus");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setIcon(iidel);
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setToolTipText(null);
			} else if((elementsofproperty.get(propertyname).get(0).getTextfield().getText().equals("")) && (size == 1)) {
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setName("minuscomplete");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setIcon(iidelcomplete);
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setToolTipText("Remove custom property from GUI");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setVisible(true);
			} else if((!(elementsofproperty.get(propertyname).get(0).getTextfield().getText().equals(""))) && (size == 1)){
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setName("minus");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setIcon(justremove);
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setToolTipText(null);
			}
		}
		disableneeded=false;
		for(String propertyname : listofkeys) {
			if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
				continue;
			}
			int size=elementsofproperty.get(propertyname).size();
			for(int index=0; index < size; index++) {
				JTextField textfield=elementsofproperty.get(propertyname).get(index).getTextfield();
				if(metadataFrontEnd.isAllowedValue(propertyname,textfield.getText())) {
					textfield.setBorder(border);
					textfield.setToolTipText(null);
				} else {
					textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
					textfield.setToolTipText("Invalid property value");
					disableneeded=true;
				}
			}
			checkForValues(propertyname);
			if(disableneeded == true) {
				setApplyEnabled(false);
			} else {
				setApplyEnabled(true);
			}
		}
	}


	@Override
	public void removeUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		for(String propertyname: addlistofkeys) {
			int size=elementsofproperty.get(propertyname).size();
			if((!(elementsofproperty.get(propertyname).get(0).getTextfield().getText().equals(""))) && (size > 1)) {
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setName("minus");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setIcon(iidel);
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setToolTipText(null);
			} else if((elementsofproperty.get(propertyname).get(0).getTextfield().getText().equals("")) && (size == 1)) {
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setName("minuscomplete");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setIcon(iidelcomplete);
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setToolTipText("Remove custom property from GUI");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setVisible(true);
			} else if((!(elementsofproperty.get(propertyname).get(0).getTextfield().getText().equals(""))) && (size == 1)){
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setName("minus");
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setIcon(justremove);
				elementsofproperty.get(propertyname).get(0).getMinusbutton().setToolTipText(null);
			}
		}
		disableneeded=false;
		for(String propertyname : listofkeys) {
			if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
				continue;
			}
			int size=elementsofproperty.get(propertyname).size();
			for(int index=0; index < size; index++) {
				JTextField textfield=elementsofproperty.get(propertyname).get(index).getTextfield();
				if(metadataFrontEnd.isAllowedValue(propertyname,textfield.getText())) {
					textfield.setBorder(border);
					textfield.setToolTipText(null);
				} else {
					textfield.setBorder(BorderFactory.createLineBorder(Color.RED,2));
					textfield.setToolTipText("Invalid property value");
					disableneeded=true;
				}
			}
			checkForValues(propertyname);
			if(disableneeded == true) {
				setApplyEnabled(false);
			} else {
				setApplyEnabled(true);
			}
		}
	}


	@Override
	public void changedUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setControl(EditorControl editorControl) {
		this.editorcontrol=editorControl;
		boolean enabledallowed=true;
		for(String key : requiredkeys) {
			if (key.equals("requiredseparator")) {
				continue;
			}
			JTextField textfield = elementsofproperty.get(key).get(0).getTextfield();
			if(textfield.getText().equals("")) {
				enabledallowed=false;
			}
		}
		if(enabledallowed == false) {
			this.editorcontrol.setApplyEnabled(false);
		}
	}

	private void setApplyEnabled(boolean enabled) {
		if(editorcontrol != null) {
			editorcontrol.setApplyEnabled(enabled);
		}
	}


	@Override
	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		for(String propertyname : listofkeys) {
			if ((propertyname.equals("additionalproperty")) || (propertyname.equals("requiredseparator")) || (propertyname.equals("optionalseparator"))) {
				continue;
			}
			customwidth=elementsofproperty.get(propertyname).get(0).getTextfield().getWidth()+"px:grow";
			break;
		}
		//System.out.println(customwidth);
	}


	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub

	}


	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub

	}


	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub

	}


	@Override
	public void setEditingItem(MetadataBuilder item) {
		// TODO Auto-generated method stub
		this.metadataFrontEnd=item;
		setVariables();
	}


	@Override
	public MetadataBuilder getEditingItem() {
		// TODO Auto-generated method stub
		return this.metadataFrontEnd;
	}

}
