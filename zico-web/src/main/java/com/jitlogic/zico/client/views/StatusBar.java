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

package com.jitlogic.zico.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;
import com.jitlogic.zico.client.resources.Resources;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class StatusBar extends Composite implements MessageDisplay {
    interface StatusBarUiBinder extends UiBinder<HTMLPanel, StatusBar> { }
    private static StatusBarUiBinder ourUiBinder = GWT.create(StatusBarUiBinder.class);

    public static enum MessageType {
        INFO,
        WARNING,
        ERROR
    };

    interface StatusBarStyle extends CssResource {
        String hidden();
        String text();
        String cmd();
        String bar();
    }

    @UiField
    StatusBarStyle style;

    @UiField
    DivElement lblText;

    @UiField(provided = true)
    Image imgCancel;

    private static class StatusMessage {
        private String source;
        private MessageType type;
        private String text;
        private Scheduler.ScheduledCommand cmdCancel;

        public StatusMessage(String source, MessageType type, String text,
                             Scheduler.ScheduledCommand cmdCancel) {
            this.source = source;
            this.type = type;
            this.text = text;
            this.cmdCancel = cmdCancel;
        }
    }

    private Map<String,StatusMessage> messages = new HashMap<String, StatusMessage>();
    private StatusMessage current;

    @Inject
    public StatusBar() {
        imgCancel = new Image(Resources.INSTANCE.cancel());
        initWidget(ourUiBinder.createAndBindUi(this));
        redisplay();
    }


    private void redisplay() {
        if (current == null && messages.size() > 0) {
            current = messages.values().iterator().next();
        }

        String text = current != null ? current.text : "Ready.";

        lblText.setInnerText(text);

        imgCancel.setVisible(current != null && current.cmdCancel != null);
    }

    @Override
    public void info(String source, String msg) {
        message(source, MessageType.INFO, msg);
    }

    @Override
    public void error(String source, String msg) {
        message(source, MessageType.ERROR, msg);
    }

    @Override
    public void error(String source, String msg, ServerFailure e) {
        message(source, MessageType.ERROR, msg + " " + e.getMessage());
    }

    @Override
    public void message(String source, MessageType type, String msg) {
        StatusMessage m = new StatusMessage(source, type, msg, null);
        messages.put(source, m);
        current = m;
        redisplay();
    }


    @Override
    public void clear(String source) {
        messages.remove(source);
        if (current != null && source.equals(current.source)) {
            current = null;
        }
        redisplay();
    }

}