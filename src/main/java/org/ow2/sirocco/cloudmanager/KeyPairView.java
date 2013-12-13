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

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.core.api.ICredentialsManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.Credentials;
import org.ow2.sirocco.cloudmanager.model.cimi.CredentialsCreate;
import org.ow2.sirocco.cloudmanager.model.cimi.CredentialsTemplate;

import com.vaadin.cdi.UIScoped;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@UIScoped
public class KeyPairView extends VerticalLayout implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Button deleteKeyPairButton;

    private Table keyPairTable;

    BeanContainer<String, KeyPairBean> keyPairs;

    @Inject
    private ICredentialsManager credentialManager;

    public KeyPairView() {
        this.setSizeFull();

        HorizontalLayout actionButtonHeader = new HorizontalLayout();
        actionButtonHeader.setMargin(true);
        actionButtonHeader.setSpacing(true);
        actionButtonHeader.setWidth("100%");
        actionButtonHeader.setHeight("50px");

        Button button = new Button("Import Key Pair...");
        button.setIcon(new ThemeResource("img/add.png"));
        button.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                KeyPairImportDialog dialog = new KeyPairImportDialog(new KeyPairImportDialog.DialogCallback() {

                    @Override
                    public void response(final String name, final String publicKey) {
                        try {
                            CredentialsCreate credentialsCreate = new CredentialsCreate();
                            credentialsCreate.setName(name);
                            CredentialsTemplate credentialsTemplate = new CredentialsTemplate();
                            credentialsTemplate.setPublicKey(publicKey);
                            credentialsCreate.setCredentialTemplate(credentialsTemplate);
                            KeyPairView.this.credentialManager.createCredentials(credentialsCreate);
                        } catch (CloudProviderException e) {
                            Notification.show("Key Pair creation failure", e.getMessage(), Notification.Type.ERROR_MESSAGE);
                        }
                        KeyPairView.this.refresh();
                        KeyPairView.this.valueChange(null);
                    }
                });
                UI.getCurrent().addWindow(dialog);
            }
        });
        actionButtonHeader.addComponent(button);

        this.deleteKeyPairButton = new Button("Delete");
        this.deleteKeyPairButton.setIcon(new ThemeResource("img/delete.png"));
        this.deleteKeyPairButton.setEnabled(false);
        this.deleteKeyPairButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                final Set<?> selectedKeyPairIds = (Set<?>) KeyPairView.this.keyPairTable.getValue();
                String name = KeyPairView.this.keyPairs.getItem(selectedKeyPairIds.iterator().next()).getBean().getName();
                ConfirmDialog confirmDialog = ConfirmDialog.newConfirmDialog("Delete Image",
                    "Are you sure you want to delete key pair " + name + " ?", new ConfirmDialog.ConfirmationDialogCallback() {

                        @Override
                        public void response(final boolean ok, final boolean ignored) {
                            if (ok) {
                                for (Object id : selectedKeyPairIds) {
                                    try {
                                        KeyPairView.this.credentialManager.deleteCredentials(id.toString());
                                        KeyPairView.this.keyPairs.removeItem(id);
                                    } catch (CloudProviderException e) {
                                        e.printStackTrace();
                                    }
                                }
                                KeyPairView.this.keyPairTable.setValue(null);
                                KeyPairView.this.valueChange(null);
                            }
                        }
                    });
                KeyPairView.this.getUI().addWindow(confirmDialog);
            }
        });
        actionButtonHeader.addComponent(this.deleteKeyPairButton);

        Label spacer = new Label();
        spacer.setWidth("100%");
        actionButtonHeader.addComponent(spacer);
        actionButtonHeader.setExpandRatio(spacer, 1.0f);

        button = new Button("Refresh", new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                KeyPairView.this.refresh();
            }
        });
        button.setIcon(new ThemeResource("img/refresh.png"));
        actionButtonHeader.addComponent(button);

        this.addComponent(actionButtonHeader);
        this.addComponent(this.keyPairTable = this.createkeyPairTable());
        this.setExpandRatio(this.keyPairTable, 1.0f);

    }

    void refresh() {
        this.keyPairTable.getContainerDataSource().removeAllItems();
        try {
            for (Credentials cred : this.credentialManager.getCredentials()) {
                this.keyPairs.addBean(new KeyPairBean(cred));
            }
        } catch (CloudProviderException e) {
            e.printStackTrace();
        }
        this.valueChange(null);
    }

    Table createkeyPairTable() {
        this.keyPairs = new BeanContainer<String, KeyPairBean>(KeyPairBean.class);
        this.keyPairs.setBeanIdProperty("id");
        Table table = new Table();
        table.setContainerDataSource(this.keyPairs);

        table.setSizeFull();
        table.setPageLength(0);

        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setImmediate(true);

        table.setVisibleColumns("id", "name", "created");

        table.addValueChangeListener(this);

        return table;
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Set<?> selectedKeyPairIds = (Set<?>) this.keyPairTable.getValue();
        if (selectedKeyPairIds != null && selectedKeyPairIds.size() > 0) {
            this.deleteKeyPairButton.setEnabled(true);
        } else {
            this.deleteKeyPairButton.setEnabled(false);
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }

    public static class KeyPairBean {
        String id;

        String name;

        Date created;

        KeyPairBean(final Credentials credential) {
            this.id = credential.getUuid();
            this.name = credential.getName();
            this.created = credential.getCreated();
        }

        public String getId() {
            return this.id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getName() {
            return this.name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Date getCreated() {
            return this.created;
        }

        public void setCreated(final Date created) {
            this.created = created;
        }

    }
}
