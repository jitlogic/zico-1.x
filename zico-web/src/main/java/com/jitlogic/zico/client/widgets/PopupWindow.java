/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zico.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class PopupWindow extends PopupPanel {
    interface PopupWindowUiBinder extends UiBinder<DockLayoutPanel, PopupWindow> { }
    private static PopupWindowUiBinder uiBinder = GWT.create(PopupWindowUiBinder.class);

    @UiField
    Label lblCaption;

    @UiField
    Image imgClose;

    DockLayoutPanel panel;

    @UiField(provided=true)
    WidgetResources res;

    boolean isMoving;
    int dx, dy;

    public PopupWindow(Widget content) {
        this(content, WidgetResources.INSTANCE);
    }

    public PopupWindow(Widget content, WidgetResources res) {
        super(false);
        this.res = res;
        panel = uiBinder.createAndBindUi(this);
        panel.add(content);

        //removeStyleName("gwt-PopupPanel");

        sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEMOVE | Event.ONMOUSEUP | Event.ONMOUSEOUT | Event.ONMOUSEOVER);

        add(panel);
    }

    public void resizeAndCenter(int w, int h) {
        setPixelSize(w, h+20);
        setPopupPosition((Window.getClientWidth()-w)/2, (Window.getClientHeight()-h)/2);
    }

    public void setCaption(String caption) {
        lblCaption.setText(caption);
    }

    @UiHandler("imgClose")
    void clickClose(ClickEvent e) {
        hide();
    }

    @Override
    public void onBrowserEvent(Event event) {
        switch (DOM.eventGetType(event)) {
            case Event.ONMOUSEDOWN:
                dx = event.getClientX() - getPopupLeft();
                dy = event.getClientY() - getPopupTop();
                if (dy < 24) {
                    isMoving = true;
                    DOM.setCapture(getElement());
                }
                return;
            case Event.ONMOUSEMOVE:
                if (isMoving) {
                    setPopupPosition(event.getClientX() - dx, event.getClientY() - dy);
                    return;
                }
                break;
            case Event.ONMOUSEUP:
                if (isMoving) {
                    DOM.releaseCapture(getElement());
                    isMoving = false;
                }
                return;
            case Event.ONMOUSEOUT:
            case Event.ONMOUSEOVER:
                if (isMoving) {
                    return;
                }
                break;
        }
        super.onBrowserEvent(event);
    }

}
