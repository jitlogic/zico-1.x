package com.jitlogic.zico.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public class ToolButton extends Widget implements HasClickHandlers {

    private Element imgElement;

    private boolean toggleMode;
    private boolean toggled;

    private boolean enabled = true;
    private ImageResource upIcon, downIcon;


    @UiConstructor
    public ToolButton(ImageResource upIcon) {
        this(upIcon, null);
    }


    public ToolButton(ImageResource upIcon, ImageResource downIcon) {

        this.upIcon = upIcon;
        this.downIcon = downIcon;

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

        div.addClassName(WidgetResources.INSTANCE.toolBarCss().button());
        imgElement.setAttribute("draggable", "false");

        setElement(div);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        String css = WidgetResources.INSTANCE.toolBarCss().buttonDisabled();
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
        String css = WidgetResources.INSTANCE.toolBarCss().buttonDown();
        if (toggled) {
            getElement().removeClassName(css);
        } else {
            getElement().addClassName(css);
        }
    }


    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addHandler(handler, ClickEvent.getType());
    }


    @Override
    public void onBrowserEvent(Event event) {
        switch (DOM.eventGetType(event)) {
            case Event.ONCLICK:
                if (!enabled) {
                    event.stopPropagation();
                    return;
                }
                if (toggleMode && enabled) {
                    setToggled(!isToggled());
                }
                break;
        }
        super.onBrowserEvent(event);
    }
}
