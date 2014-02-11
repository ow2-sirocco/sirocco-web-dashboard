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

import org.ow2.sirocco.cloudmanager.SecurityGroupView.SecurityGroupBean;
import org.ow2.sirocco.cloudmanager.core.api.ICloudProviderManager;
import org.ow2.sirocco.cloudmanager.core.api.INetworkManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.Job;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderAccount;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroup;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.SecurityGroupCreate;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.cdi.UIScoped;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.steinwedel.messagebox.ButtonId;
import de.steinwedel.messagebox.Icon;
import de.steinwedel.messagebox.MessageBox;

@UIScoped
@SuppressWarnings("serial")
public class SecurityGroupCreationWizard extends Window implements WizardProgressListener {
    private SecurityGroupView secGroupView;

    private Wizard wizard;

    private Util.PlacementStep placementStep;

    private Util.MetadataStep metadataStep;

    @Inject
    private ICloudProviderManager providerManager;

    @Inject
    private INetworkManager networkManager;

    public SecurityGroupCreationWizard() {
        super("SecurityGroup Creation");
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
        this.wizard.setHeight("300px");
        this.wizard.setWidth("560px");

        content.addComponent(this.wizard);
        content.setComponentAlignment(this.wizard, Alignment.TOP_CENTER);
        this.setContent(content);
    }

    public boolean init(final SecurityGroupView secGroupView) {
        this.secGroupView = secGroupView;
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

        SecurityGroupCreate secGroupCreate = new SecurityGroupCreate();
        secGroupCreate.setProperties(new HashMap<String, String>());

        try {
            String accountId = (String) this.placementStep.providerBox.getValue();
            secGroupCreate.setProviderAccountId(accountId);
            secGroupCreate.setLocation((String) this.placementStep.locationBox.getValue());
            secGroupCreate.setName(this.metadataStep.nameField.getValue());
            secGroupCreate.setDescription(this.metadataStep.descriptionField.getValue());
            if (secGroupCreate.getDescription().isEmpty()) {
                secGroupCreate.setDescription(null);
            }

            Job job = this.networkManager.createSecurityGroup(secGroupCreate);
            SecurityGroup newSecurityGroup = (SecurityGroup) job.getAffectedResources().get(0);

            this.secGroupView.securityGroups.addBeanAt(0, new SecurityGroupBean(newSecurityGroup));

        } catch (CloudProviderException e) {
            Util.diplayErrorMessageBox("SecurityGroup creation failure", e);
        }
    }

    @Override
    public void wizardCancelled(final WizardCancelledEvent event) {
        this.close();
    }

}
