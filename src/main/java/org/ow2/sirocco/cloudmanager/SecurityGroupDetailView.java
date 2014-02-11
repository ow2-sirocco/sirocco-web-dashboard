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
package org.ow2.sirocco.cloudmanager;

import java.util.HashMap;
import java.util.Map;

import org.ow2.sirocco.cloudmanager.SecurityGroupView.SecurityGroupBean;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroup;
import org.ow2.sirocco.cloudmanager.util.InputDialog;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class SecurityGroupDetailView extends VerticalLayout implements MetadataView.Callback {
    private static final long serialVersionUID = 1L;

    private Label title;

    private Table attributeTable;

    private SecurityGroupBean securityGroupBean;

    private SecurityGroupView securityGroupView;

    private SecurityGroupRuleView ruleView;

    public SecurityGroupDetailView(final SecurityGroupView securityGroupView) {
        this.securityGroupView = securityGroupView;
        this.setSizeFull();
        this.setSpacing(true);
        this.setMargin(true);
        this.addStyleName("detailmargins");
        this.setVisible(false);
        this.title = new Label();
        this.title.setStyleName("detailTitle");
        this.addComponent(this.title);
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        VerticalLayout attributeTab = new VerticalLayout();
        attributeTab.setSizeFull();
        this.attributeTable = new Table();
        attributeTab.addComponent(this.attributeTable);
        this.attributeTable.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        this.attributeTable.setSizeFull();
        this.attributeTable.setPageLength(0);
        this.attributeTable.setSelectable(false);
        this.attributeTable.addContainerProperty("attribute", String.class, null);
        this.attributeTable.addContainerProperty("value", String.class, null);
        this.attributeTable.addContainerProperty("edit", Button.class, null);
        this.attributeTable.setColumnWidth("edit", 400);

        tabSheet.addTab(attributeTab, "Attributes");

        this.ruleView = new SecurityGroupRuleView();
        tabSheet.addTab(this.ruleView, "Rules");
        this.addComponent(tabSheet);
        this.setExpandRatio(tabSheet, 1.0f);
    }

    public void updateResourceMetadata(final Map<String, String> metadata) {
        Map<String, Object> updatedAttributes = new HashMap<>();
        updatedAttributes.put("properties", metadata);
        try {
            this.securityGroupBean.securityGroup = this.securityGroupView.networkManager.updateSecurityGroupAttributes(
                this.securityGroupBean.getId(), updatedAttributes);
        } catch (CloudProviderException e) {
            // TODO
            e.printStackTrace();
        }
        this.update(this.securityGroupBean);
    }

    private int index = 1;

    @SuppressWarnings("serial")
    private void addAttribute(final String attributeName, final String value, final boolean editable) {
        Button editAttribute;
        if (editable) {
            editAttribute = new Button("edit");
            editAttribute.addClickListener(new Button.ClickListener() {
                public void buttonClick(final ClickEvent event) {
                    InputDialog inputDialog = InputDialog.newInputDialog("Enter " + attributeName, attributeName, value,
                        new InputDialog.DialogCallback() {

                            @Override
                            public void response(final String value) {
                                Map<String, Object> updatedAttributes = new HashMap<>();
                                updatedAttributes.put(attributeName, value);
                                try {
                                    SecurityGroupDetailView.this.securityGroupBean.securityGroup = SecurityGroupDetailView.this.securityGroupView.networkManager
                                        .updateSecurityGroupAttributes(SecurityGroupDetailView.this.securityGroupBean.getId(),
                                            updatedAttributes);
                                } catch (CloudProviderException e) {
                                    // TODO
                                    e.printStackTrace();
                                }
                                SecurityGroupDetailView.this.securityGroupBean = SecurityGroupDetailView.this.securityGroupView
                                    .updateSecurityGroupAttribute(SecurityGroupDetailView.this.securityGroupBean,
                                        attributeName, value);
                                SecurityGroupDetailView.this.update(SecurityGroupDetailView.this.securityGroupBean);
                            }
                        });
                    UI.getCurrent().addWindow(inputDialog);
                }
            });
            editAttribute.addStyleName("link");
        } else {
            editAttribute = null;
        }

        this.attributeTable.addItem(new Object[] {attributeName, value, editAttribute}, new Integer(this.index++));
    }

    public void hide() {
        this.securityGroupBean = null;
        this.setVisible(false);
    }

    public SecurityGroup getSecurityGroup() {
        return this.securityGroupBean.securityGroup;
    }

    public void update(final SecurityGroupBean securityGroupBean) {
        this.setVisible(true);
        this.securityGroupBean = securityGroupBean;
        SecurityGroup securityGroup = securityGroupBean.securityGroup;
        this.title.setValue("Image " + securityGroup.getName());
        this.attributeTable.getContainerDataSource().removeAllItems();
        this.index = 1;
        this.addAttribute("name", securityGroupBean.getName(), true);
        this.addAttribute("description", securityGroupBean.getDescription(), true);
        this.addAttribute("id", securityGroup.getUuid(), false);
        this.addAttribute("tenant", securityGroup.getTenant().getName(), false);
        if (securityGroup.getCreated() != null) {
            this.addAttribute("created", securityGroup.getCreated().toString(), false);
        }
        if (securityGroup.getUpdated() != null) {
            this.addAttribute("updated", securityGroup.getUpdated().toString(), false);
        }
        this.addAttribute("state", securityGroup.getState().toString(), false);
        this.addAttribute("provider", securityGroup.getCloudProviderAccount().getCloudProvider().getDescription(), false);
        this.addAttribute("provider account id", securityGroup.getCloudProviderAccount().getUuid(), false);
        this.addAttribute("provider-assigned id", securityGroup.getProviderAssignedId(), false);
        this.addAttribute("location", securityGroup.getLocation().description(false), false);

        this.ruleView.init(securityGroup, this.securityGroupView.networkManager);
    }

}
