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

import org.ow2.sirocco.cloudmanager.VolumeAttachDialog.MachineChoice;
import org.ow2.sirocco.cloudmanager.core.api.IMachineManager;
import org.ow2.sirocco.cloudmanager.core.api.IVolumeManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.Machine;
import org.ow2.sirocco.cloudmanager.model.cimi.MachineVolume;
import org.ow2.sirocco.cloudmanager.model.cimi.Volume;
import org.ow2.sirocco.cloudmanager.model.cimi.Volume.State;

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
public class VolumeView extends VerticalLayout implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Button attachVolumeButton;

    private Button detachVolumeButton;

    private Button deleteVolumeButton;

    private Table volumeTable;

    BeanContainer<String, VolumeBean> volumes;

    @Inject
    private VolumeCreationWizard volumeCreationWizard;

    @Inject
    private IVolumeManager volumeManager;

    @Inject
    private IMachineManager machineManager;

    public VolumeView() {
        this.setSizeFull();

        HorizontalLayout actionButtonHeader = new HorizontalLayout();
        actionButtonHeader.setMargin(true);
        actionButtonHeader.setSpacing(true);
        actionButtonHeader.setWidth("100%");
        actionButtonHeader.setHeight("50px");

        Button button = new Button("Create Volume...");
        button.setIcon(new ThemeResource("img/add.png"));
        button.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                if (VolumeView.this.volumeCreationWizard.init(VolumeView.this)) {
                    UI.getCurrent().addWindow(VolumeView.this.volumeCreationWizard);
                }
            }
        });
        actionButtonHeader.addComponent(button);

        this.attachVolumeButton = new Button("Attach");
        this.attachVolumeButton.setEnabled(false);
        this.attachVolumeButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                Set<?> selectedVolumeIds = (Set<?>) VolumeView.this.volumeTable.getValue();
                final String volumeId = (String) selectedVolumeIds.iterator().next();
                List<VolumeAttachDialog.MachineChoice> choices = new ArrayList<>();
                Volume volume;
                try {
                    volume = VolumeView.this.volumeManager.getVolumeByUuid(volumeId);
                    List<Machine> machines = VolumeView.this.machineManager.getMachines().getItems();
                    for (Machine machine : machines) {
                        if (machine.getCloudProviderAccount().getId() == volume.getCloudProviderAccount().getId()
                            && machine.getLocation().getId() == volume.getLocation().getId()) {
                            MachineChoice machineChoice = new MachineChoice();
                            machineChoice.id = machine.getUuid();
                            machineChoice.name = machine.getName();
                            choices.add(machineChoice);
                        }
                    }

                } catch (CloudProviderException e) {
                    Util.diplayErrorMessageBox("Internal error", e);
                }

                VolumeAttachDialog volumeAttachDialog = new VolumeAttachDialog(choices,
                    new VolumeAttachDialog.DialogCallback() {

                        @Override
                        public void response(final String machineId, final String location) {
                            try {
                                Volume volume = VolumeView.this.volumeManager.getVolumeByUuid(volumeId);
                                MachineVolume volumeAttachment = new MachineVolume();
                                volumeAttachment.setInitialLocation(location);
                                volumeAttachment.setVolume(volume);
                                VolumeView.this.machineManager.addVolumeToMachine(machineId, volumeAttachment);
                            } catch (CloudProviderException e) {
                                Util.diplayErrorMessageBox("Volume attach failure", e);
                            }
                        }
                    });
                UI.getCurrent().addWindow(volumeAttachDialog);
            }
        });
        actionButtonHeader.addComponent(this.attachVolumeButton);

        this.detachVolumeButton = new Button("Detach");
        this.detachVolumeButton.setEnabled(false);
        this.detachVolumeButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                Set<?> selectedVolumeIds = (Set<?>) VolumeView.this.volumeTable.getValue();
                String volumeId = (String) selectedVolumeIds.iterator().next();
                try {
                    Volume volume = VolumeView.this.volumeManager.getVolumeByUuid(volumeId);
                    MachineVolume volumeAttachment = volume.getAttachments().get(0);
                    VolumeView.this.machineManager.removeVolumeFromMachine(volumeAttachment.getOwner().getUuid(),
                        volumeAttachment.getUuid());
                } catch (CloudProviderException e) {
                    Util.diplayErrorMessageBox("Volume detach failure", e);
                }
            }
        });
        actionButtonHeader.addComponent(this.detachVolumeButton);

        this.deleteVolumeButton = new Button("Delete");
        this.deleteVolumeButton.setIcon(new ThemeResource("img/delete.png"));
        this.deleteVolumeButton.setEnabled(false);
        this.deleteVolumeButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                final Set<?> selectedVolumeIds = (Set<?>) VolumeView.this.volumeTable.getValue();
                String name = VolumeView.this.volumes.getItem(selectedVolumeIds.iterator().next()).getBean().getName();
                ConfirmDialog confirmDialog = ConfirmDialog.newConfirmDialog("Delete Volume",
                    "Are you sure you want to delete volume " + name + " ?", new ConfirmDialog.ConfirmationDialogCallback() {

                        @Override
                        public void response(final boolean ok, final boolean ignored) {
                            if (ok) {
                                for (Object id : selectedVolumeIds) {
                                    try {
                                        VolumeView.this.volumeManager.deleteVolume(id.toString());
                                    } catch (CloudProviderException e) {
                                        Util.diplayErrorMessageBox("Volume delete failure", e);
                                    }
                                }
                                VolumeView.this.valueChange(null);
                            }
                        }
                    });
                VolumeView.this.getUI().addWindow(confirmDialog);
            }
        });
        actionButtonHeader.addComponent(this.deleteVolumeButton);

        Label spacer = new Label();
        spacer.setWidth("100%");
        actionButtonHeader.addComponent(spacer);
        actionButtonHeader.setExpandRatio(spacer, 1.0f);

        button = new Button("Refresh", new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                VolumeView.this.refresh();
            }
        });
        button.setIcon(new ThemeResource("img/refresh.png"));
        actionButtonHeader.addComponent(button);

        this.addComponent(actionButtonHeader);
        this.addComponent(this.volumeTable = this.createVolumeTable());
        this.setExpandRatio(this.volumeTable, 1.0f);
    }

    void refresh() {
        this.volumeTable.setValue(null);
        this.volumeTable.getContainerDataSource().removeAllItems();
        try {
            for (Volume volume : this.volumeManager.getVolumes().getItems()) {
                this.volumes.addBean(new VolumeBean(volume));
            }
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Volume list error", e);
        }
        this.valueChange(null);
    }

    Table createVolumeTable() {
        this.volumes = new BeanContainer<String, VolumeBean>(VolumeBean.class);
        this.volumes.setBeanIdProperty("id");
        Table table = new Table();
        table.setContainerDataSource(this.volumes);

        table.setSizeFull();
        table.setPageLength(0);

        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setImmediate(true);

        table.addGeneratedColumn("state", new Util.StateColumnGenerator());
        table.addGeneratedColumn("location", new Util.LocationColumnGenerator());

        table.setVisibleColumns("name", "state", "capacity", "attachments", "provider", "location");

        table.addValueChangeListener(this);

        return table;
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Set<?> selectedVolumeIds = (Set<?>) this.volumeTable.getValue();
        if (selectedVolumeIds != null && selectedVolumeIds.size() > 0) {
            if (selectedVolumeIds.size() == 1) {
                Item volume = this.volumeTable.getItem(selectedVolumeIds.iterator().next());
                String state = (String) volume.getItemProperty("state").getValue();
                String attachment = (String) volume.getItemProperty("attachments").getValue();
                this.attachVolumeButton.setEnabled(state.endsWith("AVAILABLE") && attachment.isEmpty());
                this.detachVolumeButton.setEnabled(state.endsWith("USE"));
                this.deleteVolumeButton.setEnabled(attachment.isEmpty() && !state.endsWith("DELETING"));
            } else {
                this.attachVolumeButton.setEnabled(false);
                this.detachVolumeButton.setEnabled(false);
                boolean allowMultiDelete = true;
                for (Object volumeId : selectedVolumeIds) {
                    String state = (String) this.volumeTable.getItem(volumeId).getItemProperty("state").getValue();
                    if (state.endsWith("DELETING")) {
                        allowMultiDelete = false;
                        break;
                    }
                }
                this.deleteVolumeButton.setEnabled(allowMultiDelete);
            }
        } else {
            this.attachVolumeButton.setEnabled(false);
            this.detachVolumeButton.setEnabled(false);
            this.deleteVolumeButton.setEnabled(false);
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }

    public void updateVolume(Volume volume) {
        BeanItem<VolumeBean> item = this.volumes.getItem(volume.getUuid());
        if (item != null) {
            if (volume.getState() != State.DELETED) {
                try {
                    volume = this.volumeManager.getVolumeByUuid(volume.getUuid());
                } catch (CloudProviderException e) {
                    return;
                }
            }
            VolumeBean volumeBean = item.getBean();
            volumeBean.init(volume);
            item.getItemProperty("state").setValue(volumeBean.getState());
            item.getItemProperty("name").setValue(volumeBean.getName());
            this.valueChange(null);
        }
    }

    public static class VolumeBean {
        String id;

        String name;

        String state;

        String capacity;

        String attachments;

        String provider;

        String location;

        VolumeBean(final Volume volume) {
            this.init(volume);
        }

        void init(final Volume volume) {
            this.id = volume.getUuid();
            this.name = volume.getName();
            this.state = this.stateFrom(volume);
            this.capacity = Util.printKilobytesValue(volume.getCapacity());
            this.attachments = this.attachmentsFromVolume(volume);
            this.provider = this.providerFrom(volume);
            this.location = this.locationFrom(volume);
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

        public String getState() {
            return this.state;
        }

        public void setState(final String state) {
            this.state = state;
        }

        public String getCapacity() {
            return this.capacity;
        }

        public void setCapacity(final String capacity) {
            this.capacity = capacity;
        }

        public String getAttachments() {
            return this.attachments;
        }

        public void setAttachments(final String attachments) {
            this.attachments = attachments;
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

        private String attachmentsFromVolume(final Volume volume) {
            if (volume.getAttachments() != null && !volume.getAttachments().isEmpty()) {
                MachineVolume attachment = volume.getAttachments().get(0);
                return "Machine " + attachment.getOwner().getName();
            } else {
                return "";
            }
        }

        public String stateFrom(final Volume volume) {
            if (volume.getState() != Volume.State.DELETING && volume.getAttachments() != null
                && !volume.getAttachments().isEmpty()) {
                MachineVolume attachment = volume.getAttachments().get(0);
                if (attachment.getState().toString().endsWith("ING")) {
                    return attachment.getState().toString();
                }
                if (attachment.getState().toString().equals("ATTACHED")) {
                    return "IN_USE";
                }
                return attachment.getState().toString();
            }
            return volume.getState().toString();
        }

        public String providerFrom(final Volume volume) {
            if (volume.getCloudProviderAccount() != null) {
                return volume.getCloudProviderAccount().getCloudProvider().getDescription();
            } else {
                return "";
            }
        }

        public String locationFrom(final Volume volume) {
            if (volume.getLocation() != null) {
                return volume.getLocation().getIso3166_1();
            } else {
                return "";
            }
        }
    }

}
