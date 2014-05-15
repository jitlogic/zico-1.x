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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.IsPopupWindow;
import com.jitlogic.zico.client.widgets.PopupWindow;
import com.jitlogic.zico.shared.data.UserProxy;
import com.jitlogic.zico.shared.services.UserServiceProxy;

import java.util.*;

public class UserPrefsView implements IsPopupWindow {
    interface UserPrefsViewUiBinder extends UiBinder<Widget, UserPrefsView> { }
    private static UserPrefsViewUiBinder ourUiBinder = GWT.create(UserPrefsViewUiBinder.class);

    @UiField
    TextBox txtUsername;

    @UiField
    TextBox txtRealName;

    @UiField
    CheckBox chkIsAdmin;

    @UiField
    VerticalPanel hostList;


    private UserServiceProxy editUserRequest;
    private UserProxy editedUser;
    public UserManagementPanel panel;

    private List<String> availableHosts;

    private Map<String,CheckBox> selectedHosts = new HashMap<String, CheckBox>();

    private PopupWindow window;

    private MessageDisplay md;

    public UserPrefsView(ZicoRequestFactory rf, UserProxy user, UserManagementPanel panel,
                         List<String> availableHosts, MessageDisplay md) {
        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        editUserRequest = rf.userService();
        this.editedUser = user != null ? editUserRequest.edit(user) : editUserRequest.create(UserProxy.class);
        this.panel = panel;
        this.md = md;

        this.availableHosts = availableHosts;

        window.setCaption(user != null ? "Edit user: " + user.getUserName() : "New user");

        if (user != null) {
            txtUsername.setText(user.getUserName());
            txtUsername.setEnabled(false);
            txtRealName.setText(user.getRealName());
            chkIsAdmin.setValue(user.isAdmin());
        }

        Set<String> hosts = new HashSet<String>();

        if (user != null && user.getAllowedHosts() != null) {
            hosts.addAll(user.getAllowedHosts());
        }

        for (String host : availableHosts) {
            CheckBox chkHost = new CheckBox();
            chkHost.setValue(hosts.contains(host));
            selectedHosts.put(host, chkHost);
            HorizontalPanel hp = new HorizontalPanel();
            hp.add(chkHost);
            hp.add(new Label(host));
            hostList.add(hp);
        }

        window.resizeAndCenter(400, 500);
    }

    @UiHandler("btnOk")
    void clickOk(ClickEvent e) {
        save();
    }

    @UiHandler("btnCancel")
    void clickCancel(ClickEvent e) {
        window.hide();
    }

    private static final String MDS = "UserPrefsView";

    private void save() {
        if (editedUser.getUserName() == null) {
            editedUser.setUserName(txtUsername.getText());
        }
        editedUser.setRealName(txtRealName.getText());
        editedUser.setAdmin(chkIsAdmin.getValue());

        List<String> hosts = new ArrayList<String>(selectedHosts.size());

        for (Map.Entry<String, CheckBox> e : selectedHosts.entrySet()) {
            if (e.getValue().getValue()) {
                hosts.add(e.getKey());
            }
        }

        Collections.sort(hosts);
        editedUser.setAllowedHosts(hosts);

        editUserRequest.persist(editedUser).fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void response) {
                window.hide();
                panel.refreshUsers(null);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                md.error(MDS, "Error saving user data", failure);
            }
        });
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }
}