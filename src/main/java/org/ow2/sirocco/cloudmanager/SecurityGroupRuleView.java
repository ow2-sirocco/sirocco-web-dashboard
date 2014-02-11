/**
 *
 * SIROCCO
 * Copyright (C) 2014 Orange
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

import java.util.ArrayList;
import java.util.List;

import org.ow2.sirocco.cloudmanager.SecurityGroupRuleCreationDialog.DialogCallback;
import org.ow2.sirocco.cloudmanager.SecurityGroupRuleCreationDialog.SecGroupChoice;
import org.ow2.sirocco.cloudmanager.core.api.INetworkManager;
import org.ow2.sirocco.cloudmanager.core.api.QueryParams;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.core.api.exception.ResourceNotFoundException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroup;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroupRule;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroupRuleParams;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class SecurityGroupRuleView extends HorizontalLayout implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Table ruleTable;

    private BeanContainer<String, SecurityGroupRuleBean> rules;

    private Button deleteRuleButton;

    private SecurityGroup secGroup;

    private INetworkManager networkManager;

    public SecurityGroupRuleView() {
        this.setSizeFull();
        this.setMargin(true);
        this.ruleTable = new Table();
        this.ruleTable.setWidth("450px");
        this.rules = new BeanContainer<String, SecurityGroupRuleBean>(SecurityGroupRuleBean.class);
        this.rules.setBeanIdProperty("id");
        this.ruleTable.setContainerDataSource(this.rules);

        this.ruleTable.setPageLength(0);

        this.ruleTable.setSelectable(true);
        this.ruleTable.setImmediate(true);

        this.ruleTable.setVisibleColumns("protocol", "port", "source");

        this.ruleTable.setColumnWidth("protocol", 100);
        this.ruleTable.setColumnWidth("port", 150);
        this.ruleTable.setColumnWidth("source", 150);

        this.ruleTable.addValueChangeListener(this);
        this.addComponent(this.ruleTable);

        VerticalLayout buttonContainer = new VerticalLayout();
        buttonContainer.setMargin(true);
        buttonContainer.setSpacing(true);
        Button addRuleButton = new Button("Add rule...");
        addRuleButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                SecurityGroupRuleView.this.addRule();
            }
        });
        buttonContainer.addComponent(addRuleButton);
        this.deleteRuleButton = new Button("Delete rule");
        this.deleteRuleButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                SecurityGroupRuleView.this.deleteRule();
            }
        });
        buttonContainer.addComponent(this.deleteRuleButton);
        this.deleteRuleButton.setEnabled(false);
        this.addComponent(buttonContainer);
    }

    private void addRule() {
        List<SecurityGroupRuleCreationDialog.SecGroupChoice> choices = new ArrayList<>();

        List<SecurityGroup> groups;
        try {
            groups = this.networkManager.getSecurityGroups(
                new QueryParams.Builder()
                    .filterByProvider(SecurityGroupRuleView.this.secGroup.getCloudProviderAccount().getUuid())
                    .filterByLocation(SecurityGroupRuleView.this.secGroup.getLocation().getUuid()).build()).getItems();
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Internal error", e);
            return;
        }
        for (SecurityGroup group : groups) {
            SecGroupChoice choice = new SecGroupChoice();
            choice.id = group.getUuid();
            choice.name = group.getName();
            choices.add(choice);
        }

        SecurityGroupRuleCreationDialog dialog = new SecurityGroupRuleCreationDialog(choices, new DialogCallback() {

            @Override
            public void response(final SecurityGroupRuleParams ruleParams) {
                try {
                    SecurityGroupRuleView.this.networkManager.addRuleToSecurityGroup(
                        SecurityGroupRuleView.this.secGroup.getUuid(), ruleParams);
                } catch (CloudProviderException e) {
                    Util.diplayErrorMessageBox("Internal error", e);
                }
                SecurityGroupRuleView.this.refresh();
            }
        });
        UI.getCurrent().addWindow(dialog);
    }

    private void deleteRule() {
        try {
            this.networkManager.deleteRuleFromSecurityGroup((String) SecurityGroupRuleView.this.ruleTable.getValue());
            SecurityGroupRuleView.this.refresh();
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Cannot delete rule", e);
        }
    }

    public void init(final SecurityGroup secGroup, final INetworkManager networkManager) {
        this.networkManager = networkManager;
        this.secGroup = secGroup;
        this.refresh();
    }

    public void refresh() {
        try {
            this.secGroup = SecurityGroupRuleView.this.networkManager.getSecurityGroupById(this.secGroup.getId());
        } catch (ResourceNotFoundException e) {
            // Util.diplayErrorMessageBox("Internal error", e);
        }
        this.ruleTable.setValue(null);
        this.ruleTable.getContainerDataSource().removeAllItems();
        for (SecurityGroupRule rule : this.secGroup.getRules()) {
            this.rules.addBean(new SecurityGroupRuleBean(rule));
        }
        this.valueChange(null);
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Object ruleId = this.ruleTable.getValue();
        this.deleteRuleButton.setEnabled(ruleId != null);
    }

    public static class SecurityGroupRuleBean {
        String id;

        String protocol;

        String port;

        String source;

        SecurityGroupRuleBean(final SecurityGroupRule rule) {
            this.init(rule);
        }

        void init(final SecurityGroupRule rule) {
            this.id = rule.getUuid();
            this.protocol = rule.getIpProtocol();
            if (rule.getFromPort().equals(rule.getToPort())) {
                this.port = rule.getFromPort().toString();
            } else {
                this.port = rule.getFromPort().toString() + "-" + rule.getToPort().toString();
            }
            if (rule.getSourceIpRange() != null) {
                this.source = rule.getSourceIpRange();
            } else {
                this.source = rule.getSourceGroup().getName();
            }
        }

        public String getId() {
            return this.id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getProtocol() {
            return this.protocol;
        }

        public void setProtocol(final String protocol) {
            this.protocol = protocol;
        }

        public String getPort() {
            return this.port;
        }

        public void setPort(final String port) {
            this.port = port;
        }

        public String getSource() {
            return this.source;
        }

        public void setSource(final String source) {
            this.source = source;
        }

    }

}
