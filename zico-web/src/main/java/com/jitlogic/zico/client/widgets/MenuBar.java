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
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class MenuBar extends Widget implements Menu {


    public static interface Css extends CssResource {
        String menu();
        String item();
        String disabled();
        String separator();
    }


    public static interface Resources extends ClientBundle {
        @Source("MenuBar.css")
        Css css();

        @Source("empty16x16.png")
        ImageResource empty();
    }


    public static final Resources RESOURCES = GWT.create(Resources.class);


    static {
        RESOURCES.css().ensureInjected();
    }


    private List<MenuItem> menuItems = new ArrayList<MenuItem>();
    private PopupPanel popup;
    private Element body;


    public MenuBar(PopupPanel popup) {
        this(RESOURCES, popup);
    }


    public MenuBar(Resources resources, PopupPanel popup) {
        super();
        sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONFOCUS | Event.ONKEYDOWN);
        this.popup = popup;
        body = DOM.createDiv();
        body.addClassName(resources.css().menu());
        this.setElement(body);
    }


    private MenuItem find(Element e) {
        for (MenuItem i : menuItems) {
            if (DOM.isOrHasChild(i.getElement(), e)) {
                return i;
            }
        }
        return null;
    }


    public void addItem(MenuItem item) {
        menuItems.add(item);
        body.appendChild(item.getElement());
    }


    public void addSeparator() {
        Element div = DOM.createDiv();
        Element span = DOM.createSpan();
        span.addClassName(RESOURCES.css().separator());

        div.appendChild(span);
        body.appendChild(div);
    }


    @Override
    public void onBrowserEvent(Event event) {
        MenuItem item = find(DOM.eventGetTarget(event));

        switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
            if (item != null && item.getCommand() != null && item.isEnabled()) {
                item.getCommand().execute();
                popup.hide();
            }
            break;
        }
        super.onBrowserEvent(event);
    }

}
