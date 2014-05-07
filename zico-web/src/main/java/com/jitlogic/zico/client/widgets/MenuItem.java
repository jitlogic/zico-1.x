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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;

public class MenuItem extends UIObject {

    private boolean enabled = true;

    private MenuBar.Resources resources;

    private String text;
    private ImageResource icon;
    private Scheduler.ScheduledCommand command;

    private Element item;


    public MenuItem(String text, Scheduler.ScheduledCommand command) {
        this(text, null, command);
    }


    public MenuItem(String text, ImageResource icon, Scheduler.ScheduledCommand command) {
        this.text = text;
        this.icon = icon;
        this.command = command;
        resources = MenuBar.RESOURCES;


        init();
        setElement(item);
    }


    private void init() {
        item = DOM.createDiv();
        item.addClassName(resources.css().item());

        if (!enabled) {
            item.addClassName(resources.css().disabled());
        }

        Element a = DOM.createAnchor();

        Element img = DOM.createImg();
        img.setAttribute("src", icon != null ? icon.getURL() : resources.empty().getURL());
        a.appendChild(img);

        Element span = DOM.createSpan();
        span.setInnerText(text);
        a.appendChild(span);

        item.appendChild(a);
    }


    public Scheduler.ScheduledCommand getCommand() {
        return command;
    }


    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            item.removeClassName(resources.css().disabled());
        } else {
            item.addClassName(resources.css().disabled());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

}
