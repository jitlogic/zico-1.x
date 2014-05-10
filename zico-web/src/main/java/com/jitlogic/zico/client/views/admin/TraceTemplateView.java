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
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.IsPopupWindow;
import com.jitlogic.zico.client.widgets.PopupWindow;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;
import com.jitlogic.zico.shared.services.SystemServiceProxy;

public class TraceTemplateView implements IsPopupWindow {
    interface TraceTemplateViewUiBinder extends UiBinder<Widget, TraceTemplateView> { }
    private static TraceTemplateViewUiBinder ourUiBinder = GWT.create(TraceTemplateViewUiBinder.class);

    @UiField
    TextBox txtOrder;

    @UiField
    TextBox txtCondition;

    @UiField
    TextBox txtTemplate;

    private SystemServiceProxy editTemplateRequest;
    private TraceTemplateProxy editedTemplate;
    private ErrorHandler errorHandler;
    private TraceTemplatePanel panel;

    private PopupWindow window;

    public TraceTemplateView(ZicoRequestFactory rf, TraceTemplatePanel panel, TraceTemplateProxy tti, ErrorHandler errorHandler) {

        this.panel = panel;
        this.errorHandler = errorHandler;

        editTemplateRequest = rf.systemService();
        editedTemplate = tti != null
                ? editTemplateRequest.edit(tti)
                : editTemplateRequest.create(TraceTemplateProxy.class);

        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        window.setCaption(tti != null ? "Editing template" : "New template");
    }

    @UiHandler("btnOk")
    void handleOk(ClickEvent e) {
        save();
    }

    @UiHandler("btnCancel")
    void handleCancel(ClickEvent e) {
        window.hide();
    }


    private void save() {
        editedTemplate.setOrder(Integer.parseInt(txtOrder.getText()));
        editedTemplate.setCondition(txtCondition.getText());
        editedTemplate.setTemplate(txtTemplate.getText());

        editTemplateRequest.saveTemplate(editedTemplate).fire(new Receiver<Integer>() {
            @Override
            public void onSuccess(Integer response) {
                window.hide();
                panel.refreshTemplates();
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error saving trace template", failure);
            }
        });
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }
}