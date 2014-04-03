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

import org.ow2.sirocco.cloudmanager.MachineView.MachineBean;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.Machine;
import org.ow2.sirocco.cloudmanager.model.cimi.MachineDisk;
import org.ow2.sirocco.cloudmanager.model.cimi.MachineNetworkInterface;
import org.ow2.sirocco.cloudmanager.util.InputDialog;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class MachineDetailView extends VerticalLayout implements MetadataView.Callback {
    private static final long serialVersionUID = 1L;

    private Label title;

    private Table attributeTable;

    private MachineBean machineBean;

    private MachineView machineView;

    private MetadataView metadataView;

    public MachineDetailView(final MachineView machineView) {
        this.machineView = machineView;
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
            this.machineBean.machine = (Machine) MachineDetailView.this.machineView.machineManager.updateMachineAttributes(
                MachineDetailView.this.machineBean.getId(), updatedAttributes).getTargetResource();
        } catch (CloudProviderException e) {
            // TODO
            e.printStackTrace();
        }
        this.update(this.machineBean);
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
                                    MachineDetailView.this.machineBean.machine = (Machine) MachineDetailView.this.machineView.machineManager
                                        .updateMachineAttributes(MachineDetailView.this.machineBean.getId(), updatedAttributes)
                                        .getTargetResource();
                                } catch (CloudProviderException e) {
                                    // TODO
                                    e.printStackTrace();
                                }
                                MachineDetailView.this.machineBean = MachineDetailView.this.machineView.updateMachineAttribute(
                                    MachineDetailView.this.machineBean, attributeName, value);
                                MachineDetailView.this.update(MachineDetailView.this.machineBean);
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
        this.machineBean = null;
        this.setVisible(false);
    }

    public Machine getMachine() {
        return this.machineBean.machine;
    }

    public void update(final MachineBean machineBean) {
        this.setVisible(true);
        this.machineBean = machineBean;
        Machine machine = machineBean.machine;
        this.title.setValue("Machine " + machine.getName());
        this.attributeTable.getContainerDataSource().removeAllItems();
        this.index = 1;
        this.addAttribute("name", machineBean.getName(), true);
        this.addAttribute("description", machineBean.getDescription(), true);
        this.addAttribute("id", machine.getUuid(), false);
        this.addAttribute("tenant", machine.getTenant().getName(), false);
        if (machine.getCreated() != null) {
            this.addAttribute("created", machine.getCreated().toString(), false);
        }
        if (machine.getUpdated() != null) {
            this.addAttribute("updated", machine.getUpdated().toString(), false);
        }
        this.addAttribute("state", machine.getState().toString(), false);
        this.addAttribute("number of cpu", machine.getCpu().toString(), false);
        this.addAttribute("memory", Util.printKibibytesValue(machine.getMemory()), false);
        StringBuffer sb = new StringBuffer();
        if (machine.getDisks() != null) {
            for (MachineDisk disk : machine.getDisks()) {
                sb.append(Util.printKilobytesValue(disk.getCapacity()) + " ");
            }
            this.addAttribute("disk", sb.toString(), false);
        }
        if (machine.getNetworkInterfaces() != null) {
            int nicIndex = 0;
            for (MachineNetworkInterface nic : machine.getNetworkInterfaces()) {
                if (nic.getNetwork() != null) {
                    this.addAttribute("nic" + nicIndex, "Network: " + nic.getNetwork().getName(), false);
                }
                if (nic.getAddresses() != null && !nic.getAddresses().isEmpty()) {
                    if (nic.getAddresses().size() == 2) {
                        this.addAttribute("public IP", nic.getAddresses().get(1).getAddress().getIp(), false);
                        this.addAttribute("private IP", nic.getAddresses().get(0).getAddress().getIp(), false);
                    } else {
                        this.addAttribute("private IP", nic.getAddresses().get(0).getAddress().getIp(), false);
                    }
                }
                nicIndex++;
            }
        }
        if (machine.getImage() != null) {
            this.addAttribute("image", machine.getImage().getName(), false);
        }
        if (machine.getConfig() != null) {
            this.addAttribute("config", machine.getConfig().getName(), false);
        }
        this.addAttribute("provider", machine.getCloudProviderAccount().getCloudProvider().getDescription(), false);
        this.addAttribute("provider account id", machine.getCloudProviderAccount().getUuid(), false);
        this.addAttribute("provider-assigned id", machine.getProviderAssignedId(), false);
        this.addAttribute("location", machine.getLocation().description(false), false);

        this.metadataView.init(machine.getProperties());

    }

}
