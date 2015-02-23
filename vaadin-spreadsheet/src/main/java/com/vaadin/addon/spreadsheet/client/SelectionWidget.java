package com.vaadin.addon.spreadsheet.client;

/*
 * #%L
 * Vaadin Spreadsheet
 * %%
 * Copyright (C) 2013 - 2015 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 * 
 * See the file license.html distributed with this software for more
 * information about licensing.
 * 
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */

import java.util.logging.Logger;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.ui.VOverlay;

public class SelectionWidget extends Composite {

    private class SelectionOutlineWidget extends Widget {

        private static final int eventBits = Event.ONMOUSEDOWN
                | Event.ONMOUSEMOVE | Event.ONMOUSEUP | Event.TOUCHEVENTS
                | Event.ONLOSECAPTURE;

        private final DivElement top = Document.get().createDivElement();
        private final DivElement left = Document.get().createDivElement();
        private final DivElement right = Document.get().createDivElement();
        private final DivElement bottom = Document.get().createDivElement();
        private final DivElement corner = Document.get().createDivElement();
        private final DivElement cornerTouchArea = Document.get()
                .createDivElement();
        private final DivElement root = Document.get().createDivElement();

        private int col1;
        private int row1;
        private int col2;
        private int row2;

        private int maxColumn;

        private int minRow;

        private int maxRow;

        private int minColumn;

        public SelectionOutlineWidget() {
            initDOM();
            initListeners();
        }

        private void initDOM() {
            root.setClassName("sheet-selection");

            if (touchMode) {
                // makes borders bigger with drag-symbol
                root.addClassName("touch");
            }

            top.setClassName("s-top");
            left.setClassName("s-left");
            right.setClassName("s-right");
            bottom.setClassName("s-bottom");
            corner.setClassName("s-corner");
            cornerTouchArea.setClassName("s-corner-touch");

            if (touchMode) {
                // append a large touch area for the corner, since it's too
                // small otherwise
                right.appendChild(cornerTouchArea);
                cornerTouchArea.appendChild(corner);
            } else {
                right.appendChild(corner);
            }

            top.appendChild(left);
            top.appendChild(right);
            left.appendChild(bottom);
            root.appendChild(top);

            setElement(root);
        }

        private void initListeners() {
            // Widget is not attached in GWT terms, so can't call sinkEvents
            // directly
            Event.sinkEvents(root, eventBits);
            Event.setEventListener(root, new EventListener() {

                @Override
                public void onBrowserEvent(Event event) {
                    final Element target = DOM.eventGetTarget(event);
                    final int type = event.getTypeInt();

                    boolean touchEvent = type == Event.ONTOUCHSTART
                            || type == Event.ONTOUCHEND
                            || type == Event.ONTOUCHMOVE
                            || type == Event.ONTOUCHCANCEL;

                    if (paintMode) {
                        onPaintEvent(event);
                        event.stopPropagation();
                    } else if (type == Event.ONMOUSEDOWN) {
                        if (target.equals(corner)) {
                            onPaintEvent(event);
                            event.stopPropagation();
                        } else if (target.equals(top)) {
                        } else if (target.equals(left)) {
                        } else if (target.equals(right)) {
                        } else if (target.equals(bottom)) {
                        }
                        // TODO dragging the selection
                    } else if (touchEvent) {

                        if (type == Event.ONTOUCHEND
                                || type == Event.ONTOUCHCANCEL) {
                            Event.releaseCapture(root);
                            selectCellsStop(event);
                        } else if (target.equals(corner)
                                || target.equals(cornerTouchArea)) {

                            if (type == Event.ONTOUCHSTART) {
                                storeEventPos(event);
                                Event.setCapture(root);
                            } else {

                                // corners, resize selection
                                selectCells(event);
                            }
                        } else {
                            // handles, fill
                            if (fillMode) {
                                // same as dragging the corner in normal mode
                                onPaintEvent(event);
                            }
                        }
                        event.preventDefault();
                        event.stopPropagation();
                    }
                }

            });
        }

        public void setPosition(int col1, int col2, int row1, int row2) {

            root.removeClassName(SheetWidget.toKey(this.col1, this.row1));
            if (minColumn > 0 && col1 < minColumn) {
                col1 = minColumn;
                setLeftEdgeHidden(true);
            } else {
                setLeftEdgeHidden(false);
            }
            if (minRow > 0 && row1 < minRow) {
                row1 = minRow;
                setTopEdgeHidden(true);
            } else {
                setTopEdgeHidden(false);
            }
            if (maxRow > 0 && row2 > maxRow) {
                row2 = maxRow;
                setBottomEdgeHidden(true);
                setCornerHidden(true); // paint is hidden if right edge is
                                       // hidden
            } else {
                setBottomEdgeHidden(false);
                setCornerHidden(false);
            }
            if (maxColumn > 0 && maxColumn < col2) {
                col2 = maxColumn;
                setRightEdgeHidden(true);
            } else {
                setRightEdgeHidden(false);
            }
            this.col1 = col1;
            this.row1 = row1;
            this.col2 = col2;
            this.row2 = row2;
            if (col1 <= col2 && row1 <= row2) {
                root.addClassName(SheetWidget.toKey(this.col1, this.row1));
                setVisible(true);
                updateWidth();
                updateHeight();
            } else {
                setVisible(false);
            }
        }

        private void updateWidth() {
            int w = handler.getColWidthActual(col1);
            for (int i = col1 + 1; i <= col2; i++) {
                w += handler.getColWidthActual(i);
            }
            setWidth(w);
        }

        private void updateHeight() {
            int[] rowHeightsPX = handler.getRowHeightsPX();
            if (rowHeightsPX != null && rowHeightsPX.length != 0) {
                setHeight(countSum(handler.getRowHeightsPX(), row1, row2 + 1));
            }
        }

        private void setWidth(int width) {
            top.getStyle().setWidth(width + 1, Unit.PX);
            bottom.getStyle().setWidth(width + 1, Unit.PX);
        }

        private void setHeight(float height) {
            left.getStyle().setHeight(height, Unit.PX);
            right.getStyle().setHeight(height, Unit.PX);
        }

        public void setSheetElement(Element element) {
            element.appendChild(root);
            element.appendChild(paint);
        }

        public void setZIndex(int zIndex) {
            getElement().getStyle().setZIndex(zIndex);
        }

        protected void setLeftEdgeHidden(boolean hidden) {
            left.getStyle().setVisibility(
                    hidden ? Visibility.HIDDEN : Visibility.VISIBLE);
        }

        protected void setTopEdgeHidden(boolean hidden) {
            top.getStyle().setVisibility(
                    hidden ? Visibility.HIDDEN : Visibility.VISIBLE);
        }

        protected void setRightEdgeHidden(boolean hidden) {
            right.getStyle().setVisibility(
                    hidden ? Visibility.HIDDEN : Visibility.VISIBLE);
        }

        protected void setBottomEdgeHidden(boolean hidden) {
            bottom.getStyle().setVisibility(
                    hidden ? Visibility.HIDDEN : Visibility.VISIBLE);
        }

        protected void setCornerHidden(boolean hidden) {
            cornerTouchArea.getStyle().setDisplay(
                    hidden ? Display.NONE : Display.BLOCK);
            corner.getStyle().setDisplay(hidden ? Display.NONE : Display.BLOCK);
        }

        private void onPaintEvent(Event event) {
            switch (DOM.eventGetType(event)) {
            case Event.ONTOUCHSTART:
                if (event.getTouches().length() > 1) {
                    return;
                }
            case Event.ONMOUSEDOWN:
                beginPaintingCells(event);
                break;
            case Event.ONMOUSEUP:
            case Event.ONTOUCHEND:
            case Event.ONTOUCHCANCEL:
                DOM.releaseCapture(getElement());
            case Event.ONLOSECAPTURE:
                stopPaintingCells(event);
                break;
            case Event.ONMOUSEMOVE:
                paintCells(event);
                break;
            case Event.ONTOUCHMOVE:
                paintCells(event);
                // prevent scrolling
                event.preventDefault();
                break;
            default:
                break;
            }
        }

        public void remove() {
            root.removeFromParent();
            Event.sinkEvents(root, (~eventBits));
        }

        public void setLimits(int minRow, int maxRow, int minColumn,
                int maxColumn) {
            this.minRow = minRow;
            this.maxRow = maxRow;
            this.minColumn = minColumn;
            this.maxColumn = maxColumn;
        }
    }

    private final Logger debugConsole = Logger.getLogger("spreadsheet-logger");

    private final SelectionOutlineWidget bottomRight;
    private SelectionOutlineWidget bottomLeft;
    private SelectionOutlineWidget topRight;
    private SelectionOutlineWidget topLeft;

    private int col1;
    private int row1;
    private int col2;
    private int row2;

    private int colEdgeIndex;
    private int rowEdgeIndex;
    private String paintColClassName = "s-paint-empty";
    private String paintRowClassName = "s-paint-empty";
    private int paintedRowIndex;
    private int paintedColIndex;

    private boolean paintMode;
    private boolean touchMode;
    private boolean fillMode;
    private boolean extraInsideSelection = false;

    private final SheetHandler handler;
    private int cornerX;
    private int cornerY;
    private int origX;
    private int origY;

    private SheetWidget sheetWidget;

    private int horizontalSplitPosition;

    private int verticalSplitPosition;

    private final DivElement paint = Document.get().createDivElement();
    private int totalHeight;
    private int totalWidth;
    private String paintPaneClassName = "bottom-right";

    private int tempCol;
    private int tempRow;

    private int selectionStartCol;
    private int selectionStartRow;

    private VOverlay touchActions;

    private boolean dragging;

    private boolean decreaseSelection;

    private boolean increaseSelection;

    public SelectionWidget(SheetHandler actionHandler, SheetWidget sheetWidget) {
        handler = actionHandler;
        this.sheetWidget = sheetWidget;
        touchMode = sheetWidget.isTouchMode();
        bottomRight = new SelectionOutlineWidget();
        initWidget(bottomRight);

        bottomRight.setZIndex(8);
        bottomRight.addStyleName("bottom-right");
        setVisible(false);

        paint.setClassName("s-paint");
        paint.addClassName(paintPaneClassName);

        paint.getStyle().setVisibility(Visibility.HIDDEN);
        paint.getStyle().setWidth(0, Unit.PX);
        paint.getStyle().setHeight(0, Unit.PX);

        Element bottomRightPane = sheetWidget.getBottomRightPane();
        bottomRight.setSheetElement(bottomRightPane);
        bottomRightPane.appendChild(paint);
    }

    public void setHorizontalSplitPosition(int horizontalSplitPosition) {
        this.horizontalSplitPosition = horizontalSplitPosition;
        if (horizontalSplitPosition > 0 && bottomLeft == null) {
            bottomLeft = new SelectionOutlineWidget();
            bottomLeft.setSheetElement(sheetWidget.getBottomLeftPane());
            bottomLeft.setVisible(false);
            bottomLeft.setZIndex(18);
            bottomLeft.addStyleName("bottom-left");
        } else if (horizontalSplitPosition == 0 && bottomLeft != null) {
            bottomLeft.remove();
            bottomLeft = null;
        }
        updateTopLeft();
        updateLimits();
    }

    public void setVerticalSplitPosition(int verticalSplitPosition) {
        this.verticalSplitPosition = verticalSplitPosition;
        if (verticalSplitPosition > 0 && topRight == null) {
            topRight = new SelectionOutlineWidget();
            topRight.setSheetElement(sheetWidget.getTopRightPane());
            topRight.setVisible(false);
            topRight.setZIndex(18);
            topRight.addStyleName("top-right");
        } else if (verticalSplitPosition == 0 && topRight != null) {
            topRight.remove();
            topRight = null;
        }
        updateTopLeft();
        updateLimits();
    }

    private void updateTopLeft() {
        if (verticalSplitPosition > 0 && horizontalSplitPosition > 0
                && topLeft == null) {
            topLeft = new SelectionOutlineWidget();
            topLeft.setSheetElement(sheetWidget.getTopLeftPane());
            topLeft.setVisible(false);
            topLeft.setZIndex(28);
            topLeft.addStyleName("top-left");
        } else if (topLeft != null
                && (verticalSplitPosition == 0 || horizontalSplitPosition == 0)) {
            topLeft.remove();
            topLeft = null;
        }
    }

    private void updateLimits() {
        bottomRight.setLimits(verticalSplitPosition == 0 ? 0
                : verticalSplitPosition + 1, 0,
                horizontalSplitPosition == 0 ? 0 : horizontalSplitPosition + 1,
                0);
        if (bottomLeft != null) {
            bottomLeft.setLimits(verticalSplitPosition == 0 ? 0
                    : verticalSplitPosition + 1, 0, 0, horizontalSplitPosition);
        }
        if (topRight != null) {
            topRight.setLimits(0, verticalSplitPosition,
                    horizontalSplitPosition == 0 ? 0
                            : horizontalSplitPosition + 1, 0);
        }
        if (topLeft != null) {
            topLeft.setLimits(0, verticalSplitPosition, 0,
                    horizontalSplitPosition);
        }
    }

    public int getRow1() {
        return row1;
    }

    public int getRow2() {
        return row2;
    }

    public int getCol1() {
        return col1;
    }

    public int getCol2() {
        return col2;
    }

    public void setPosition(int col1, int col2, int row1, int row2) {
        this.col1 = col1;
        this.row1 = row1;
        this.col2 = col2;
        this.row2 = row2;
        bottomRight.setPosition(col1, col2, row1, row2);
        if (verticalSplitPosition > 0 & horizontalSplitPosition > 0) {
            topLeft.setPosition(col1, col2, row1, row2);
        }
        if (verticalSplitPosition > 0) {
            topRight.setPosition(col1, col2, row1, row2);
        }
        if (horizontalSplitPosition > 0) {
            bottomLeft.setPosition(col1, col2, row1, row2);
        }
        totalHeight = countSum(handler.getRowHeightsPX(), row1, row2 + 1);
        totalWidth = countSum(handler.getColWidths(), col1, col2 + 1);

        if (fillMode) {
            setFillMode(false);
        }

        if (!dragging) {
            showTouchActions();
        }

    }

    private void showTouchActions() {
        if (touchMode) {
            // show touch actions in popup

            if (touchActions != null) {
                // remove old
                touchActions.hide();
            }

            touchActions = new VOverlay(true);
            touchActions.addStyleName("v-spreadsheet-selection-actions");
            final Button b = new Button("fill");
            touchActions.setWidth("50px");
            touchActions.setHeight("25px");
            touchActions.add(b);

            touchActions.setPopupPositionAndShow(new PositionCallback() {

                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {
                    // above top border
                    int top = bottomRight.top.getAbsoluteTop();
                    int left = bottomRight.top.getAbsoluteLeft();
                    int width = bottomRight.top.getClientWidth();

                    top -= offsetHeight + 5;
                    left += (width / 2) - (offsetWidth / 2);

                    Element parent = sheetWidget.getBottomRightPane();
                    int parentTop = parent.getAbsoluteTop();
                    debugConsole.warning(parent.getClassName() + " "
                            + parentTop + " " + top);
                    if (parentTop > top) {
                        // put under instead
                        top = bottomRight.bottom.getAbsoluteBottom() + 5;
                    }

                    touchActions.setPopupPosition(left, top);

                    // TODO check for room
                }
            });
            touchActions.show();

            b.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    setFillMode(true);
                    touchActions.hide();
                }
            });
        }
    }

    @Override
    public void setWidth(String width) {

    }

    @Override
    public void setHeight(String height) {

    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (topLeft != null) {
            topLeft.setVisible(visible);
        }
        if (topRight != null) {
            topRight.setVisible(visible);
        }
        if (bottomLeft != null) {
            bottomLeft.setVisible(visible);
        }
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() || bottomLeft != null
                && bottomLeft.isVisible() || topRight != null
                && topRight.isVisible() || topLeft != null
                && topLeft.isVisible();
    }

    /**
     * 
     * @param sizes
     * @param beginIndex
     *            1-based inclusive
     * @param endIndex
     *            1-based exclusive
     * @return
     */
    public int countSum(int[] sizes, int beginIndex, int endIndex) {
        if (sizes == null || sizes.length < endIndex - 1) {
            return 0;
        }
        int pos = 0;
        for (int i = beginIndex; i < endIndex; i++) {
            pos += sizes[i - 1];
        }
        return pos;
    }

    /**
     * Returns index of the cell that has the left edge closest to the given
     * cursor position. Used for determinating how many rows/columns should be
     * painted when the mouse cursor is dragged somewhere.
     * 
     * @param cellSizes
     *            the sizes used to calculate
     * @param startIndex
     *            1-based index where the cursorPosition refers to
     * @param cursorPosition
     *            the position of the cursor relative to startIndex. Can be
     *            negative
     * @return
     */
    public int closestCellEdgeIndexToCursor(int cellSizes[], int startIndex,
            int cursorPosition) {

        // TODO completely broken when zoomed in
        int pos = 0;
        if (cursorPosition < 0) {
            if (startIndex > 1) {
                while (startIndex > 1 && pos > cursorPosition) {
                    startIndex--;
                    pos -= cellSizes[startIndex - 1];
                }
                return startIndex;
            } else {
                return 1;
            }
        } else {
            if (startIndex < cellSizes.length) {
                while (startIndex <= cellSizes.length && pos < cursorPosition) {
                    pos += cellSizes[startIndex - 1];
                    startIndex++;
                }
                return startIndex;
            } else {
                return cellSizes.length;
            }
        }
    }

    private void beginPaintingCells(Event event) {
        colEdgeIndex = 0;
        rowEdgeIndex = 0;
        sheetWidget.scrollSelectionAreaIntoView();
        paintMode = true;
        storeEventPos(event);
        DOM.setCapture(getElement());
        event.preventDefault();

        sheetWidget.getElement().addClassName("selecting");
    }

    private void storeEventPos(Event event) {
        Element element = getTopLeftMostElement();
        origX = element.getAbsoluteLeft();
        origY = element.getAbsoluteTop();
        cornerX = origX + totalWidth;
        cornerY = origY + totalHeight;

        selectionStartCol = col1;
        selectionStartRow = row1;
    }

    private Element getTopLeftMostElement() {
        if (sheetWidget.isCellRenderedInTopRightPane(col1, row1)) {
            return topRight.getElement();
        }
        if (sheetWidget.isCellRenderedInBottomLeftPane(col1, row1)) {
            return bottomLeft.getElement();
        }
        if (sheetWidget.isCellRenderedInTopLeftPane(col1, row1)) {
            return topLeft.getElement();
        }
        return bottomRight.getElement();
    }

    private void stopPaintingCells(Event event) {
        paint.getStyle().setVisibility(Visibility.HIDDEN);
        paintMode = false;
        paint.getStyle().setWidth(0, Unit.PX);
        paint.getStyle().setHeight(0, Unit.PX);
        paint.getStyle().setVisibility(Visibility.HIDDEN);
        int c1, c2, r1, r2;
        if (decreaseSelection && (colEdgeIndex >= col1 && colEdgeIndex <= col2)
                && (rowEdgeIndex >= row1 && rowEdgeIndex <= row2)) {
            handler.onSelectionDecreasePainted(col1, col2, colEdgeIndex, row1,
                    row2, rowEdgeIndex);
        } else if (increaseSelection
                && (col2 + 1 < colEdgeIndex || colEdgeIndex < col1
                        || row2 + 1 < rowEdgeIndex || rowEdgeIndex < row1)) {
            c1 = colEdgeIndex < col1 ? colEdgeIndex : col1;
            c2 = colEdgeIndex > col2 ? colEdgeIndex - 1 : col2;
            r1 = rowEdgeIndex < row1 ? rowEdgeIndex : row1;
            r2 = rowEdgeIndex > row2 ? rowEdgeIndex - 1 : row2;
            if (c1 > 0 && r1 > 0) {
                handler.onSelectionIncreasePainted(c1, c2, r1, r2);
            }
        }

        sheetWidget.getElement().removeClassName("selecting");

    }

    private void selectCells(Event event) {

        dragging = true;

        final int clientX = WidgetUtil.getTouchOrMouseClientX(event);
        final int clientY = WidgetUtil.getTouchOrMouseClientY(event);
        // position in perspective to the top left
        int xMousePos = clientX - origX;
        int yMousePos = clientY - origY;

        // touch offset; coords are made for mouse movement and need adjustment
        // on touch
        xMousePos -= 70;
        yMousePos -= 20;

        final int[] colWidths = handler.getColWidths();
        final int colIndex = closestCellEdgeIndexToCursor(colWidths,
                selectionStartCol, xMousePos);
        final int[] rowHeightsPX = handler.getRowHeightsPX();
        final int rowIndex = closestCellEdgeIndexToCursor(rowHeightsPX,
                selectionStartRow, yMousePos);

        tempCol = colIndex;
        tempRow = rowIndex;
        sheetWidget.getSheetHandler().onSelectingCellsWithDrag(colIndex,
                rowIndex);
    }

    private void selectCellsStop(Event event) {
        sheetWidget.getSheetHandler().onFinishedSelectingCellsWithDrag(
                sheetWidget.getSelectedCellColumn(), tempCol,
                sheetWidget.getSelectedCellRow(), tempRow);

        dragging = false;
        showTouchActions();
    }

    protected void setFillMode(boolean b) {
        fillMode = b;
        bottomRight.setCornerHidden(b);
        if (b) {
            bottomRight.addStyleName("fill");
        } else {
            bottomRight.removeStyleName("fill");
        }
    }

    private void paintCells(Event event) {
        decreaseSelection = false;
        increaseSelection = false;
        final int clientX = WidgetUtil.getTouchOrMouseClientX(event);
        final int clientY = WidgetUtil.getTouchOrMouseClientY(event);
        // position in perspective to the top left
        int xMousePos = clientX - origX;
        int yMousePos = clientY - origY;

        final int[] colWidths = handler.getColWidths();
        int colIndex = closestCellEdgeIndexToCursor(colWidths, col1, xMousePos);
        final int[] rowHeightsPX = handler.getRowHeightsPX();
        int rowIndex = closestCellEdgeIndexToCursor(rowHeightsPX, row1,
                yMousePos);

        int w = 0;
        int h = 0;

        paint.removeClassName(paintColClassName);
        paint.removeClassName(paintRowClassName);
        // case 1: "removing"
        if ((colIndex >= col1 && colIndex <= col2 + 1)
                && (rowIndex >= row1 && rowIndex <= row2 + 1)) {
            // the depending point is the right bottom corner
            xMousePos = Math.abs(cornerX - clientX);
            yMousePos = Math.abs(cornerY - clientY);
            // the axis with larger delta is used
            if (xMousePos >= yMousePos && (colIndex <= col2)) {
                // remove columns
                MergedRegion paintedRegion = MergedRegionUtil
                        .findIncreasingSelection(
                                handler.getMergedRegionContainer(), row1, row2,
                                colIndex, col2);
                colEdgeIndex = paintedRegion.col1;
                rowEdgeIndex = row1;
                w = countSum(colWidths, colEdgeIndex, col2 + 1);
                h = totalHeight;
                decreaseSelection = true;
            } else if (yMousePos > xMousePos && (rowIndex <= row2)) {
                // remove rows
                MergedRegion paintedRegion = MergedRegionUtil
                        .findIncreasingSelection(
                                handler.getMergedRegionContainer(), rowIndex,
                                row2, col1, col2);
                colEdgeIndex = col1;
                rowEdgeIndex = paintedRegion.row1;
                w = totalWidth;
                h = countSum(rowHeightsPX, rowEdgeIndex, row2 + 1);
                decreaseSelection = true;
            } else {
                h = 0;
                w = 0;
                colEdgeIndex = col2;
                rowEdgeIndex = row2;
            }
            if (!extraInsideSelection) {
                paint.addClassName("s-paint-inside");
                extraInsideSelection = true;
            }
            paintedColIndex = colEdgeIndex;
            paintedRowIndex = rowEdgeIndex;
        } else if ((rowIndex < row1 || rowIndex > row2)
                || (colIndex < col1 || colIndex > col2)) {
            if (extraInsideSelection) {
                paint.removeClassName("s-paint-inside");
                extraInsideSelection = false;
            }
            if (rowIndex > row2) {
                // see diff from old selection bottom
                yMousePos = clientY - cornerY;
            } else if (rowIndex >= row1) {
                yMousePos = 0;
            }
            if (colIndex > col2) {
                // see diff from old selection right
                xMousePos = clientX - cornerX;
            } else if (colIndex >= col1) {
                xMousePos = 0;
            }
            if (Math.abs(colIndex - col2) > Math.abs(rowIndex - row2)) {
                colEdgeIndex = colIndex;
                rowEdgeIndex = row1;
                paintedRowIndex = rowEdgeIndex;
                h = totalHeight;
                // left or right
                if (xMousePos < 0) {
                    w = countSum(colWidths, colEdgeIndex, col1);
                    paintedColIndex = colEdgeIndex;
                } else {
                    w = countSum(colWidths, col2 + 1, colEdgeIndex);
                    paintedColIndex = (col2 + 1);
                }
                increaseSelection = true;
            } else if (Math.abs(colIndex - col2) < Math.abs(rowIndex - row2)) {
                colEdgeIndex = col1;
                rowEdgeIndex = rowIndex;
                paintedColIndex = colEdgeIndex;
                w = totalWidth;
                // up or down
                if (yMousePos < 0) {
                    h = countSum(rowHeightsPX, rowEdgeIndex, row1);
                    paintedRowIndex = rowEdgeIndex;
                } else {
                    h = countSum(rowHeightsPX, row2 + 1, rowEdgeIndex);
                    paintedRowIndex = (row2 + 1);
                }
                increaseSelection = true;
            }
        }
        // update position
        if (paintedColIndex != 0 && paintedRowIndex != 0) {
            paintColClassName = "col" + paintedColIndex;
            paintRowClassName = "row" + paintedRowIndex;
            paint.removeClassName(paintPaneClassName);
            paint.addClassName(paintColClassName);
            paint.addClassName(paintRowClassName);
            paint.removeFromParent();
            if (sheetWidget.isCellRenderedInScrollPane(paintedColIndex,
                    paintedRowIndex)) {
                sheetWidget.getBottomRightPane().appendChild(paint);
                paintPaneClassName = "bottom-right";
            } else if (sheetWidget.isCellRenderedInBottomLeftPane(
                    paintedColIndex, paintedRowIndex)) {
                sheetWidget.getBottomLeftPane().appendChild(paint);
                paintPaneClassName = "bottom-left";
            } else if (sheetWidget.isCellRenderedInTopRightPane(
                    paintedColIndex, paintedRowIndex)) {
                sheetWidget.getTopRightPane().appendChild(paint);
                paintPaneClassName = "top-right";
            } else if (sheetWidget.isCellRenderedInTopLeftPane(paintedColIndex,
                    paintedRowIndex)) {
                sheetWidget.getTopLeftPane().appendChild(paint);
                paintPaneClassName = "top-left";
            }
            paint.addClassName(paintPaneClassName);
        }
        // update size
        if (w > 0 && h > 0) {
            paint.getStyle().setVisibility(Visibility.VISIBLE);
            paint.getStyle().setWidth(w + 1, Unit.PX);
            paint.getStyle().setHeight(h + 1, Unit.PX);
        } else {
            paint.getStyle().setVisibility(Visibility.HIDDEN);
            paint.getStyle().setWidth(0, Unit.PX);
            paint.getStyle().setHeight(0, Unit.PX);
        }
    }
}
