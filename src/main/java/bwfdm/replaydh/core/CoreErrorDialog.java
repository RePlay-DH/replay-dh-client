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
package bwfdm.replaydh.core;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Markus Gärtner
 *
 */
class CoreErrorDialog extends JFrame {

	private static final long serialVersionUID = 2706145795518241027L;

	CoreErrorDialog(Throwable t) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Container container = getContentPane();
        container.setLayout(new BorderLayout());
        JLabel header = new JLabel("Unexpected launcher error:"); //$NON-NLS-1$
        header.setBorder(new EmptyBorder(5, 10, 10, 10));
        container.add(header, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane();
        container.add(scrollPane, BorderLayout.CENTER);
        StringBuffer sb = new StringBuffer(t.toString());
        String nl = System.getProperty("line.separator"); //$NON-NLS-1$
        sb.append(nl).append(nl);
        Throwable err = t;
        while (err != null) {
            if (err != t) {
                sb.append(nl).append("Caused by " + err).append(nl).append(nl); //$NON-NLS-1$
            }
            StackTraceElement[] stackTrace = err.getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                sb.append(stackTrace[i].toString()).append(nl);
            }
            err = err.getCause();
        }
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setBackground(container.getBackground());
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 400));
        textArea.setCaretPosition(0);

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
