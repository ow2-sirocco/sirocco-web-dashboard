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

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.AddressAssociateDialog.MachineChoice;
import org.ow2.sirocco.cloudmanager.core.api.IMachineManager;
import org.ow2.sirocco.cloudmanager.core.api.INetworkManager;
import org.ow2.sirocco.cloudmanager.core.api.QueryParams;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.Address;
import org.ow2.sirocco.cloudmanager.model.cimi.Address.State;
import org.ow2.sirocco.cloudmanager.model.cimi.Machine;

import com.google.common.collect.Iterables;
import com.vaadin.cdi.UIScoped;
import com.vaadin.data.Item;
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
public class AddressView extends VerticalLayout implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Button associateAddressButton;

    private Button disassociateAddressButton;

    private Button releaseAddressButton;

    private Table addressTable;

    BeanContainer<String, AddressBean> addresses;

    @Inject
    private AddressAllocationWizard addressAllocationWizard;

    @Inject
    private INetworkManager networkManager;

    @Inject
    private IMachineManager machineManager;

    public AddressView() {
        this.setSizeFull();

        HorizontalLayout actionButtonHeader = new HorizontalLayout();
        actionButtonHeader.setMargin(true);
        actionButtonHeader.setSpacing(true);
        actionButtonHeader.setWidth("100%");
        actionButtonHeader.setHeight("50px");

        Button button = new Button("Allocate Address...");
        button.setIcon(new ThemeResource("img/add.png"));
        button.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                if (AddressView.this.addressAllocationWizard.init(AddressView.this)) {
                    UI.getCurrent().addWindow(AddressView.this.addressAllocationWizard);
                }
            }
        });
        actionButtonHeader.addComponent(button);

        this.associateAddressButton = new Button("Associate");
        this.associateAddressButton.setEnabled(false);
        this.associateAddressButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                Set<?> selectedAddressIds = (Set<?>) AddressView.this.addressTable.getValue();
                final String addressId = (String) Iterables.getOnlyElement(selectedAddressIds);
                List<AddressAssociateDialog.MachineChoice> choices = new ArrayList<>();
                Address address = null;
                try {
                    address = AddressView.this.networkManager.getAddressByUuid(addressId);
                    List<Machine> machines = AddressView.this.machineManager.getMachines(
                        new QueryParams.Builder().filterByProvider(address.getCloudProviderAccount().getUuid())
                            .filterByLocation(address.getLocation().getUuid()).build()).getItems();
                    for (Machine machine : machines) {
                        MachineChoice machineChoice = new MachineChoice();
                        machineChoice.id = machine.getUuid();
                        machineChoice.name = machine.getName();
                        choices.add(machineChoice);
                    }

                } catch (CloudProviderException e) {
                    Util.diplayErrorMessageBox("Internal error", e);
                }

                final String ipAddress = address.getIp();
                AddressAssociateDialog addressAssociateDialog = new AddressAssociateDialog(choices,
                    new AddressAssociateDialog.DialogCallback() {

                        @Override
                        public void response(final String machineId) {
                            try {
                                AddressView.this.networkManager.addAddressToMachine(machineId, ipAddress);
                            } catch (CloudProviderException e) {
                                Util.diplayErrorMessageBox("Address association failure", e);
                            }
                        }
                    });
                UI.getCurrent().addWindow(addressAssociateDialog);
            }
        });
        actionButtonHeader.addComponent(this.associateAddressButton);

        this.disassociateAddressButton = new Button("Disassociate");
        this.disassociateAddressButton.setEnabled(false);
        this.disassociateAddressButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                Set<?> selectedAddressIds = (Set<?>) AddressView.this.addressTable.getValue();
                String addressId = (String) selectedAddressIds.iterator().next();
                try {
                    Address address = AddressView.this.networkManager.getAddressByUuid(addressId);
                    AddressView.this.networkManager.removeAddressFromMachine(address.getResource().getUuid(), address.getIp());
                } catch (CloudProviderException e) {
                    Util.diplayErrorMessageBox("Address disassociation failure", e);
                }
            }
        });
        actionButtonHeader.addComponent(this.disassociateAddressButton);

        this.releaseAddressButton = new Button("Release");
        this.releaseAddressButton.setIcon(new ThemeResource("img/delete.png"));
        this.releaseAddressButton.setEnabled(false);
        this.releaseAddressButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                final Set<?> selectedAddressIds = (Set<?>) AddressView.this.addressTable.getValue();
                String ip = AddressView.this.addresses.getItem(selectedAddressIds.iterator().next()).getBean().getIp();
                ConfirmDialog confirmDialog = ConfirmDialog.newConfirmDialog("Release Address",
                    "Are you sure you want to release address " + ip + " ?", new ConfirmDialog.ConfirmationDialogCallback() {

                        @Override
                        public void response(final boolean ok, final boolean ignored) {
                            if (ok) {
                                for (Object id : selectedAddressIds) {
                                    try {
                                        AddressView.this.networkManager.deleteAddress(id.toString());
                                    } catch (CloudProviderException e) {
                                        Util.diplayErrorMessageBox("Address delete failure", e);
                                    }
                                }
                                AddressView.this.refresh();
                            }
                        }
                    });
                AddressView.this.getUI().addWindow(confirmDialog);
            }
        });
        actionButtonHeader.addComponent(this.releaseAddressButton);

        Label spacer = new Label();
        spacer.setWidth("100%");
        actionButtonHeader.addComponent(spacer);
        actionButtonHeader.setExpandRatio(spacer, 1.0f);

        button = new Button("Refresh", new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                AddressView.this.refresh();
            }
        });
        button.setIcon(new ThemeResource("img/refresh.png"));
        actionButtonHeader.addComponent(button);

        this.addComponent(actionButtonHeader);
        this.addComponent(this.addressTable = this.createAddressTable());
        this.setExpandRatio(this.addressTable, 1.0f);
    }

    void refresh() {
        this.addressTable.setValue(null);
        this.addressTable.getContainerDataSource().removeAllItems();
        try {
            for (Address address : this.networkManager.getAddresses().getItems()) {
                this.addresses.addBean(new AddressBean(address));
            }
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Address list error", e);
        }
        this.valueChange(null);
    }

    Table createAddressTable() {
        this.addresses = new BeanContainer<String, AddressBean>(AddressBean.class);
        this.addresses.setBeanIdProperty("id");
        Table table = new Table();
        table.setContainerDataSource(this.addresses);

        table.setSizeFull();
        table.setPageLength(0);

        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setImmediate(true);

        table.addGeneratedColumn("location", new Util.LocationColumnGenerator());

        table.setVisibleColumns("ip", "instance", "privateIp", "provider", "location");
        table.setColumnHeader("ip", "IP address");
        table.setColumnHeader("privateIp", "private IP address");

        table.addValueChangeListener(this);

        return table;
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Set<?> selectedAddressIds = (Set<?>) this.addressTable.getValue();
        if (selectedAddressIds != null && selectedAddressIds.size() > 0) {
            if (selectedAddressIds.size() == 1) {
                Item address = this.addressTable.getItem(selectedAddressIds.iterator().next());
                String instance = (String) address.getItemProperty("instance").getValue();
                this.associateAddressButton.setEnabled(instance.isEmpty());
                this.disassociateAddressButton.setEnabled(!instance.isEmpty());
                this.releaseAddressButton.setEnabled(instance.isEmpty());
            } else {
                this.associateAddressButton.setEnabled(false);
                this.disassociateAddressButton.setEnabled(false);
                this.releaseAddressButton.setEnabled(true);
            }
        } else {
            this.associateAddressButton.setEnabled(false);
            this.disassociateAddressButton.setEnabled(false);
            this.releaseAddressButton.setEnabled(false);
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }

    public void updateAddress(Address address) {
        BeanItem<AddressBean> item = this.addresses.getItem(address.getUuid());
        if (item != null) {
            if (address.getState() != State.DELETED) {
                try {
                    address = this.networkManager.getAddressByUuid(address.getUuid());
                } catch (CloudProviderException e) {
                    return;
                }
            }
            AddressBean addressBean = item.getBean();
            addressBean.init(address);
            item.getItemProperty("instance").setValue(addressBean.getInstance());
            this.valueChange(null);
        }
    }

    public static class AddressBean {
        String id;

        String ip;

        String privateIp;

        String instance;

        String provider;

        String location;

        AddressBean(final Address address) {
            this.init(address);
        }

        void init(final Address address) {
            this.id = address.getUuid();
            this.ip = address.getIp();
            if (address.getResource() != null) {
                this.instance = address.getResource().getName();
            } else {
                this.instance = "";
            }
            this.privateIp = address.getInternalIp();
            this.provider = this.providerFrom(address);
            this.location = this.locationFrom(address);
        }

        public String getId() {
            return this.id;
        }

        public void setId(final String id) {
            this.id = id;
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

        public String getIp() {
            return this.ip;
        }

        public void setIp(final String ip) {
            this.ip = ip;
        }

        public String getPrivateIp() {
            return this.privateIp;
        }

        public void setPrivateIp(final String privateIp) {
            this.privateIp = privateIp;
        }

        public String getInstance() {
            return this.instance;
        }

        public void setInstance(final String instance) {
            this.instance = instance;
        }

        public String providerFrom(final Address address) {
            if (address.getCloudProviderAccount() != null) {
                return address.getCloudProviderAccount().getCloudProvider().getDescription();
            } else {
                return "";
            }
        }

        public String locationFrom(final Address address) {
            if (address.getLocation() != null) {
                return address.getLocation().description(true);
            } else {
                return "";
            }
        }
    }

}
