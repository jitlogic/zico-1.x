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

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

public class ConfirmDialog extends PopupWindow {

    private DockLayoutPanel content;
    private Label lblMessage;
    private FlowPanel buttons;

    private WidgetResources.FormCss css = WidgetResources.INSTANCE.formCss();

    public ConfirmDialog(String caption, String msg) {
        content = new DockLayoutPanel(Style.Unit.PX);
        buttons = new FlowPanel();
        lblMessage = new Label(msg);

        content.addStyleName(css.form());
        lblMessage.addStyleName(css.msgbox());
        buttons.addStyleName(css.btnline());

        content.addSouth(buttons, 42);
        content.add(lblMessage);

        resizeAndCenter(300, 110);
        setCaption(caption);
        addContent(content);
    }

    public ConfirmDialog withBtn(String btnText) {
        return withBtn(btnText, null);
    }

    public ConfirmDialog withBtn(String btnText, final ClickHandler clickHandler) {
        Button button = new Button(btnText);
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (clickHandler != null) {
                    clickHandler.onClick(event);
                }
                hide();
            }
        });
        buttons.add(button);
        return this;
    }
}