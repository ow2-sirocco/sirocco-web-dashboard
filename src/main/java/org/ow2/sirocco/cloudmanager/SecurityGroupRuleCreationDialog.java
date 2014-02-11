/**
 *
 * SIROCCO
 * Copyright (C) 2014 France Telecom
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

import org.apache.commons.net.util.SubnetUtils;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroupRuleParams;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public final class SecurityGroupRuleCreationDialog extends Window implements Button.ClickListener {
    private static final long serialVersionUID = 1L;

    private final DialogCallback callback;

    private final Button okButton;

    private final Button cancelButton;

    private Label errorLabel;

    private ComboBox protocolBox;

    private ComboBox sourceSecGroupBox;

    private TextField portField;

    private OptionGroup sourceChoice;

    private TextField sourceIpRangeField;

    public static class SecGroupChoice {
        public String id;

        public String name;
    }

    public SecurityGroupRuleCreationDialog(final List<SecGroupChoice> choices, final DialogCallback callback) {
        super("Add Rule");
        this.callback = callback;
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        FormLayout content = new FormLayout();
        content.setMargin(true);
        content.setWidth("500px");
        content.setHeight("350px");

        this.protocolBox = new ComboBox("IP Protocol");
        this.protocolBox.setRequired(true);
        this.protocolBox.setTextInputAllowed(false);
        this.protocolBox.setNullSelectionAllowed(false);
        this.protocolBox.setImmediate(true);
        this.protocolBox.addItem("TCP");
        this.protocolBox.addItem("UDP");
        this.protocolBox.addItem("ICMP");
        this.protocolBox.setValue("TCP");
        content.addComponent(this.protocolBox);

        this.portField = new TextField("Port range");
        this.portField.setRequired(true);
        this.portField.setWidth("80%");
        this.portField.setRequired(true);
        this.portField.setRequiredError("Please provide a port range");
        this.portField.setImmediate(true);
        content.addComponent(this.portField);

        this.sourceChoice = new OptionGroup("Source");
        this.sourceChoice.addItem("CIDR");
        this.sourceChoice.addItem("Security Group");
        this.sourceChoice.setValue("CIDR");
        this.sourceChoice.setImmediate(true);
        this.sourceChoice.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(final ValueChangeEvent event) {
                boolean sourceIsCidr = SecurityGroupRuleCreationDialog.this.sourceChoice.getValue().equals("CIDR");
                SecurityGroupRuleCreationDialog.this.sourceIpRangeField.setEnabled(sourceIsCidr);
                SecurityGroupRuleCreationDialog.this.sourceSecGroupBox.setEnabled(!sourceIsCidr);
            }
        });
        content.addComponent(this.sourceChoice);

        this.sourceIpRangeField = new TextField("CIDR");
        this.sourceIpRangeField.setRequired(true);
        this.sourceIpRangeField.setWidth("80%");
        this.sourceIpRangeField.setRequired(true);
        this.sourceIpRangeField.setRequiredError("Please provide a CIDR");
        this.sourceIpRangeField.setImmediate(true);
        this.sourceIpRangeField.setValue("0.0.0.0/0");
        content.addComponent(this.sourceIpRangeField);

        this.sourceSecGroupBox = new ComboBox("Security Group");
        this.sourceSecGroupBox.setRequired(true);
        this.sourceSecGroupBox.setTextInputAllowed(false);
        this.sourceSecGroupBox.setNullSelectionAllowed(false);
        this.sourceSecGroupBox.setInputPrompt("select machine");
        this.sourceSecGroupBox.setImmediate(true);
        for (SecGroupChoice choice : choices) {
            this.sourceSecGroupBox.addItem(choice.id);
            this.sourceSecGroupBox.setItemCaption(choice.id, choice.name);
        }
        this.sourceSecGroupBox.setValue(choices.get(0).id);
        this.sourceSecGroupBox.setEnabled(false);
        content.addComponent(this.sourceSecGroupBox);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSizeFull();
        buttonLayout.setSpacing(true);

        this.errorLabel = new Label("                                       ");
        this.errorLabel.setStyleName("errorMsg");
        this.errorLabel.setWidth("100%");
        buttonLayout.addComponent(this.errorLabel);
        buttonLayout.setExpandRatio(this.errorLabel, 1.0f);

        this.okButton = new Button("Ok", this);
        this.cancelButton = new Button("Cancel", this);
        this.cancelButton.focus();
        buttonLayout.addComponent(this.okButton);
        buttonLayout.addComponent(this.cancelButton);
        content.addComponent(buttonLayout);
        // content.setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);

        this.setContent(content);

    }

    private boolean fillAndValidate(final SecurityGroupRuleParams ruleParams) {
        ruleParams.setIpProtocol((String) this.protocolBox.getValue());
        if (this.portField.getValue().isEmpty()) {
            this.errorMessage("Missing port");
            return false;
        }
        try {
            if (this.portField.getValue().contains("-")) {
                String[] range = this.portField.getValue().split("-");
                if (range.length != 2) {
                    this.errorMessage("Invalid port range");
                    return false;
                }
                int fromPort = Integer.parseInt(range[0]);
                int toPort = Integer.parseInt(range[1]);
                if (fromPort < 1 || fromPort > 65535 || toPort < 1 || toPort > 65535 || fromPort > toPort) {
                    this.errorMessage("Invalid port range");
                    return false;
                }
                ruleParams.setFromPort(fromPort);
                ruleParams.setToPort(toPort);
            } else {
                int port = Integer.parseInt(this.portField.getValue());
                ruleParams.setFromPort(port);
                ruleParams.setToPort(port);
            }
        } catch (NumberFormatException e) {
            this.errorMessage("Invalid port number");
            return false;
        }
        if (this.sourceChoice.getValue().equals("CIDR")) {
            if (this.sourceIpRangeField.getValue().isEmpty()) {
                this.errorMessage("Missing cidr");
                return false;
            }
            try {
                new SubnetUtils(this.sourceIpRangeField.getValue());
            } catch (IllegalArgumentException e) {
                // XXX issue with commons-net library 3.3
                if (!this.sourceIpRangeField.getValue().equals("0.0.0.0/0")) {
                    this.errorMessage("Invalid cidr");
                    return false;
                }
            }
            ruleParams.setSourceIpRange(this.sourceIpRangeField.getValue());
        } else {
            ruleParams.setSourceGroupUuid((String) this.sourceSecGroupBox.getValue());
        }
        return true;
    }

    private void errorMessage(final String message) {
        this.errorLabel.setValue(message);
    }

    public void buttonClick(final ClickEvent event) {
        if (event.getSource() == this.okButton) {
            SecurityGroupRuleParams ruleParams = new SecurityGroupRuleParams();
            if (this.fillAndValidate(ruleParams)) {
                this.close();
                this.callback.response(ruleParams);
            }
        } else {
            this.close();
        }
    }

    public interface DialogCallback {
        void response(SecurityGroupRuleParams ruleParams);
    }
}