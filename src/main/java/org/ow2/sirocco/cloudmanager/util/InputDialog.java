/**
 *
 * SIROCCO
 * Copyright (C) 2013 Orange
 * Contact: sirocco@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 */
package org.ow2.sirocco.cloudmanager.util;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public final class InputDialog extends Window implements Button.ClickListener {
    private static final long serialVersionUID = 1L;

    private final DialogCallback callback;

    private final Button okButton;

    private final Button cancelButton;

    private AbstractTextField textField;

    public static InputDialog newInputDialog(final String title, final String name, final String initialValue,
        final DialogCallback callback) {
        return new InputDialog(title, name, initialValue, false, callback);
    }

    public static InputDialog newPasswordDialog(final String title, final String name, final String initialValue,
        final DialogCallback callback) {
        return new InputDialog(title, name, initialValue, true, callback);
    }

    private InputDialog(final String title, final String name, final String initialValue, final boolean isPassword,
        final DialogCallback callback) {
        super(title);
        this.callback = callback;
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing(true);
        verticalLayout.setMargin(true);

        FormLayout content = new FormLayout();
        content.setMargin(true);
        content.setWidth("400px");
        content.setHeight("100px");

        this.textField = isPassword ? new PasswordField(name) : new TextField(name);
        this.textField.setRequired(true);
        this.textField.setWidth("100%");
        this.textField.setRequired(true);
        this.textField.setRequiredError("Please provide a " + name);
        this.textField.setImmediate(true);
        if (initialValue != null) {
            this.textField.setValue(initialValue);
        }
        content.addComponent(this.textField);

        verticalLayout.addComponent(content);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth("100%");
        Label spacer = new Label("");
        buttonLayout.addComponent(spacer);
        spacer.setWidth("100%");
        buttonLayout.setExpandRatio(spacer, 1f);
        this.okButton = new Button("Ok", this);
        this.okButton.setClickShortcut(KeyCode.ENTER, null);
        this.cancelButton = new Button("Cancel", this);
        this.cancelButton.setClickShortcut(KeyCode.ESCAPE, null);
        this.cancelButton.focus();
        buttonLayout.addComponent(this.okButton);
        buttonLayout.addComponent(this.cancelButton);
        content.addComponent(buttonLayout);
        // content.setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);

        verticalLayout.addComponent(buttonLayout);

        this.setContent(verticalLayout);
    }

    public void buttonClick(final ClickEvent event) {
        if (event.getSource() == this.okButton) {
            if (!this.textField.getValue().isEmpty()) {
                this.close();
                this.callback.response(this.textField.getValue());
            }
        } else {
            this.close();
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.textField.focus();
    }

    public interface DialogCallback {
        void response(String value);
    }
}