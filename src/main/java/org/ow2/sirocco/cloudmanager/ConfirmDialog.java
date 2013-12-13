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
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public final class ConfirmDialog extends Window implements Button.ClickListener {

    private static final long serialVersionUID = 1L;

    private static final int ONE_HUNDRED_PERCENT = 100;

    private final ConfirmationDialogCallback callback;

    private CheckBox optionBox;

    private final Button okButton;

    private final Button cancelButton;

    public static ConfirmDialog newConfirmDialogWithOption(final String caption, final String question, final String option,
        final ConfirmationDialogCallback callback) {
        return new ConfirmDialog(caption, question, option, "Ok", "Cancel", callback);
    }

    public static ConfirmDialog newConfirmDialog(final String caption, final String question,
        final ConfirmationDialogCallback callback) {
        return new ConfirmDialog(caption, question, null, "Ok", "Cancel", callback);
    }

    private ConfirmDialog(final String caption, final String question, final String option, final String okLabel,
        final String cancelLabel, final ConfirmationDialogCallback callback) {

        super(caption);
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        content.setWidth("400px");
        content.setHeight("150px");

        this.callback = callback;

        if (question != null) {
            Label label = new Label(question);
            content.addComponent(label);
        }
        if (option != null) {
            this.optionBox = new CheckBox(option);
            content.addComponent(this.optionBox);
        }

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        this.okButton = new Button(okLabel, this);
        this.cancelButton = new Button(cancelLabel, this);
        this.cancelButton.focus();
        buttonLayout.addComponent(this.okButton);
        buttonLayout.addComponent(this.cancelButton);
        content.addComponent(buttonLayout);
        content.setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);

        this.setContent(content);

    }

    public void buttonClick(final ClickEvent event) {
        this.close();
        this.callback.response(event.getSource() == this.okButton, this.optionBox != null ? this.optionBox.getValue() : false);
    }

    public interface ConfirmationDialogCallback {

        void response(boolean ok, boolean option);
    }
}