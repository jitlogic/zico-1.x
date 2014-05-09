package com.jitlogic.zico.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public class ToolButton extends Widget {

    private Element imgElement;

    public static interface Css extends CssResource {
        String btn();
        String down();
        String disabled();
    }

    public static interface Resources extends ClientBundle {
        @Source("ToolButton.css")
        Css css();
    }

    public static final Resources RESOURCES = GWT.create(Resources.class);

    static {
        RESOURCES.css().ensureInjected();
    }

    private boolean toggleMode;
    private boolean toggled;

    private boolean enabled = true;
    private ImageResource upIcon, downIcon;
    private Scheduler.ScheduledCommand command;



    public ToolButton(ImageResource upIcon) {
        this(upIcon, null);
    }


    public ToolButton(ImageResource upIcon, Scheduler.ScheduledCommand command) {
        this(upIcon, null, command);
    }


    public ToolButton(ImageResource upIcon, ImageResource downIcon, Scheduler.ScheduledCommand command) {

        this.upIcon = upIcon;
        this.downIcon = downIcon;
        this.command = command;

        sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.FOCUSEVENTS | Event.KEYEVENTS);

        init();

    }

    private void init() {
        Element div = DOM.createDiv();
        imgElement = DOM.createImg();
        if (upIcon != null) {
            imgElement.setAttribute("src", upIcon.getURL());
        }
        div.appendChild(imgElement);

        div.addClassName(RESOURCES.css().btn());

        setElement(div);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        String css = RESOURCES.css().disabled();
        if (enabled) {
            getElement().removeClassName(css);
        } else {
            getElement().addClassName(css);
        }
    }

    public boolean isToggleMode() {
        return toggleMode;
    }

    public void setToggleMode(boolean toggleMode) {
        this.toggleMode = toggleMode;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        String css = RESOURCES.css().down();
        if (toggled) {
            getElement().removeClassName(css);
        } else {
            getElement().addClassName(css);
        }
    }

    public void onBrowserEvent(Event event) {
        switch (DOM.eventGetType(event)) {
            case Event.ONCLICK:
                if (enabled && command != null) {
                    command.execute();
                }
                if (toggleMode && enabled) {
                    setToggled(!isToggled());
                }
        }
        super.onBrowserEvent(event);
    }
}
