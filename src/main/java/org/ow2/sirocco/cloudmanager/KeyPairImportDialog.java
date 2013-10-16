/**
 *
 * SIROCCO
 * Copyright (C) 2013 France Telecom
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
package org.ow2.sirocco.cloudmanager;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public final class KeyPairImportDialog extends Window implements Button.ClickListener {
    private static final long serialVersionUID = 1L;

    private final DialogCallback callback;

    private final Button okButton;

    private final Button cancelButton;

    private TextField nameField;

    private TextArea publicKeyField;

    public KeyPairImportDialog(final DialogCallback callback) {
        super("Import Key Pair");
        this.callback = callback;
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setMargin(true);
        content.setSpacing(true);
        content.setWidth("500px");
        content.setHeight("250px");

        FormLayout form = new FormLayout();
        form.setWidth("100%");
        form.setMargin(true);
        form.setSpacing(true);

        this.nameField = new TextField("Name");
        this.nameField.setWidth("50%");
        this.nameField.setRequired(true);
        this.nameField.setRequiredError("Please enter a name for your key pair");
        form.addComponent(this.nameField);

        this.publicKeyField = new TextArea("Public Key");
        this.publicKeyField.setWidth("100%");
        this.publicKeyField.setRequired(true);
        this.publicKeyField.setRequiredError("Please enter a name for your key pair");
        form.addComponent(this.publicKeyField);

        content.addComponent(form);

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
            if (!this.nameField.getValue().isEmpty() && !this.publicKeyField.getValue().isEmpty()) {
                this.close();
                this.callback.response(this.nameField.getValue(), this.publicKeyField.getValue());
            }
        } else {
            this.close();
        }
    }

    public interface DialogCallback {
        void response(String name, String publicKey);
    }
}
