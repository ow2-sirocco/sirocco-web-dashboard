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
import org.ow2.sirocco.cloudmanager.model.cimi.Network;
import org.ow2.sirocco.cloudmanager.model.cimi.Subnet;

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

@UIScoped
public class NetworkView extends VerticalLayout implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Button deleteNetworkButton;

    private Table networkTable;

    BeanContainer<String, NetworkBean> networks;

    @Inject
    private NetworkCreationWizard networkCreationWizard;

    @Inject
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
                if (NetworkView.this.networkCreationWizard.init(NetworkView.this)) {
                    UI.getCurrent().addWindow(NetworkView.this.networkCreationWizard);
                }
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
                ConfirmDialog confirmDialog = ConfirmDialog.newConfirmDialog("Delete Network",
                    "Are you sure you want to delete network " + name + " ?", new ConfirmDialog.ConfirmationDialogCallback() {

                        @Override
                        public void response(final boolean ok, final boolean ignored) {
                            if (ok) {
                                for (Object id : selectedNetworkIds) {
                                    try {
                                        NetworkView.this.networkManager.deleteNetwork(id.toString());
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

    }

    void refresh() {
        this.networkTable.setValue(null);
        this.networkTable.getContainerDataSource().removeAllItems();
        try {
            for (Network network : this.networkManager.getNetworks().getItems()) {
                this.networks.addBean(new NetworkBean(network));
            }
        } catch (CloudProviderException e) {
            e.printStackTrace();
        }
        this.valueChange(null);
    }

    Table createNetworkTable() {
        this.networks = new BeanContainer<String, NetworkBean>(NetworkBean.class);
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

    public void updateNetwork(final Network network) {
        BeanItem<NetworkBean> item = this.networks.getItem(network.getUuid());
        if (item != null) {
            NetworkBean networkBean = item.getBean();
            networkBean.init(network);
            item.getItemProperty("state").setValue(networkBean.getState());
            item.getItemProperty("name").setValue(networkBean.getName());
            this.valueChange(null);
        }
    }

    public static class NetworkBean {
        String id;

        String name;

        String description;

        String state;

        String subnets;

        String provider;

        String location;

        NetworkBean(final Network network) {
            this.init(network);
        }

        void init(final Network network) {
            this.id = network.getUuid();
            this.name = network.getName();
            this.description = network.getDescription();
            this.state = network.getState().toString();
            this.provider = this.providerFrom(network);
            this.location = this.locationFrom(network);
            this.subnets = this.subnetsFrom(network);
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
                return network.getLocation().getIso3166_1();
            } else {
                return "";
            }
        }
    }

}
