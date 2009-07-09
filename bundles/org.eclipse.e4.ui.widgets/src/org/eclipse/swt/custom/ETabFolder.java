/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.custom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class ETabFolder extends CTabFolder {

	boolean webbyStyle = false;

	static final int[] E4_TOP_LEFT_CORNER = new int[] {0,6, 1,5, 1,4, 4,1, 5,1, 6,0};
	static final int[] E4_TOP_RIGHT_CORNER = new int[] {0,0, 1,1, 2,1, 4,5, 5,5, 6,6};
	
	int topMargin = 4;  //The space above the highest (selected) tab
	int selectionMargin = 4;  //bonus margin for selected tabs
	int tabTopMargin = 4;  //margin within tab above text below line
	int tabBottomMargin = 4; //bottom margin within tab
	int hSpace = 2;  //horizontal spacing between tabs
	int leftMargin = topMargin;  //first horizontal space
	
/**
 * @param parent
 * @param style
 */
public ETabFolder(Composite parent, int style) {
	super(parent, style);
}

public boolean getWebbyStyle() {
	return webbyStyle;
}

public void setWebbyStyle(boolean webbyStyle) {
	checkWidget();
	this.webbyStyle = webbyStyle;
	layout();
	redrawTabs();
	redraw();
	if(this.webbyStyle != webbyStyle) {
		this.webbyStyle = webbyStyle;
		updateTabHeight(true);
		if(webbyStyle && single) {
			setSingle(false); //will cause update
			return; //no update needed
		}
		Rectangle rectBefore = getClientArea();
		updateItems();
		Rectangle rectAfter = getClientArea();
		if (!rectBefore.equals(rectAfter)) {
			notifyListeners(SWT.Resize, new Event());
		}
		redraw();
	}
}

int getTextMidline() {
	return (tabHeight - topMargin - selectionMargin) /2 + topMargin + selectionMargin;
}

void drawTabArea(Event event) {
	if (!webbyStyle || onBottom || single) {
		super.drawTabArea(event);
		return;
	}

	GC gc = event.gc;
	Point size = getSize();
	int[] shape = null;
	Color borderColor = getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY); //TODO parameterize

	if (tabHeight == 0) {
		int style = getStyle();
		if ((style & SWT.FLAT) != 0 && (style & SWT.BORDER) == 0)
			return;
		int x1 = borderLeft - 1;
		int x2 = size.x - borderRight;
		int y1 = borderTop + highlight_header;
		int y2 = borderTop;
		if (borderLeft > 0 && onBottom)
			y2 -= 1;

		shape = new int[] { x1, y1, x1, y2, x2, y2, x2, y1 };

		// If horizontal gradient, show gradient across the whole area
		if (selectedIndex != -1 && selectionGradientColors != null
				&& selectionGradientColors.length > 1
				&& !selectionGradientVertical) {
			drawBackground(gc, shape, true);
		} else if (selectedIndex == -1 && gradientColors != null
				&& gradientColors.length > 1 && !gradientVertical) {
			drawBackground(gc, shape, false);
		} else {
			gc.setBackground(selectedIndex == -1 ? getBackground()
					: selectionBackground);
			gc.fillPolygon(shape);
		}

		// draw 1 pixel border
		if (borderLeft > 0) {
			gc.setForeground(borderColor);
			gc.drawPolyline(shape);
		}
		return;
	}

	int x = Math.max(0, borderLeft - 1);
	int y = borderTop;
	int width = size.x - borderLeft - borderRight + 1;
	int height = tabHeight - 1;

	// Draw Tab Header
	int[] left, right;
	if ((getStyle() & SWT.BORDER) != 0) {
		left = simple ? SIMPLE_TOP_LEFT_CORNER : TOP_LEFT_CORNER;
		right = simple ? SIMPLE_TOP_RIGHT_CORNER : TOP_RIGHT_CORNER;
	} else {
		left = simple ? SIMPLE_TOP_LEFT_CORNER_BORDERLESS
				: TOP_LEFT_CORNER_BORDERLESS;
		right = simple ? SIMPLE_TOP_RIGHT_CORNER_BORDERLESS
				: TOP_RIGHT_CORNER_BORDERLESS;
	}
	shape = new int[left.length + right.length + 4];
	int index = 0;
	shape[index++] = x;
	shape[index++] = y + height + highlight_header + 1;
	for (int i = 0; i < left.length / 2; i++) {
		shape[index++] = x + left[2 * i];
		shape[index++] = y + left[2 * i + 1];
	}
	for (int i = 0; i < right.length / 2; i++) {
		shape[index++] = x + width + right[2 * i];
		shape[index++] = y + right[2 * i + 1];
	}
	shape[index++] = x + width;
	shape[index++] = y + height + highlight_header + 1;

	// Fill in background
	boolean bkSelected = single && selectedIndex != -1;
	drawBackground(gc, shape, bkSelected);
	// Fill in parent background for non-rectangular shape
	Region r = new Region();
	r.add(new Rectangle(x, y, width + 1, height + 1));
	r.subtract(shape);
	gc.setBackground(getParent().getBackground());
	fillRegion(gc, r);
	r.dispose();

	// Draw the unselected tabs.
	for (int i = 0; i < items.length; i++) {
		if (i != selectedIndex
				&& event.getBounds().intersects(items[i].getBounds())) {
			items[i].onPaint(gc, false);
		}
	}

	// Draw selected tab
	if (selectedIndex != -1) {
		CTabItem item = items[selectedIndex];
		item.onPaint(gc, true);
	} else {
		// if no selected tab - draw line across bottom of all tabs
		int x1 = borderLeft;
		int y1 = borderTop + tabHeight;
		int x2 = size.x - borderRight;
		gc.setForeground(borderColor);
		gc.drawLine(x1, y1, x2, y1);
	}

	// Draw Buttons
	drawChevron(gc);
	drawMinimize(gc);
	drawMaximize(gc);

	// Draw border line
	if (borderLeft > 0) {
		RGB outside = getParent().getBackground().getRGB();
		antialias(shape, borderColor.getRGB(), null, outside, gc);
		gc.setForeground(borderColor);
		gc.drawPolyline(shape);
	}
}
	
boolean setItemLocation() {
	if(!webbyStyle || onBottom || single) {
		return super.setItemLocation();
	}
	
	boolean changed = false;
	if (items.length == 0) return false;
	int y = borderTop;

	int rightItemEdge = getRightItemEdge();
	int maxWidth = rightItemEdge - borderLeft;
	int width = 0;
	for (int i = 0; i < priority.length; i++) {
		CTabItem item = items[priority[i]];
		width += item.width;
		item.showing = i == 0 ? true : item.width > 0 && width <= maxWidth;
	}
	int x = leftMargin;
	int defaultX = getDisplay().getBounds().width + 10; // off screen
	firstIndex = items.length - 1;
	for (int i = 0; i < items.length; i++) {
		ETabItem item = (ETabItem) items[i];
		if (!item.showing) {
			if (item.x != defaultX) changed = true;
			item.x = defaultX;
		} else {
			firstIndex = Math.min(firstIndex, i);
			if (item.x != x || item.y != y) changed = true;
			item.x = x;
			item.y = y;
			if (i == selectedIndex) {
				int edge = Math.min(item.x + item.width, rightItemEdge);
				item.closeRect.x = edge - CTabItem.RIGHT_MARGIN - BUTTON_SIZE;
				item.y = item.y;
			} else {
				item.closeRect.x = item.x + item.width - CTabItem.RIGHT_MARGIN - BUTTON_SIZE;
				item.y = item.y;
			}
			item.closeRect.y = getTextMidline() - BUTTON_SIZE /2;
			x = x + item.width + hSpace;
		}
	}
	
	return changed;
}

//The space above the selected tab
int getSelectedTabTopOffset() {
	return topMargin;
}
//The space above the unselected tab
int getUnselectedTabTopOffset() {
	return topMargin + selectionMargin;
}


}