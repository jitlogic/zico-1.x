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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class CloseableTab extends Composite {
    interface CloseableTabUiBinder extends UiBinder<Widget, CloseableTab> { }
    private static CloseableTabUiBinder ourUiBinder = GWT.create(CloseableTabUiBinder.class);

    @UiField(provided=true)
    WidgetResources res;

    @UiField
    Label lblCaption;

    @UiField
    Image imgClose;

    private Widget content;
    private TabLayoutPanel panel;

    @UiConstructor
    public CloseableTab(TabLayoutPanel panel, Widget content, String caption) {
        this.content = content;
        this.panel = panel;
        res = WidgetResources.INSTANCE;
        initWidget(ourUiBinder.createAndBindUi(this));
        imgClose.getElement().setAttribute("draggable", "false");
        lblCaption.setText(caption);
    }


    @UiHandler("imgClose")
    void onClickClose(ClickEvent e) {
        panel.remove(content);
    }
}