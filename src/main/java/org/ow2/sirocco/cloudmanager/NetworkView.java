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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ow2.sirocco.cloudmanager.core.api.INetworkManager;
import org.ow2.sirocco.cloudmanager.core.api.IdentityContextHolder;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.core.api.exception.ResourceNotFoundException;
import org.ow2.sirocco.cloudmanager.model.cimi.Network;
import org.ow2.sirocco.cloudmanager.model.cimi.Subnet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Component("NetworkView")
@Scope("prototype")
public class NetworkView extends VerticalLayout implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Button deleteNetworkButton;

    private Table networkTable;

    BeanContainer<Integer, NetworkBean> networks;

    @Autowired
    private NetworkCreationWizard networkCreationWizard;

    @Autowired
    @Qualifier("INetworkManager")
    private INetworkManager networkManager;

    public NetworkView() {
        this.setSizeFull();

        HorizontalLayout actionButtonHeader = new HorizontalLayout();
        actionButtonHeader.setMargin(true);
        actionButtonHeader.setSpacing(true);
        actionButtonHeader.setWidth("100%");
        actionButtonHeader.setHeight("50px");

        Button button = new Button("Create Network...");
        button.setIcon(new ThemeResource("img/add.png"));
        button.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                NetworkView.this.networkCreationWizard.init(NetworkView.this);
                UI.getCurrent().addWindow(NetworkView.this.networkCreationWizard);
            }
        });
        actionButtonHeader.addComponent(button);

        this.deleteNetworkButton = new Button("Delete");
        this.deleteNetworkButton.setIcon(new ThemeResource("img/delete.png"));
        this.deleteNetworkButton.setEnabled(false);
        this.deleteNetworkButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                final Set<?> selectedNetworkIds = (Set<?>) NetworkView.this.networkTable.getValue();
                String name = NetworkView.this.networks.getItem(selectedNetworkIds.iterator().next()).getBean().getName();
                ConfirmDialog confirmDialog = new ConfirmDialog("Delete Network", "Are you sure you want to delete network "
                    + name + " ?", "Ok", "Cancel", new ConfirmDialog.ConfirmationDialogCallback() {

                    @Override
                    public void response(final boolean ok) {
                        if (ok) {
                            MyUI ui = (MyUI) UI.getCurrent();
                            IdentityContextHolder.set(ui.getTenantId(), ui.getUserName());
                            for (Object id : selectedNetworkIds) {
                                try {
                                    NetworkView.this.networkManager.deleteNetwork(id.toString());
                                    Network network = NetworkView.this.networkManager.getNetworkById(id.toString());
                                    NetworkBean newMachineImageBean = new NetworkBean(network);
                                    int index = NetworkView.this.networks.indexOfId(id);
                                    NetworkView.this.networks.removeItem(id);
                                    NetworkView.this.networks.addBeanAt(index, newMachineImageBean);
                                } catch (CloudProviderException e) {
                                    e.printStackTrace();
                                }
                            }
                            NetworkView.this.valueChange(null);
                        }
                    }
                });
                NetworkView.this.getUI().addWindow(confirmDialog);
            }
        });
        actionButtonHeader.addComponent(this.deleteNetworkButton);

        Label spacer = new Label();
        spacer.setWidth("100%");
        actionButtonHeader.addComponent(spacer);
        actionButtonHeader.setExpandRatio(spacer, 1.0f);

        button = new Button("Refresh", new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                NetworkView.this.refresh();
            }
        });
        button.setIcon(new ThemeResource("img/refresh.png"));
        actionButtonHeader.addComponent(button);

        this.addComponent(actionButtonHeader);
        this.addComponent(this.networkTable = this.createNetworkTable());
        this.setExpandRatio(this.networkTable, 1.0f);

        // refresh();
    }

    Label createLabel(final String iconFileName, final String text) {
        Label label = new Label();
        label.setContentMode(ContentMode.HTML);
        label.setValue("<img src=\"" + "VAADIN/themes/mytheme/img/" + iconFileName + "\" /> " + text);
        return label;
    }

    Label makeCountryLabel(final String country) {
        return this.createLabel(country.toLowerCase() + "Flag.png", "");
    }

    void refresh() {
        this.networkTable.getContainerDataSource().removeAllItems();
        try {
            MyUI ui = (MyUI) UI.getCurrent();
            IdentityContextHolder.set(ui.getTenantId(), ui.getUserName());
            for (Network network : this.networkManager.getNetworks()) {
                System.out.println("Network id=" + network.getId() + " name=" + network.getName());
                this.networks.addBean(new NetworkBean(network));
            }
        } catch (CloudProviderException e) {
            e.printStackTrace();
        }
        this.valueChange(null);
    }

    Table createNetworkTable() {
        this.networks = new BeanContainer<Integer, NetworkBean>(NetworkBean.class);
        this.networks.setBeanIdProperty("id");
        Table table = new Table();
        table.setContainerDataSource(this.networks);

        table.setSizeFull();
        table.setPageLength(0);

        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setImmediate(true);

        table.addGeneratedColumn("state", new Util.StateColumnGenerator());
        table.addGeneratedColumn("location", new Util.LocationColumnGenerator());

        table.setVisibleColumns("name", "subnets", "state", "provider", "location");

        table.addValueChangeListener(this);

        return table;
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Set<?> selectedNetworkIds = (Set<?>) this.networkTable.getValue();
        if (selectedNetworkIds != null && selectedNetworkIds.size() > 0) {
            if (selectedNetworkIds.size() == 1) {
                String state = (String) this.networkTable.getItem(selectedNetworkIds.iterator().next())
                    .getItemProperty("state").getValue();
                this.deleteNetworkButton.setEnabled(!state.endsWith("DELETING"));
            } else {
                boolean allowMultiDelete = true;
                for (Object machineId : selectedNetworkIds) {
                    String state = (String) this.networkTable.getItem(machineId).getItemProperty("state").getValue();
                    if (state.endsWith("DELETING")) {
                        allowMultiDelete = false;
                        break;
                    }
                }
                this.deleteNetworkButton.setEnabled(allowMultiDelete);
            }
        } else {
            this.deleteNetworkButton.setEnabled(false);
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }

    void pollNetworks() {
        List<Integer> ids = new ArrayList<>(this.networks.getItemIds());
        for (Integer id : ids) {
            NetworkBean machineImageBean = this.networks.getItem(id).getBean();
            if (machineImageBean.getState().endsWith("ING")) {
                try {
                    Network network = this.networkManager.getNetworkById(id.toString());
                    System.out.println("Network id=" + id + " state=" + network.getState());
                    if (!network.getState().toString().endsWith("ING")) {

                        NetworkBean newNetworkBean = new NetworkBean(network);
                        int index = this.networks.indexOfId(id);
                        this.networks.removeItem(id);
                        this.networks.addBeanAt(index, newNetworkBean);
                        this.networkTable.setValue(null);
                        this.valueChange(null);
                        this.getUI().push();
                    }
                } catch (ResourceNotFoundException e) {
                    this.networks.removeItem(id);
                    this.networkTable.setValue(null);
                    this.valueChange(null);
                    System.out.println("REMOVE PUSH");
                    this.getUI().push();
                } catch (CloudProviderException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class NetworkBean {
        Integer id;

        String name;

        String description;

        String state;

        String subnets;

        String provider;

        String location;

        NetworkBean(final Network network) {
            this.id = network.getId();
            this.name = network.getName();
            this.description = network.getDescription();
            this.state = network.getState().toString();
            this.provider = this.providerFrom(network);
            this.location = this.locationFrom(network);
            this.subnets = this.subnetsFrom(network);
        }

        public Integer getId() {
            return this.id;
        }

        public void setId(final Integer id) {
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

        public String getSubnets() {
            return this.subnets;
        }

        public void setSubnets(final String subnets) {
            this.subnets = subnets;
        }

        private String subnetsFrom(final Network network) {
            StringBuilder sb = new StringBuilder();
            if (network.getSubnets() != null) {
                for (Subnet subnet : network.getSubnets()) {
                    sb.append(subnet.getName() + " " + subnet.getCidr() + "\n");
                }
            }
            return sb.toString();
        }

        public String providerFrom(final Network network) {
            if (network.getCloudProviderAccount() != null) {
                return network.getCloudProviderAccount().getCloudProvider().getDescription();
            } else {
                return "";
            }
        }

        public String locationFrom(final Network network) {
            if (network.getLocation() != null) {
                return network.getLocation().getCountryName();
            } else {
                return "";
            }
        }
    }
}
