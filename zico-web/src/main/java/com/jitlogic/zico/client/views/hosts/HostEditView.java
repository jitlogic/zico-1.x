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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.PopupWindow;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.services.HostServiceProxy;

public class HostEditView {
    interface HostEditViewUiBinder extends UiBinder<Widget, HostEditView> { }

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


    private static final long KB = 1024;
    private static final long MB = 1024*KB;
    private static final long GB = 1024*MB;


    private HostProxy editedHost;
    private HostServiceProxy editHostRequest;
    private HostListPanel panel;
    private ErrorHandler errorHandler;

    private PopupWindow window;

    public HostEditView(ZicoRequestFactory rf, HostListPanel panel, HostProxy info, ErrorHandler errorHandler) {
        super();
        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        this.panel = panel;
        this.errorHandler = errorHandler;

        editHostRequest = rf.hostService();
        if (info != null) {
            editedHost = editHostRequest.edit(info);
        }

        window.resizeAndCenter(290, 210);

        if (info != null) {
            window.setCaption("Edit host: " + info.getName());
            txtHostName.setText(info.getName());
            txtHostName.setEnabled(false);
            txtHostGroup.setText(info.getGroup());
            txtHostAddr.setText(info.getAddr());
            txtHostPass.setText(info.getPass());
            long sz = info.getMaxSize() / GB;
            txtMaxSize.setText(""+sz);
            txtHostDesc.setText(info.getComment());
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

        Request<Void> req;

        if (editedHost == null) {

            String name = txtHostName.getText();
            if (name.length() == 0 || !name.matches("^[0-9a-zA-Z_\\-\\.]+$")) {
                errorHandler.error("Illegal host name: '" + name + "'. Use only those characters: 0-9a-zA-Z_-");
                return;
            }

            req = editHostRequest.newHost(
                    name,
                    txtHostAddr.getText(),
                    txtHostGroup.getText(),
                    txtHostDesc.getText(),
                    txtHostPass.getText(),
                    Long.parseLong(txtMaxSize.getText()) * GB);
        } else {
            editedHost.setAddr(txtHostAddr.getText());
            editedHost.setGroup(txtHostGroup.getText());
            editedHost.setComment(txtHostDesc.getText());
            editedHost.setPass(txtHostPass.getText());
            editedHost.setMaxSize(Long.parseLong(txtMaxSize.getText()) * GB);
            req = editHostRequest.persist(editedHost);
        }

        req.fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                window.hide();
                panel.refresh();
            }
            @Override
            public void onFailure(ServerFailure error) {
                errorHandler.error("Cannot save host settings", error);
            }
        });
    }

}