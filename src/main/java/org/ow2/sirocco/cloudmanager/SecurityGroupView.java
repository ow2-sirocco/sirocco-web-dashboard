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

import java.util.Set;

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.core.api.INetworkManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroup;

import com.vaadin.cdi.UIScoped;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

@UIScoped
public class SecurityGroupView extends VerticalSplitPanel implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Button deleteSecurityGroupButton;

    private Table securityGroupTable;

    BeanContainer<String, SecurityGroupBean> securityGroups;

    @Inject
    private SecurityGroupCreationWizard securityGroupCreationWizard;

    @Inject
    INetworkManager networkManager;

    private SecurityGroupDetailView detailView;

    public SecurityGroupView() {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();

        HorizontalLayout actionButtonHeader = new HorizontalLayout();
        actionButtonHeader.setMargin(true);
        actionButtonHeader.setSpacing(true);
        actionButtonHeader.setWidth("100%");
        actionButtonHeader.setHeight("50px");

        Button button = new Button("Create SecurityGroup...");
        button.setIcon(new ThemeResource("img/add.png"));
        button.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                if (SecurityGroupView.this.securityGroupCreationWizard.init(SecurityGroupView.this)) {
                    UI.getCurrent().addWindow(SecurityGroupView.this.securityGroupCreationWizard);
                }
            }
        });
        actionButtonHeader.addComponent(button);

        this.deleteSecurityGroupButton = new Button("Delete");
        this.deleteSecurityGroupButton.setIcon(new ThemeResource("img/delete.png"));
        this.deleteSecurityGroupButton.setEnabled(false);
        this.deleteSecurityGroupButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                final Set<?> selectedSecurityGroupIds = (Set<?>) SecurityGroupView.this.securityGroupTable.getValue();
                String name = SecurityGroupView.this.securityGroups.getItem(selectedSecurityGroupIds.iterator().next())
                    .getBean().getName();
                ConfirmDialog confirmDialog = ConfirmDialog.newConfirmDialog("Delete SecurityGroup",
                    "Are you sure you want to delete securityGroup " + name + " ?",
                    new ConfirmDialog.ConfirmationDialogCallback() {

                        @Override
                        public void response(final boolean ok, final boolean ignored) {
                            if (ok) {
                                for (Object id : selectedSecurityGroupIds) {
                                    try {
                                        SecurityGroupView.this.networkManager.deleteSecurityGroup(id.toString());
                                    } catch (CloudProviderException e) {
                                        Util.diplayErrorMessageBox("Cannot delete security group "
                                            + SecurityGroupView.this.securityGroups.getItem(id).getBean().getName(), e);
                                    }
                                }
                                SecurityGroupView.this.valueChange(null);
                            }
                        }
                    });
                SecurityGroupView.this.getUI().addWindow(confirmDialog);
            }
        });
        actionButtonHeader.addComponent(this.deleteSecurityGroupButton);

        Label spacer = new Label();
        spacer.setWidth("100%");
        actionButtonHeader.addComponent(spacer);
        actionButtonHeader.setExpandRatio(spacer, 1.0f);

        button = new Button("Refresh", new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                SecurityGroupView.this.refresh();
            }
        });
        button.setIcon(new ThemeResource("img/refresh.png"));
        actionButtonHeader.addComponent(button);

        verticalLayout.addComponent(actionButtonHeader);
        verticalLayout.addComponent(this.securityGroupTable = this.createSecurityGroupTable());
        verticalLayout.setExpandRatio(this.securityGroupTable, 1.0f);

        this.setFirstComponent(verticalLayout);
        this.setSecondComponent(this.detailView = new SecurityGroupDetailView(this));
        this.setSplitPosition(60.0f);

    }

    void refresh() {
        this.securityGroupTable.setValue(null);
        this.securityGroupTable.getContainerDataSource().removeAllItems();
        try {
            for (SecurityGroup securityGroup : this.networkManager.getSecurityGroups().getItems()) {
                this.securityGroups.addBean(new SecurityGroupBean(securityGroup));
            }
        } catch (CloudProviderException e) {
            e.printStackTrace();
        }
        this.valueChange(null);
    }

    Table createSecurityGroupTable() {
        this.securityGroups = new BeanContainer<String, SecurityGroupBean>(SecurityGroupBean.class);
        this.securityGroups.setBeanIdProperty("id");
        Table table = new Table();
        table.setContainerDataSource(this.securityGroups);

        table.setSizeFull();
        table.setPageLength(0);

        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setImmediate(true);

        table.addGeneratedColumn("state", new Util.StateColumnGenerator());
        table.addGeneratedColumn("location", new Util.LocationColumnGenerator());

        table.setVisibleColumns("name", "state", "description", "provider", "location");

        table.addValueChangeListener(this);

        return table;
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Set<?> selectedSecurityGroupIds = (Set<?>) this.securityGroupTable.getValue();
        if (selectedSecurityGroupIds != null && selectedSecurityGroupIds.size() > 0) {
            if (selectedSecurityGroupIds.size() == 1) {
                Object id = selectedSecurityGroupIds.iterator().next();
                String state = (String) this.securityGroupTable.getItem(id).getItemProperty("state").getValue();
                this.deleteSecurityGroupButton.setEnabled(!state.endsWith("DELETING"));
                this.detailView.update(this.securityGroups.getItem(id).getBean());
            } else {
                this.detailView.hide();
                boolean allowMultiDelete = true;
                for (Object machineId : selectedSecurityGroupIds) {
                    String state = (String) this.securityGroupTable.getItem(machineId).getItemProperty("state").getValue();
                    if (state.endsWith("DELETING")) {
                        allowMultiDelete = false;
                        break;
                    }
                }
                this.deleteSecurityGroupButton.setEnabled(allowMultiDelete);
            }
        } else {
            this.detailView.hide();
            this.deleteSecurityGroupButton.setEnabled(false);
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }

    public void updateSecurityGroup(final SecurityGroup securityGroup) {
        BeanItem<SecurityGroupBean> item = this.securityGroups.getItem(securityGroup.getUuid());
        if (item != null) {
            SecurityGroupBean securityGroupBean = item.getBean();
            securityGroupBean.init(securityGroup);
            item.getItemProperty("state").setValue(securityGroupBean.getState());
            item.getItemProperty("name").setValue(securityGroupBean.getName());
            this.valueChange(null);
        }
    }

    SecurityGroupBean updateSecurityGroupAttribute(final SecurityGroupBean securityGroupBean, final String attribute,
        final String value) {
        this.securityGroupTable.getItem(securityGroupBean.getId()).getItemProperty(attribute).setValue(value);
        return this.securityGroups.getItem(securityGroupBean.getId()).getBean();
    }

    public static class SecurityGroupBean {
        SecurityGroup securityGroup;

        String id;

        String name;

        String description;

        String state;

        String provider;

        String location;

        SecurityGroupBean(final SecurityGroup securityGroup) {
            this.init(securityGroup);
        }

        void init(final SecurityGroup securityGroup) {
            this.securityGroup = securityGroup;
            this.id = securityGroup.getUuid();
            this.name = securityGroup.getName();
            this.description = securityGroup.getDescription();
            this.state = securityGroup.getState().toString();
            this.provider = this.providerFrom(securityGroup);
            this.location = this.locationFrom(securityGroup);
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

        public String getDescription() {
            return this.description;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getState() {
            return this.state;
        }

        public void setState(final String state) {
            this.state = state;
        }

        public String getProvider() {
            return this.provider;
        }

        public void setProvider(final String provider) {
            this.provider = provider;
        }

        public String getLocation() {
            return this.location;
        }

        public void setLocation(final String location) {
            this.location = location;
        }

        public String providerFrom(final SecurityGroup securityGroup) {
            if (securityGroup.getCloudProviderAccount() != null) {
                return securityGroup.getCloudProviderAccount().getCloudProvider().getDescription();
            } else {
                return "";
            }
        }

        public String locationFrom(final SecurityGroup securityGroup) {
            if (securityGroup.getLocation() != null) {
                return securityGroup.getLocation().description(true);
            } else {
                return "";
            }
        }
    }

}
