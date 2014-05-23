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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.MessageDisplay;

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

    @UiField
    DivElement lblText;

    @UiField
    Hyperlink lnkCommand;

    private static class StatusMessage {
        private String source;
        private MessageType type;
        private String text;
        private String cmdText;
        private Scheduler.ScheduledCommand command;

        public StatusMessage(String source, MessageType type, String text,
                             String cmdText, Scheduler.ScheduledCommand command) {
            this.source = source;
            this.type = type;
            this.text = text;
            this.cmdText = cmdText;
            this.command = command;
        }
    }

    private Map<String,StatusMessage> messages = new HashMap<String, StatusMessage>();
    private StatusMessage current;

    @Inject
    public StatusBar() {
        initWidget(ourUiBinder.createAndBindUi(this));
        redisplay();
    }


    private void redisplay() {
        if (current == null && messages.size() > 0) {
            current = messages.values().iterator().next();
        }
        if (current != null) {
            lblText.setInnerText(current.text);
            if (current.command != null) {
                lnkCommand.setText("[" + current.cmdText + "]");
                lnkCommand.setVisible(true);
            }
        } else {
            lblText.setInnerText("Ready.");
            lnkCommand.setVisible(false);
        }
    }


    @Override
    public void info(String source, String msg) {
        message(source, MessageType.INFO, msg);
    }


    @Override
    public void info(String source, String msg, String cmdText, Scheduler.ScheduledCommand cmd) {
        message(source, MessageType.INFO, msg, cmdText, cmd);
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
    public void error(String source, String msg, Throwable e) {
        message(source, MessageType.ERROR, msg + " " + e.getMessage());
    }


    public void message(String source, MessageType type, String msg) {
        message(source, type, msg, null, null);
    }


    public void message(String source, MessageType type, String msg,
                        String cmdText, Scheduler.ScheduledCommand cmdCommand) {
        StatusMessage m = new StatusMessage(source, type, msg, cmdText, cmdCommand);
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


    @UiHandler("lnkCommand")
    void onCmdClick(ClickEvent e) {
        if (current != null && current.command != null) {
            current.command.execute();
        }
    }

}