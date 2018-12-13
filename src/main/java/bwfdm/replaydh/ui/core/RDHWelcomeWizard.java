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
package bwfdm.replaydh.ui.core;

import java.awt.Window;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;

/**
 * @author Markus Gärtner
 *
 */
public abstract class RDHWelcomeWizard {

	public static Wizard<WelcomeContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<WelcomeContext> wizard = new Wizard<>(
				parent, "welcome", ResourceManager.getInstance().get("replaydh.wizard.welcome.title"),
				environment, INIT, IDENTITY, /*WORKSPACE, VALIDATE_WORKSPACE,*/ FINISH);

		return wizard;
	}

	public static final class WelcomeContext {
		String username;
		String organization;
		Path workspace;
		WorkspaceValidator.WorkspaceState workspaceState;

		public Path getWorkspace() {
			return workspace;
		}

		public String getUsername() {
			return username;
		}

		public String getOrganization() {
			return organization;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static abstract class WelcomeStep extends AbstractWizardStep<WelcomeContext> {
		protected WelcomeStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}


	/**
	 * First step - just a textual introduction
	 */
	private static final WelcomeStep INIT = new WelcomeStep(
			"init",
			"replaydh.wizard.welcome.init.title",
			"replaydh.wizard.welcome.init.description") {

		@Override
		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
			return IDENTITY;
		}

		@Override
		protected JPanel createPanel() {
			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.init.message"))).xy(1, 1)
					.build();
		}
	};

	/**
	 * Ask user for username and identity of organization
	 */
	private static final WelcomeStep IDENTITY = new WelcomeStep(
			"identity",
			"replaydh.wizard.welcome.identity.title",
			"replaydh.wizard.welcome.identity.description") {

		JTextField tfOrganization;
		JTextField tfUsername;

		@Override
		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
			context.username = tfUsername.getText().trim();
			context.organization = tfOrganization.getText().trim();

			return FINISH;
		}

		/**
		 * Allowed punctuation symbols: -_.@
		 * Invalid:
		 * 		A control character: [\x00-\x1F\x7F]
		 * 		A whitespace character (other than space): [\t\n\x0B\f\r]
		 * 		Punctuation: One of !"#$%&'()*+,/:;<=>?[\]^`{|}~}
		 */
		private final Matcher INVALID_NAME_MATCHER = Pattern.compile(
				"[!\"#$%&'\\(\\)\\*+,/:;<=>\\?\\[\\\\]\\^`\\{|\\}~\t\n\\x{0B}\f\r\\p{Cntrl}]").matcher("");

		private boolean isValidUsername() {
			String username = tfUsername.getText().trim();
			if(username.isEmpty()) {
				return false;
			}

//			INVALID_NAME_MATCHER.reset(username);

//			return !INVALID_NAME_MATCHER.find();

			for(int i=0; i<username.length(); i++) {
				char c = username.charAt(i);
				if(c=='_' || c=='-' || c=='.' || c==' ') {
					continue;
				}

				int codePoint = username.codePointAt(i);
				if(codePoint!=c) {
					i++;
				}

				if(!Character.isLetterOrDigit(codePoint)) {
					return false;
				}
			}

			return true;
		}

		@Override
		protected JPanel createPanel() {
			tfOrganization = new JTextField(20);
			tfUsername = new JTextField(20);
			tfUsername.getDocument().addDocumentListener(new DocumentAdapter(){
				@Override
				public void anyUpdate(DocumentEvent e) {
					setNextEnabled(isValidUsername());
				}
			});

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref, $nlg, pref, $nlg, top:pref, $nlg, pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.identity.user"))).xy(1, 1, "fill, top")
					.add(tfUsername).xy(1, 3, "center, center")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.identity.organization"))).xy(1, 5, "fill, top")
					.add(tfOrganization).xy(1, 7, "center, center")
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, WelcomeContext context) {
			String username = context.username;
			if(username==null) {
				username = environment.getProperty(RDHProperty.CLIENT_USERNAME);
			}
			tfUsername.setText(username);

			String organization = context.organization;
			if(organization==null) {
				organization = environment.getProperty(RDHProperty.CLIENT_ORGANIZATION);
			}
			tfOrganization.setText(organization);

			setNextEnabled(isValidUsername());
		}
	};

	/**
	 * Wrap up info
	 */
	private static final WelcomeStep FINISH = new WelcomeStep(
			"finish",
			"replaydh.wizard.welcome.finish.title",
			"replaydh.wizard.welcome.finish.description") {

		@Override
		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
			return null;
		}

		@Override
		protected JPanel createPanel() {
			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.finish.message"))).xy(1, 1)
					.build();
		}
	};
}
