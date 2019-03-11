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
package bwfdm.replaydh.ui.workflow.graph;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;

import javax.swing.Icon;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxRectangleShape;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.WorkflowUtils;

/**
 * Renders workflow steps as rectangular cells with the following
 * space allocations:
 * <p>
 * +-----------+----------+-----------+
 * |           |          |           |
 * |           |  AREA A  |           |
 * |           +----------+           |
 * |   INPUT   |          |  OUTPUT   |
 * |           |          |           |
 * |           |  AREA B  |           |
 * |           |          |           |
 * +-----------+----------+-----------+
 * <p>
 * The placeholder areas {@code A} and {@code B} are allocated as
 * necessary depending on whether or not the workflow step contains
 * both a {@link WorkflowStep#getTool() tool} and {@link WorkflowStep#getPersons() persons}.
 *
 *
 * @author Markus Gärtner
 *
 */
public class WorkflowStepShape extends mxRectangleShape {

	private static final String CACHED_STEP_IMAGE = "RDH_cachedStepImage";

	private static final IconRegistry ICON_REGISTRY;
	static {
		ICON_REGISTRY = IconRegistry.newRegistry(null);
		ICON_REGISTRY.addSearchPath("bwfdm/replaydh/ui/workflow/graph/");
	}

	public static Rectangle getPreferredCellSize(WorkflowStep step) {
		Rectangle r = new Rectangle(0, 64);

		if(!WorkflowUtils.isInitial(step)
				&& !WorkflowUtils.isForeignCommit(step)) {
			int inputs = step.getInputCount();
			int outputs = step.getOutputCount();
			int persons = step.getPersonsCount();
			boolean tool = step.getTool()!=null;

			if(inputs>0) {
				r.width += 64;
			}

			if(outputs>0) {
				r.width += 64;
			}

			if(persons>0 || tool) {
				r.width += 64;
			}

			if(persons>0 && tool) {
				r.height += 32;
			} else if(inputs>1 || outputs>1 || persons>1) {
				r.height += 16;
			}
		}

		// Make sure we have room to show a default icon
		if(r.width<64) {
			r.width = 64;
		}

		return r;
	}

	//DEBUG static definitions for icons

	public static final Icon ICON_PERSONS = ICON_REGISTRY.getIcon("User Group Man Woman-64.png");
	public static final Icon ICON_TOOL = ICON_REGISTRY.getIcon("Job-64.png");
	public static final Icon ICON_RESOURCE_INPUT = ICON_REGISTRY.getIcon("Google Forms-64.png");
	public static final Icon ICON_RESOURCE_OUTPUT = ICON_REGISTRY.getIcon("Google Forms-64.png");
	public static final Icon ICON_UNKNOWN = ICON_REGISTRY.getIcon("icons8-question-mark-64.png");

	private static final Image FOREIGN_COMMIT_IMAGE;
	static {

		BufferedImage image = new BufferedImage(
				ICON_UNKNOWN.getIconWidth(),
				ICON_UNKNOWN.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		ICON_UNKNOWN.paintIcon(null, g, 0, 0);
		g.dispose();

		FOREIGN_COMMIT_IMAGE = image;
	}

	/**
	 * @see com.mxgraph.shape.mxRectangleShape#paintShape(com.mxgraph.canvas.mxGraphics2DCanvas, com.mxgraph.view.mxCellState)
	 */
	@Override
	public void paintShape(mxGraphics2DCanvas canvas, mxCellState state) {
		super.paintShape(canvas, state);

		paintWorkflowImage(canvas, state);
	}

	public void paintWorkflowImage(mxGraphics2DCanvas canvas, mxCellState state) {

		Image image = getWorkflowImage(canvas, state);
		if(image!=null) {
			Graphics2D graphics = canvas.getGraphics();
			Rectangle bounds = getImageBounds(canvas, state);
			graphics.drawImage(image, bounds.x, bounds.y, null);
		}
	}

	public Rectangle getImageBounds(mxGraphics2DCanvas canvas, mxCellState state) {
		return state.getRectangle();
	}

	public Image getWorkflowImage(mxGraphics2DCanvas canvas, mxCellState state) {
		Map<String, Object> style = state.getStyle();
		Image image = (Image) style.get(CACHED_STEP_IMAGE);
		if(image==null || isInvalidImageSize(canvas, state, image)) {
			image = createWorkflowImage(canvas, state);
			style.put(CACHED_STEP_IMAGE, image);
		}

		return image;
	}

	/**
	 * Create a static image of the specified workflow step.
	 */
	private Image createWorkflowImage(mxGraphics2DCanvas canvas, mxCellState state) {

		// Create image with specified cell size
		Rectangle bounds = getImageBounds(canvas, state);
		BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);

		// Fetch workflow step
		Object cell = state.getCell();
		mxGraph graph = state.getView().getGraph();
		WorkflowStep step = (WorkflowStep) graph.getModel().getValue(cell);

		// Don't bother constructing complex images for foreign commits
		if(WorkflowUtils.isForeignCommit(step)) {
			return FOREIGN_COMMIT_IMAGE;
		}

		// Do we rly need a Graphics2D instead of regular Graphics object?
		Graphics2D g = image.createGraphics();
		// Make sure we do a smooth rendering
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Paint workflow step on image
		int inputs = step.getInputCount();
		int outputs = step.getOutputCount();
		int persons = step.getPersonsCount();
		boolean tool = step.getTool()!=null;

//		System.out.printf("creating image: step=%s inputs=%d outputs=%d persons=%d tool=%b\n",
//				step, inputs, outputs, persons, tool);

		int x = 0;
		if(inputs>0) {
			paintImage(g, x, 0, ICON_RESOURCE_INPUT, inputs);
			x += 64;
		}

		int y = 0;
		if(tool) {
			paintImage(g, x, y, ICON_TOOL, 1);
			y += 32;
		}

		if(persons>0) {
			paintImage(g, x, y, ICON_PERSONS, persons);
		}

		if(tool || persons>0) {
			x += 64;
		}

		if(outputs>0) {
			paintImage(g, x, 0, ICON_RESOURCE_OUTPUT, outputs);
		}

		if(x==0) {
			//TODO show a default icon for "thinking" steps
		}

		// cleanup
		g.dispose();

		return image;
	}

	private static void paintImage(Graphics2D g, int x, int y, Icon icon, int count) {
		icon.paintIcon(null, g, x, y);

		// Write count centered and below the icon
		if(count > 1) {
			FontMetrics fm = g.getFontMetrics();

			String text = String.valueOf(count);

			Color c = g.getColor();
			g.setColor(Color.BLACK);

			x = x+(icon.getIconWidth()/2)-(fm.stringWidth(text)/2);
			y = y+icon.getIconHeight()+fm.getLeading()+fm.getAscent();

			g.drawString(text, x, y);
			g.setColor(c);
		}
	}

	/**
	 * Check if the available size of the cell has changed and
	 * the image needs to be created anew.
	 */
	private boolean isInvalidImageSize(mxGraphics2DCanvas canvas, mxCellState state, Image image) {
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		if(w==-1 || h==-1) {
			return false;
		}

		Rectangle bounds = getImageBounds(canvas, state);

//		System.out.printf("wI=%d hI=%d wB=%d hB=%d\n",w, h, bounds.width, bounds.height);

		return w!=bounds.width || h!=bounds.height;
	}
}
