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

import org.ow2.sirocco.cloudmanager.MachineImageView.MachineImageBean;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.MachineImage;
import org.ow2.sirocco.cloudmanager.util.InputDialog;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class MachineImageDetailView extends VerticalLayout implements MetadataView.Callback {
    private static final long serialVersionUID = 1L;

    private Label title;

    private Table attributeTable;

    private MachineImageBean machineImageBean;

    private MachineImageView machineImageView;

    private MetadataView metadataView;

    public MachineImageDetailView(final MachineImageView machineImageView) {
        this.machineImageView = machineImageView;
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
            this.machineImageBean.machineImage = this.machineImageView.machineImageManager.updateMachineImageAttributes(
                this.machineImageBean.getId(), updatedAttributes);
        } catch (CloudProviderException e) {
            // TODO
            e.printStackTrace();
        }
        this.update(this.machineImageBean);
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
                                    MachineImageDetailView.this.machineImageBean.machineImage = MachineImageDetailView.this.machineImageView.machineImageManager
                                        .updateMachineImageAttributes(MachineImageDetailView.this.machineImageBean.getId(),
                                            updatedAttributes);
                                } catch (CloudProviderException e) {
                                    // TODO
                                    e.printStackTrace();
                                }
                                MachineImageDetailView.this.machineImageBean = MachineImageDetailView.this.machineImageView
                                    .updateMachineImageAttribute(MachineImageDetailView.this.machineImageBean, attributeName,
                                        value);
                                MachineImageDetailView.this.update(MachineImageDetailView.this.machineImageBean);
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
        this.machineImageBean = null;
        this.setVisible(false);
    }

    public MachineImage getMachineImage() {
        return this.machineImageBean.machineImage;
    }

    public void update(final MachineImageBean machineImageBean) {
        this.setVisible(true);
        this.machineImageBean = machineImageBean;
        MachineImage machineImage = machineImageBean.machineImage;
        this.title.setValue("Image " + machineImage.getName());
        this.attributeTable.getContainerDataSource().removeAllItems();
        this.index = 1;
        this.addAttribute("name", machineImageBean.getName(), true);
        this.addAttribute("description", machineImageBean.getDescription(), true);
        this.addAttribute("id", machineImage.getUuid(), false);
        this.addAttribute("tenant", machineImage.getTenant().getName(), false);
        if (machineImage.getCreated() != null) {
            this.addAttribute("created", machineImage.getCreated().toString(), false);
        }
        if (machineImage.getUpdated() != null) {
            this.addAttribute("updated", machineImage.getUpdated().toString(), false);
        }
        this.addAttribute("state", machineImage.getState().toString(), false);
        this.addAttribute("architecture", machineImage.getArchitecture(), false);
        this.addAttribute("OS type", machineImage.getOsType(), false);
        if (machineImage.getCapacity() != null) {
            this.addAttribute("Capacity", Util.printKilobytesValue(machineImage.getCapacity()), false);
        }
        this.addAttribute("provider", machineImage.getProviderMappings().get(0).getProviderAccount().getCloudProvider()
            .getDescription(), false);
        this.addAttribute("provider account id", machineImage.getProviderMappings().get(0).getProviderAccount().getUuid(),
            false);
        this.addAttribute("provider-assigned id", machineImage.getProviderMappings().get(0).getProviderAssignedId(), false);
        this.addAttribute("location", machineImage.getProviderMappings().get(0).getProviderLocation().description(false), false);

        this.metadataView.init(machineImage.getProperties());

    }

}
