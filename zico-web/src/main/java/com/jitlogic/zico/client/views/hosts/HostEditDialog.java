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

package com.jitlogic.zico.client.views.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.jitlogic.zico.client.api.HostService;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.widgets.PopupWindow;
import com.jitlogic.zico.client.widgets.WidgetResources;
import com.jitlogic.zico.shared.data.HostInfo;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;

public class HostEditDialog {
    interface HostEditViewUiBinder extends UiBinder<Widget, HostEditDialog> { }
    private static HostEditViewUiBinder ourUiBinder = GWT.create(HostEditViewUiBinder.class);

    @UiField
    TextBox txtHostName;

    @UiField
    TextBox txtHostAddr;

    @UiField
    TextBox txtHostGroup;

    @UiField
    TextBox txtHostDesc;

    @UiField
    TextBox txtHostPass;

    @UiField
    TextBox txtMaxSize;

    @UiField(provided = true)
    WidgetResources wres;

    private String SRC = "HostEdit";

    private static final long KB = 1024;
    private static final long MB = 1024*KB;
    private static final long GB = 1024*MB;

    private HostInfo editedHost;
    private boolean newHost;

    private HostListPanel panel;

    private PopupWindow window;
    private MessageDisplay messageDisplay;

    private HostService hostService;

    @Inject
    public HostEditDialog(HostService hostService, HostListPanel panel, HostInfo host, MessageDisplay messageDisplay) {
        super();
        wres = WidgetResources.INSTANCE;
        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        this.panel = panel;
        this.hostService = hostService;
        this.editedHost = host != null ? host : new HostInfo();
        this.newHost = host == null;
        this.messageDisplay = messageDisplay;


        window.resizeAndCenter(290, 210);

        if (host != null) {
            window.setCaption("Edit host: " + host.getName());
            txtHostName.setText(host.getName());
            txtHostName.setEnabled(false);
            txtHostGroup.setText(host.getGroup());
            txtHostAddr.setText(host.getAddr());
            txtHostPass.setText(host.getPass());
            long sz = host.getMaxSize() / GB;
            txtMaxSize.setText(""+sz);
            txtHostDesc.setText(host.getComment());
        } else {
            window.setCaption("New host");
            txtMaxSize.setText("1");
        }
    }


    public PopupWindow getWindow() {
        return window;
    }


    @UiHandler("btnOk")
    void handleOk(ClickEvent e) {
        save();
    }


    @UiHandler("btnCancel")
    void handleCancel(ClickEvent e) {
        window.hide();
    }


    public void save() {
        String name = txtHostName.getText();
        if (name.length() == 0 || !name.matches("^[0-9a-zA-Z_\\-\\.]+$")) {
            messageDisplay.error(SRC, "Illegal host name: '" + name + "'. Use only those characters: 0-9a-zA-Z_-");
            return;
        }

        editedHost.setAddr(txtHostAddr.getText());
        editedHost.setGroup(txtHostGroup.getText());
        editedHost.setComment(txtHostDesc.getText());
        editedHost.setPass(txtHostPass.getText());
        editedHost.setMaxSize(Long.parseLong(txtMaxSize.getText()) * GB);

        MethodCallback<Void> cb = new MethodCallback<Void>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                messageDisplay.error(SRC, "Cannot save host settings", e);
            }

            @Override
            public void onSuccess(Method method, Void response) {
                window.hide();
                messageDisplay.clear(SRC);
                panel.refresh(null);
            }
        };

        if (newHost) {
            hostService.create(editedHost, cb);
        } else {
            hostService.update(editedHost.getName(), editedHost, cb);
        }
   }

}