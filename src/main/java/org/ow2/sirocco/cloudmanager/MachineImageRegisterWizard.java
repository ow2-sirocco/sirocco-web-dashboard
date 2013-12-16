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

import org.ow2.sirocco.cloudmanager.MachineImageView.MachineImageBean;
import org.ow2.sirocco.cloudmanager.core.api.ICloudProviderManager;
import org.ow2.sirocco.cloudmanager.core.api.IMachineImageManager;
import org.ow2.sirocco.cloudmanager.core.api.IMachineManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.MachineImage;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderAccount;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderLocation;
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
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.steinwedel.messagebox.ButtonId;
import de.steinwedel.messagebox.Icon;
import de.steinwedel.messagebox.MessageBox;

@UIScoped
@SuppressWarnings("serial")
public class MachineImageRegisterWizard extends Window implements WizardProgressListener {
    private MachineImageView view;

    private Wizard wizard;

    private Util.PlacementStep placementStep;

    private ImageStep imageStep;

    @Inject
    private ICloudProviderManager providerManager;

    @Inject
    private IMachineManager machineManager;

    @Inject
    private IMachineImageManager machineImageManager;

    public MachineImageRegisterWizard() {
        super("Register Image");
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        this.wizard = new Wizard();
        this.wizard.addListener(this);
        this.wizard.addStep(this.placementStep = new Util.PlacementStep(this.wizard), "placement");
        this.wizard.addStep(this.imageStep = new ImageStep(), "Image attributes");
        this.wizard.setHeight("300px");
        this.wizard.setWidth("560px");

        content.addComponent(this.wizard);
        content.setComponentAlignment(this.wizard, Alignment.TOP_CENTER);
        this.setContent(content);
    }

    public boolean init(final MachineImageView view) {
        this.view = view;
        this.wizard.setUriFragmentEnabled(false);
        this.wizard.activateStep(this.placementStep);
        String tenantId = ((MyUI) UI.getCurrent()).getTenantId();

        this.placementStep.providerBox.removeAllItems();
        try {
            this.placementStep.setProviderManager(this.providerManager);
            for (CloudProviderAccount providerAccount : this.providerManager.getCloudProviderAccountsByTenant(tenantId)) {
                this.placementStep.providerBox.addItem(providerAccount.getUuid());
                this.placementStep.providerBox.setItemCaption(providerAccount.getUuid(), providerAccount.getCloudProvider()
                    .getDescription());
            }
            if (this.placementStep.providerBox.getItemIds().isEmpty()) {
                MessageBox.showPlain(Icon.ERROR, "No providers", "First add cloud providers", ButtonId.OK);
                return false;
            }
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Internal error", e);
        }

        return true;
    }

    @Override
    public void activeStepChanged(final WizardStepActivationEvent event) {
    }

    @Override
    public void stepSetChanged(final WizardStepSetChangedEvent event) {
    }

    private CloudProviderAccount getSelectedProviderAccount() throws CloudProviderException {
        String accountUuid = (String) MachineImageRegisterWizard.this.placementStep.providerBox.getValue();
        return this.providerManager.getCloudProviderAccountByUuid(accountUuid);
    }

    private CloudProviderLocation getSelectedLocation(final CloudProviderAccount account) {
        String locationConstraint = (String) MachineImageRegisterWizard.this.placementStep.locationBox.getValue();
        for (CloudProviderLocation loc : account.getCloudProvider().getCloudProviderLocations()) {
            if (loc.matchLocationConstraint(locationConstraint)) {
                return loc;
            }
        }
        return null;
    }

    @Override
    public void wizardCompleted(final WizardCompletedEvent event) {
        this.close();

        try {
            MachineImage image = new MachineImage();
            image.setName(this.imageStep.nameField.getValue());
            image.setDescription(this.imageStep.descriptionField.getValue());
            image.setProperties(new HashMap<String, String>());
            CloudProviderAccount account = this.getSelectedProviderAccount();
            CloudProviderLocation location = this.getSelectedLocation(account);
            image = this.machineImageManager.registerMachineImage(image, this.imageStep.providerAssignedIdField.getValue(),
                account, location);
            this.view.images.addBeanAt(0, new MachineImageBean(image));
        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("Image registration failure", e);
        }

    }

    @Override
    public void wizardCancelled(final WizardCancelledEvent event) {
        this.close();
    }

    private class ImageStep implements WizardStep {
        FormLayout content;

        TextField nameField;

        TextArea descriptionField;

        TextField providerAssignedIdField;

        ImageStep() {
            this.content = new FormLayout();
            this.content.setSizeFull();
            this.content.setMargin(true);

            this.nameField = new TextField("Name");
            this.nameField.setWidth("80%");
            this.nameField.setRequired(true);
            this.nameField.setRequiredError("Please provide a name");
            this.nameField.setImmediate(true);
            this.content.addComponent(this.nameField);
            this.nameField.addValueChangeListener(new Property.ValueChangeListener() {

                @Override
                public void valueChange(final ValueChangeEvent event) {
                    MachineImageRegisterWizard.this.wizard.updateButtons();
                }
            });

            this.descriptionField = new TextArea("Description");
            this.descriptionField.setWidth("80%");
            this.content.addComponent(this.descriptionField);

            this.providerAssignedIdField = new TextField("Provider-assigned ID");
            this.providerAssignedIdField.setWidth("80%");
            this.providerAssignedIdField.setRequired(true);
            this.providerAssignedIdField.setRequiredError("Please provide an ID");
            this.providerAssignedIdField.setImmediate(true);
            this.content.addComponent(this.providerAssignedIdField);
            this.providerAssignedIdField.addValueChangeListener(new Property.ValueChangeListener() {

                @Override
                public void valueChange(final ValueChangeEvent event) {
                    MachineImageRegisterWizard.this.wizard.updateButtons();
                }
            });
        }

        @Override
        public String getCaption() {
            return "Image Attributes";
        }

        @Override
        public Component getContent() {
            return this.content;
        }

        @Override
        public boolean onAdvance() {
            return !this.nameField.getValue().isEmpty();
        }

        @Override
        public boolean onBack() {
            return true;
        }

    }

}
