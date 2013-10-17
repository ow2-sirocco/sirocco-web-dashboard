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

import java.util.HashMap;

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.VolumeView.VolumeBean;
import org.ow2.sirocco.cloudmanager.core.api.ICloudProviderManager;
import org.ow2.sirocco.cloudmanager.core.api.IVolumeManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.Job;
import org.ow2.sirocco.cloudmanager.model.cimi.Volume;
import org.ow2.sirocco.cloudmanager.model.cimi.VolumeConfiguration;
import org.ow2.sirocco.cloudmanager.model.cimi.VolumeCreate;
import org.ow2.sirocco.cloudmanager.model.cimi.VolumeTemplate;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderAccount;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.cdi.UIScoped;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.steinwedel.messagebox.ButtonId;
import de.steinwedel.messagebox.Icon;
import de.steinwedel.messagebox.MessageBox;

@UIScoped
@SuppressWarnings("serial")
public class VolumeCreationWizard extends Window implements WizardProgressListener {
    private VolumeView volumeView;

    private Wizard wizard;

    private Util.PlacementStep placementStep;

    private Util.MetadataStep metadataStep;

    private ConfigStep configStep;

    @Inject
    private ICloudProviderManager providerManager;

    @Inject
    private IVolumeManager volumeManager;

    public VolumeCreationWizard() {
        super("Volume Creation");
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        this.wizard = new Wizard();
        this.wizard.addListener(this);
        this.wizard.addStep(this.placementStep = new Util.PlacementStep(this.wizard), "placement");
        this.wizard.addStep(this.metadataStep = new Util.MetadataStep(this.wizard), "metadata");
        this.wizard.addStep(this.configStep = new ConfigStep(), "config");
        this.wizard.setHeight("300px");
        this.wizard.setWidth("560px");

        content.addComponent(this.wizard);
        content.setComponentAlignment(this.wizard, Alignment.TOP_CENTER);
        this.setContent(content);
    }

    public boolean init(final VolumeView volumeView) {
        this.volumeView = volumeView;
        this.wizard.setUriFragmentEnabled(false);
        this.wizard.activateStep(this.placementStep);
        String tenantId = ((MyUI) UI.getCurrent()).getTenantId();

        this.placementStep.providerBox.removeAllItems();
        try {
            this.placementStep.setProviderManager(this.providerManager);
            for (CloudProviderAccount providerAccount : this.providerManager.getCloudProviderAccountsByTenant(tenantId)) {
                this.placementStep.providerBox.addItem(providerAccount.getId().toString());
                this.placementStep.providerBox.setItemCaption(providerAccount.getId().toString(), providerAccount
                    .getCloudProvider().getDescription());
            }
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Internal error", e);
        }
        if (this.placementStep.providerBox.getItemIds().isEmpty()) {
            MessageBox.showPlain(Icon.ERROR, "No providers", "First add cloud providers", ButtonId.OK);
            return false;
        }

        this.metadataStep.nameField.setValue("");
        this.metadataStep.descriptionField.setValue("");
        return true;
    }

    @Override
    public void activeStepChanged(final WizardStepActivationEvent event) {
        if (event.getActivatedStep() == this.metadataStep) {
            this.metadataStep.nameField.focus();
        }
    }

    @Override
    public void stepSetChanged(final WizardStepSetChangedEvent event) {
    }

    @Override
    public void wizardCompleted(final WizardCompletedEvent event) {
        this.close();

        VolumeCreate volumeCreate = new VolumeCreate();
        volumeCreate.setProperties(new HashMap<String, String>());

        try {
            String accountId = (String) this.placementStep.providerBox.getValue();
            volumeCreate.getProperties().put("providerAccountId", accountId);
            volumeCreate.getProperties().put("location", (String) this.placementStep.locationBox.getValue());
            volumeCreate.setName(this.metadataStep.nameField.getValue());
            volumeCreate.setDescription(this.metadataStep.descriptionField.getValue());
            if (volumeCreate.getDescription().isEmpty()) {
                volumeCreate.setDescription(null);
            }

            VolumeTemplate volumeTemplate = new VolumeTemplate();
            VolumeConfiguration volumeConfig = new VolumeConfiguration();
            volumeConfig.setCapacity(Integer.valueOf(this.configStep.capacityField.getValue()) * 1000 * 1000);
            volumeTemplate.setVolumeConfig(volumeConfig);

            volumeCreate.setVolumeTemplate(volumeTemplate);

            Job job = this.volumeManager.createVolume(volumeCreate);
            Volume newVolume = (Volume) job.getAffectedResources().get(0);

            this.volumeView.volumes.addBeanAt(0, new VolumeBean(newVolume));

        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Volume creation failure", e);
        }
    }

    @Override
    public void wizardCancelled(final WizardCancelledEvent event) {
        this.close();
    }

    private class ConfigStep implements WizardStep {
        FormLayout content;

        TextField capacityField;

        ConfigStep() {
            this.content = new FormLayout();
            this.content.setSizeFull();
            this.content.setMargin(true);

            // ObjectProperty<Integer> property = new
            // ObjectProperty<Integer>(0);

            this.capacityField = new TextField("Capacity (GB)");
            this.capacityField.setImmediate(true);
            this.capacityField.setRequired(true);
            // this.capacityField.setRequiredError("Please provide a capacity");
            // this.capacityField.setConverter(new StringToIntegerConverter());
            // this.capacityField.setPropertyDataSource(property);
            this.content.addComponent(this.capacityField);
            this.capacityField.addValueChangeListener(new Property.ValueChangeListener() {

                @Override
                public void valueChange(final ValueChangeEvent event) {
                    VolumeCreationWizard.this.wizard.updateButtons();
                }
            });
        }

        @Override
        public String getCaption() {
            return "Config";
        }

        @Override
        public Component getContent() {
            return this.content;
        }

        @Override
        public boolean onAdvance() {
            if (this.capacityField.getValue() == null) {
                return false;
            }
            try {
                int capacity = Integer.valueOf(this.capacityField.getValue());
                if (capacity <= 0) {
                    this.capacityField.setComponentError(new UserError("Enter a positive capacity"));
                    return false;
                }
                this.capacityField.setComponentError(null);
                return true;
            } catch (NumberFormatException e) {
                this.capacityField.setComponentError(new UserError("Enter a number"));
                return false;
            }
        }

        @Override
        public boolean onBack() {
            return true;
        }

    }

}
