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
import com.jitlogic.zico.client.api.TraceTemplateService;
import com.jitlogic.zico.shared.data.TraceTemplateInfo;
import com.jitlogic.zico.widgets.client.IsPopupWindow;
import com.jitlogic.zico.widgets.client.MessageDisplay;
import com.jitlogic.zico.widgets.client.PopupWindow;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

public class TraceTemplateEditDialog implements IsPopupWindow {
    interface TraceTemplateViewUiBinder extends UiBinder<Widget, TraceTemplateEditDialog> { }
    private static TraceTemplateViewUiBinder ourUiBinder = GWT.create(TraceTemplateViewUiBinder.class);

    @UiField
    TextBox txtOrder;

    @UiField
    TextBox txtCondition;

    @UiField
    TextBox txtTemplate;

    private TraceTemplateInfo editedTemplate;
    private boolean newTemplate;

    private TraceTemplateService templateService;
    private TraceTemplatePanel panel;

    private PopupWindow window;

    private MessageDisplay md;

    public TraceTemplateEditDialog(TraceTemplateService templateService, TraceTemplatePanel panel,
                                   TraceTemplateInfo tti, MessageDisplay md) {
        this.templateService = templateService;
        this.panel = panel;
        this.editedTemplate = tti != null ? tti : new TraceTemplateInfo();
        this.newTemplate = tti == null;
        this.md = md;

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

        MethodCallback<Void> cb = new MethodCallback<Void>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error saving trace template", e);
            }

            @Override
            public void onSuccess(Method method, Void response) {
                window.hide();
                panel.refreshTemplates(null);
                md.clear(MDS);
            }
        };

        if (newTemplate) {
            templateService.create(editedTemplate, cb);
        } else {
            templateService.update(editedTemplate.getId(), editedTemplate, cb);
        }
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }
}