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

import org.ow2.sirocco.cloudmanager.CloudProviderView.CloudProviderAccountBean;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderAccount;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderProfile;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderProfile.AccountParameter;
import org.ow2.sirocco.cloudmanager.util.InputDialog;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class ProviderAccountDetailView extends VerticalLayout implements MetadataView.Callback {
    private static final long serialVersionUID = 1L;

    private Label title;

    private Table attributeTable;

    private CloudProviderAccountBean providerBean;

    private CloudProviderView providerView;

    private MetadataView metadataView;

    public ProviderAccountDetailView(final CloudProviderView providerView) {
        this.providerView = providerView;
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

        this.metadataView = new MetadataView(this);

        tabSheet.addTab(this.metadataView, "Metadata");
        this.addComponent(tabSheet);
        this.setExpandRatio(tabSheet, 1.0f);
    }

    public void updateResourceMetadata(final Map<String, String> metadata) {
        Map<String, Object> updatedAttributes = new HashMap<>();
        updatedAttributes.put("properties", metadata);
        try {
            this.providerBean.account = this.providerView.cloudProviderManager.updateCloudProviderAccountAttributes(
                this.providerBean.getId(), updatedAttributes);
        } catch (CloudProviderException e) {
            // TODO
            e.printStackTrace();
        }
        this.update(this.providerBean);
    }

    private int index = 1;

    private void onDialogResponse(final String attributeName, final String value) {
        Map<String, Object> updatedAttributes = new HashMap<>();
        updatedAttributes.put(attributeName, value);
        try {
            ProviderAccountDetailView.this.providerBean.account = ProviderAccountDetailView.this.providerView.cloudProviderManager
                .updateCloudProviderAccountAttributes(ProviderAccountDetailView.this.providerBean.getId(), updatedAttributes);
        } catch (CloudProviderException e) {
            // TODO
            e.printStackTrace();
        }
        ProviderAccountDetailView.this.providerBean = ProviderAccountDetailView.this.providerView
            .updateProviderAccountAttribute(ProviderAccountDetailView.this.providerBean, attributeName, value);
        ProviderAccountDetailView.this.update(ProviderAccountDetailView.this.providerBean);

    }

    @SuppressWarnings("serial")
    private void addAttribute(final String displayedAttributeName, final String attributeName, final String value,
        String displayValue, final boolean editable) {
        Button editAttribute;
        if (editable) {
            editAttribute = new Button("edit");
            editAttribute.addClickListener(new Button.ClickListener() {
                public void buttonClick(final ClickEvent event) {
                    InputDialog inputDialog = attributeName.equals("password") ? InputDialog.newPasswordDialog("Enter "
                        + displayedAttributeName, displayedAttributeName, value, new InputDialog.DialogCallback() {

                        @Override
                        public void response(final String value) {
                            ProviderAccountDetailView.this.onDialogResponse(attributeName, value);
                        }
                    }) : InputDialog.newInputDialog("Enter " + displayedAttributeName, displayedAttributeName, value,
                        new InputDialog.DialogCallback() {

                            @Override
                            public void response(final String value) {
                                ProviderAccountDetailView.this.onDialogResponse(attributeName, value);
                            }
                        });
                    UI.getCurrent().addWindow(inputDialog);
                }
            });
            editAttribute.addStyleName("link");
        } else {
            editAttribute = null;
        }
        if (displayValue == null) {
            displayValue = value;
        }
        this.attributeTable.addItem(new Object[] {displayedAttributeName, displayValue, editAttribute}, new Integer(
            this.index++));
    }

    public void hide() {
        this.providerBean = null;
        this.setVisible(false);
    }

    public CloudProviderAccount getProviderAccount() {
        return this.providerBean.account;
    }

    public void update(final CloudProviderAccountBean providerBean) {
        this.setVisible(true);
        this.providerBean = providerBean;
        CloudProviderAccount account = providerBean.account;
        this.title.setValue("Provider Account " + account.getCloudProvider().getDescription());
        this.attributeTable.getContainerDataSource().removeAllItems();
        this.index = 1;
        this.addAttribute("name", "name", providerBean.getName(), null, true);
        this.addAttribute("id", "id", account.getUuid(), null, false);
        if (account.getCreated() != null) {
            this.addAttribute("created", "created", account.getCreated().toString(), null, false);
        }
        AccountParameter param = providerBean.profile.findAccountParameter(CloudProviderProfile.PROVIDER_ENDPOINT);
        if (param != null) {
            this.addAttribute(param.getDescription(), "endpoint", providerBean.getEndpoint(), null, true);
        }
        param = providerBean.profile.findAccountParameter(CloudProviderProfile.PROVIDER_ACCOUNT_LOGIN);
        if (param != null) {
            this.addAttribute(param.getDescription(), "login", providerBean.getLogin(), null, true);
        }
        param = providerBean.profile.findAccountParameter(CloudProviderProfile.PROVIDER_ACCOUNT_PASSWORD);
        if (param != null) {
            this.addAttribute(param.getDescription(), "password", account.getPassword(), "****", true);
        }

        this.metadataView.init(account.getProperties());

    }

}
