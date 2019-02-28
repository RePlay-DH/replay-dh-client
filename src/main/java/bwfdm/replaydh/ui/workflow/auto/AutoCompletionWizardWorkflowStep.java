package bwfdm.replaydh.ui.workflow.auto;

import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JPanel;

import bwfdm.replaydh.resources.ResourceManager;

public class AutoCompletionWizardWorkflowStep {
	
	public AutoCompletionWizardWorkflowStep() {
		
	}

	private JDialog wizardWindow;
	private Frame frame = null;
	private ResourceManager rm = ResourceManager.getInstance();
	
	public void createWizard() {
		JPanel mainPanelWizard = new JPanel();
		wizardWindow = new JDialog(frame, true);
		wizardWindow.setTitle(rm.get("replaydh.wizard.metadataAutoWizard.title"));
		wizardWindow.setSize(500,600);
		wizardWindow.setLocationRelativeTo(null);
		wizardWindow.setVisible(true);
		wizardWindow.add(mainPanelWizard);
	}
	
}
