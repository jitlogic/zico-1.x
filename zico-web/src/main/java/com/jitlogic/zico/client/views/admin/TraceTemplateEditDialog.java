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

package com.jitlogic.zico.client.views.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.IsPopupWindow;
import com.jitlogic.zico.client.widgets.PopupWindow;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;
import com.jitlogic.zico.shared.services.SystemServiceProxy;

public class TraceTemplateEditDialog implements IsPopupWindow {
    interface TraceTemplateViewUiBinder extends UiBinder<Widget, TraceTemplateEditDialog> { }
    private static TraceTemplateViewUiBinder ourUiBinder = GWT.create(TraceTemplateViewUiBinder.class);

    @UiField
    TextBox txtOrder;

    @UiField
    TextBox txtCondition;

    @UiField
    TextBox txtTemplate;

    private SystemServiceProxy editTemplateRequest;
    private TraceTemplateProxy editedTemplate;
    private TraceTemplatePanel panel;

    private PopupWindow window;

    private MessageDisplay md;

    public TraceTemplateEditDialog(ZicoRequestFactory rf, TraceTemplatePanel panel, TraceTemplateProxy tti, MessageDisplay md) {

        this.panel = panel;
        this.md = md;

        editTemplateRequest = rf.systemService();
        editedTemplate = tti != null
                ? editTemplateRequest.edit(tti)
                : editTemplateRequest.create(TraceTemplateProxy.class);

        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        window.resizeAndCenter(540, 125);
        window.setCaption(tti != null ? "Editing template" : "New template");

        if (tti != null) {
            txtOrder.setText(""+tti.getOrder());
            txtCondition.setText(tti.getCondition());
            txtTemplate.setText(tti.getTemplate());
        }
    }

    @UiHandler("btnOk")
    void handleOk(ClickEvent e) {
        save();
    }

    @UiHandler("btnCancel")
    void handleCancel(ClickEvent e) {
        window.hide();
    }

    private static final String MDS = "TraceTemplateView";

    private void save() {
        editedTemplate.setOrder(Integer.parseInt(txtOrder.getText()));
        editedTemplate.setCondition(txtCondition.getText());
        editedTemplate.setTemplate(txtTemplate.getText());

        md.info(MDS, "Saving template ...");

        editTemplateRequest.saveTemplate(editedTemplate).fire(new Receiver<Integer>() {
            @Override
            public void onSuccess(Integer response) {
                window.hide();
                panel.refreshTemplates(null);
                md.clear(MDS);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                md.error(MDS, "Error saving trace template", failure);
            }
        });
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }
}