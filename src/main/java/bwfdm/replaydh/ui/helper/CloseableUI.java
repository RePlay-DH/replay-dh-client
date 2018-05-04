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
package bwfdm.replaydh.ui.helper;

import java.awt.Container;

import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.GuiUtils.GuiVisitResult;
import bwfdm.replaydh.utils.MutablePrimitives.MutableBoolean;

/**
 * @author Markus Gärtner
 *
 */
public interface CloseableUI {

	void close();

	default boolean canClose() {
		return true;
	}

	public static void tryClose(CloseableUI...targets) {
		for(CloseableUI target : targets) {
			if(target!=null) {
				target.close();
			}
		}
	}

	public static boolean canClose(CloseableUI...targets) {
		for(CloseableUI target : targets) {
			if(target!=null && !target.canClose()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Helper method that traverses the GUI tree below the
	 * given container and checks for every
	 *
	 * @param container
	 * @return
	 */
	public static boolean canClose(Container container) {

		MutableBoolean canClose = new MutableBoolean(true);

		GuiUtils.walkGUI(container, comp -> {

			boolean ignoreChildren = false;

			/*
			 *  Let components implementing the interface decide for themselves.
			 *  This also means that we will have to ignore all child components
			 *  of those containers!
			 */
			if(comp instanceof CloseableUI) {
				canClose.setBoolean(((CloseableUI)comp).canClose());
				ignoreChildren = true;
			}

			if(!canClose.booleanValue()) {
				return GuiVisitResult.STOP_TRAVERSAL;
			} else if(ignoreChildren) {
				return GuiVisitResult.IGNORE_CHILDREN;
			} else {
				return GuiVisitResult.CONTINUE;
			}
		});

		return canClose.booleanValue();
	}
}
