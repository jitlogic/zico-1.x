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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.widgets.IsPopupWindow;
import com.jitlogic.zico.client.widgets.PopupWindow;

import javax.inject.Inject;

public class PasswordChangeView implements IsPopupWindow {

    interface PasswordChangeViewUiBinder extends UiBinder<Widget, PasswordChangeView> { }

    private static PasswordChangeViewUiBinder ourUiBinder = GWT.create(PasswordChangeViewUiBinder.class);

    @UiField
    TextBox txtOldPassword;

    @UiField
    TextBox txtNewPassword;

    @UiField
    TextBox txtRepPassword;

    private ZicoRequestFactory rf;
    private String username;

    private PopupWindow window;

    @Inject
    public PasswordChangeView(ZicoRequestFactory rf, @Assisted("userName") String username) {

        this.rf = rf;
        this.username = username;

        window = new PopupWindow(ourUiBinder.createAndBindUi(this));
        window.setCaption("Change password");
        window.resizeAndCenter(400, 250);
    }

    @UiHandler("btnOk")
    void clickOk(ClickEvent e) {
        doPasswordChange();
    }

    @UiHandler("btnCancel")
    void clickCancel(ClickEvent e) {
        window.hide();
    }

    private void doPasswordChange() {
        String oldPassword = username == null ? txtOldPassword.getText() : null;
        String newPassword = txtNewPassword.getText();
        String repPassword = txtRepPassword.getText();

        if (username == null && (oldPassword == null || oldPassword.length() == 0)) {
            Window.alert("You have to enter old password.");
            return;
        }

        if (newPassword == null || newPassword.length() == 0 || repPassword == null || repPassword.length() == 0) {
            Window.alert("New password is empty.");
            return;
        }

        if (!newPassword.equals(repPassword)) {
            txtNewPassword.setText("");
            txtRepPassword.setText("");
            Window.alert("New passwords do not match.");
            return;
        }

        rf.userService().resetPassword(username, oldPassword, newPassword).fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (username == null) txtOldPassword.setText("");
                txtNewPassword.setText("");
                txtRepPassword.setText("");

                Window.alert("Password has been changed.");
                window.hide();
            }

            public void onFailure(ServerFailure e) {
                if (username == null) txtOldPassword.setText("");
                txtNewPassword.setText("");
                txtRepPassword.setText("");
                Window.alert("Password change failed: " + e.getMessage());
            }
        });
    }

    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }

}