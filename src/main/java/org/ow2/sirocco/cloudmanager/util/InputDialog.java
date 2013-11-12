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

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public final class InputDialog extends Window implements Button.ClickListener {
    private static final long serialVersionUID = 1L;

    private final DialogCallback callback;

    private final Button okButton;

    private final Button cancelButton;

    private TextField textField;

    public InputDialog(final String title, final String name, final String initialValue, final DialogCallback callback) {
        super(title);
        this.callback = callback;
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        FormLayout content = new FormLayout();
        content.setMargin(true);
        content.setWidth("400px");
        content.setHeight("150px");

        this.textField = new TextField(name);
        this.textField.setRequired(true);
        this.textField.setWidth("80%");
        this.textField.setRequired(true);
        this.textField.setRequiredError("Please provide a " + name);
        this.textField.setImmediate(true);
        if (initialValue != null) {
            this.textField.setValue(initialValue);
        }
        content.addComponent(this.textField);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        this.okButton = new Button("Ok", this);
        this.cancelButton = new Button("Cancel", this);
        this.cancelButton.focus();
        buttonLayout.addComponent(this.okButton);
        buttonLayout.addComponent(this.cancelButton);
        content.addComponent(buttonLayout);
        content.setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);

        this.setContent(content);
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