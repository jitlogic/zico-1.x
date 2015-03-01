/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zico.client.api.UserService;
import com.jitlogic.zico.shared.data.UserInfo;
import com.jitlogic.zico.widgets.client.IsPopupWindow;
import com.jitlogic.zico.widgets.client.MessageDisplay;
import com.jitlogic.zico.widgets.client.PopupWindow;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.*;

public class UserEditDialog implements IsPopupWindow {
    interface UserPrefsViewUiBinder extends UiBinder<Widget, UserEditDialog> { }
    private static UserPrefsViewUiBinder ourUiBinder = GWT.create(UserPrefsViewUiBinder.class);

    @UiField
    TextBox txtUsername;

    @UiField
    TextBox txtRealName;

    @UiField
    CheckBox chkIsAdmin;

    @UiField
    VerticalPanel hostList;

    private UserService userService;
    private UserInfo editedUser;
    private boolean newUser;
    public UserManagementPanel panel;

    private Map<String,CheckBox> selectedHosts = new HashMap<String, CheckBox>();

    private PopupWindow window;

    private MessageDisplay md;

    public UserEditDialog(UserService userService, UserInfo user, UserManagementPanel panel,
                          List<String> availableHosts, MessageDisplay md) {
        window = new PopupWindow(ourUiBinder.createAndBindUi(this));

        this.userService = userService;
        this.editedUser = user != null ? user : new UserInfo();
        this.newUser = user == null;
        this.panel = panel;
        this.md = md;

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

        window.resizeAndCenter(250, 350);
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

        MethodCallback<Void> cb = new MethodCallback<Void>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error saving user data", e);
            }

            @Override
            public void onSuccess(Method method, Void response) {
                window.hide();
                panel.refreshUsers(null);
            }
        };

        if (newUser) {
            editedUser.setPassword("changeme");
            userService.create(editedUser, cb);
        } else {
            userService.update(editedUser.getUserName(), editedUser, cb);
        }
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }
}