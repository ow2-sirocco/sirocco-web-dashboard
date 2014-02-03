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

import java.util.List;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Window;

public final class AddressAssociateDialog extends Window implements Button.ClickListener {
    private static final long serialVersionUID = 1L;

    private final DialogCallback callback;

    private final Button okButton;

    private final Button cancelButton;

    private ComboBox machineBox;

    public static class MachineChoice {
        public String id;

        public String name;
    }

    public AddressAssociateDialog(final List<MachineChoice> choices, final DialogCallback callback) {
        super("Associate Address");
        this.callback = callback;
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        FormLayout content = new FormLayout();
        content.setMargin(true);
        content.setWidth("400px");
        content.setHeight("150px");

        this.machineBox = new ComboBox("Machine");
        this.machineBox.setRequired(true);
        this.machineBox.setTextInputAllowed(false);
        this.machineBox.setNullSelectionAllowed(false);
        this.machineBox.setInputPrompt("select machine");
        this.machineBox.setImmediate(true);
        content.addComponent(this.machineBox);

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

        for (MachineChoice choice : choices) {
            this.machineBox.addItem(choice.id);
            this.machineBox.setItemCaption(choice.id, choice.name);
        }

    }

    public void buttonClick(final ClickEvent event) {
        if (event.getSource() == this.okButton) {
            if (this.machineBox.getValue() != null) {
                this.close();
                this.callback.response((String) this.machineBox.getValue());
            }
        } else {
            this.close();
        }
    }

    public interface DialogCallback {
        void response(String machineId);
    }
}