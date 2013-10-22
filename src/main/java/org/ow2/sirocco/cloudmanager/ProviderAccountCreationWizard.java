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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.core.api.ICloudProviderManager;
import org.ow2.sirocco.cloudmanager.core.api.ICloudProviderManager.CreateCloudProviderAccountOptions;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProvider;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderAccount;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderLocation;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderProfile;
import org.ow2.sirocco.cloudmanager.util.CountrySelector;
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
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@UIScoped
@SuppressWarnings("serial")
public class ProviderAccountCreationWizard extends Window implements WizardProgressListener {
    private Wizard wizard;

    private CloudProviderView view;

    private ProviderTypeStep providerTypeStep;

    private AccountParametersStep accountParametersStep;

    private ImportOptionsStep importOptionsStep;

    @Inject
    private ICloudProviderManager providerManager;

    public ProviderAccountCreationWizard() {
        super("New Cloud Provider Account");
        this.center();
        this.setClosable(false);
        this.setModal(true);
        this.setResizable(false);

        VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        this.wizard = new Wizard();
        this.wizard.addListener(this);

        this.wizard.addStep(this.providerTypeStep = new ProviderTypeStep(), "type");
        this.wizard.addStep(this.accountParametersStep = new AccountParametersStep(), "params");
        this.wizard.addStep(this.importOptionsStep = new ImportOptionsStep(), "import");
        this.wizard.setHeight("400px");
        this.wizard.setWidth("560px");

        content.addComponent(this.wizard);
        content.setComponentAlignment(this.wizard, Alignment.TOP_CENTER);
        this.setContent(content);
    }

    public void init(final CloudProviderView view) {
        this.view = view;
        this.wizard.activateStep(this.providerTypeStep);
        this.providerTypeStep.optionGroup.removeAllItems();
        for (CloudProviderProfile profile : this.providerManager.getCloudProviderProfiles()) {
            this.providerTypeStep.optionGroup.addItem(profile);
        }
    }

    @Override
    public void activeStepChanged(final WizardStepActivationEvent event) {
        if (event.getActivatedStep() == this.accountParametersStep) {
            CloudProviderProfile newProfile = (CloudProviderProfile) this.providerTypeStep.optionGroup.getValue();
            if (newProfile != this.accountParametersStep.profile) {
                this.accountParametersStep.fillForm(newProfile);
            }
        }
    }

    @Override
    public void stepSetChanged(final WizardStepSetChangedEvent event) {
    }

    @Override
    public void wizardCompleted(final WizardCompletedEvent event) {
        CloudProviderProfile selectedProfile = (CloudProviderProfile) this.providerTypeStep.optionGroup.getValue();

        CloudProvider provider = new CloudProvider();
        CloudProviderLocation location = new CloudProviderLocation();
        CloudProviderAccount account = new CloudProviderAccount();
        CreateCloudProviderAccountOptions options = new CreateCloudProviderAccountOptions();

        provider.setCloudProviderType(selectedProfile.getType());

        Map<String, String> properties = new HashMap<String, String>();
        for (Object id : this.accountParametersStep.item.getItemPropertyIds()) {
            String value = (String) this.accountParametersStep.item.getItemProperty(id).getValue();
            if (id == CloudProviderProfile.PROVIDER_ACCOUNT_LOGIN) {
                account.setLogin(value);
            } else if (id == CloudProviderProfile.PROVIDER_ACCOUNT_PASSWORD) {
                account.setPassword(value);
            } else if (id == "endpoint") {
                provider.setEndpoint(value);
            } else if (id == "description") {
                provider.setDescription(value);
            } else if (id == "location") {
                location.setIso3166_1(value);
                location.setCountryName((String) this.accountParametersStep.countrySelector.getCountryById(value)
                    .getItemProperty(CountrySelector.iso3166_PROPERTY_NAME).getValue());
            } else {
                properties.put((String) id, value);
            }

        }
        account.setProperties(properties);

        options.importMachineConfigs(this.importOptionsStep.importHardwareConfigs.getValue());
        options.importMachineImages(this.importOptionsStep.importImagesCheckBox.getValue());
        options.importOnlyOwnerMachineImages(this.importOptionsStep.importMyImagesOnly.getValue());
        options.importNetworks(this.importOptionsStep.importNetworks.getValue());

        ProviderAccountCreationWizard.this.wizard.disableButtons();

        try {
            CloudProviderAccount newAccount = null;
            List<CloudProvider> providers = this.providerManager.getCloudProviderByType(provider.getCloudProviderType());
            if (provider.getEndpoint() == null && !providers.isEmpty()) {
                newAccount = this.providerManager.createCloudProviderAccount(providers.get(0).getId().toString(), account,
                    options);
            } else {
                newAccount = this.providerManager.createCloudProviderAccount(provider, location, account, options);
            }
            this.providerManager.addCloudProviderAccountToTenant(((MyUI) UI.getCurrent()).getTenantId(), newAccount.getId()
                .toString());
        } catch (CloudProviderException e) {
            this.wizard.updateButtons();
            Util.diplayErrorMessageBox("Cannot create account", e);
            return;
        }

        this.close();
        this.view.refresh();
    }

    @Override
    public void wizardCancelled(final WizardCancelledEvent event) {
        this.close();
    }

    private class ProviderTypeStep implements WizardStep {
        VerticalLayout content;

        OptionGroup optionGroup;

        BeanItemContainer<CloudProviderProfile> container = new BeanItemContainer<CloudProviderProfile>(
            CloudProviderProfile.class);

        ProviderTypeStep() {
            this.content = new VerticalLayout();
            // this.content.setSizeFull();
            this.content.setMargin(true);
            this.content.setSpacing(true);

            this.content.addComponent(new Label("Select your Cloud Provider:"));
            this.optionGroup = new OptionGroup("", this.container);
            this.optionGroup.setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);
            this.optionGroup.setItemCaptionPropertyId("description");
            this.optionGroup.setImmediate(true);
            this.optionGroup.addValueChangeListener(new Property.ValueChangeListener() {

                @Override
                public void valueChange(final ValueChangeEvent event) {
                    ProviderAccountCreationWizard.this.wizard.updateButtons();
                }
            });

            this.content.addComponent(this.optionGroup);
        }

        @Override
        public String getCaption() {
            return "Provider Type";
        }

        @Override
        public Component getContent() {
            return this.content;
        }

        @Override
        public boolean onAdvance() {
            return this.optionGroup.getValue() != null;
        }

        @Override
        public boolean onBack() {
            return true;
        }

    }

    private class AccountParametersStep implements WizardStep {
        Panel panel;

        FormLayout content;

        PropertysetItem item;

        FieldGroup binder;

        CountrySelector countrySelector;

        CloudProviderProfile profile;

        AccountParametersStep() {
            this.panel = new Panel("Account Parameters");
            this.panel.setSizeFull();
            this.content = new FormLayout();
            // this.content.setSizeFull();
            this.content.setMargin(true);
            this.content.setSpacing(true);
            this.panel.setContent(this.content);
        }

        void fillForm(final CloudProviderProfile profile) {
            this.profile = profile;
            this.panel.setCaption(profile.getDescription());
            this.item = new PropertysetItem();
            this.binder = new FieldGroup(this.item);
            this.content.removeAllComponents();
            this.countrySelector = null;

            TextField tf = new TextField("Name");
            tf.setNullRepresentation("");
            tf.setWidth("100%");
            tf.setRequired(true);
            tf.setRequiredError("Please provide a name for the account");
            this.content.addComponent(tf);
            this.item.addItemProperty("description", new ObjectProperty<String>(""));
            this.binder.bind(tf, "description");

            CloudProviderProfile.AccountParameter param;
            if ((param = profile.findAccountParameter(CloudProviderProfile.PROVIDER_ENDPOINT)) != null) {
                this.countrySelector = new CountrySelector();
                this.content.addComponent(this.countrySelector);
                this.countrySelector.setRequired(true);
                this.countrySelector.setRequiredError("Please provide the location of the provider");
                this.item.addItemProperty("location", new ObjectProperty<String>(""));
                this.binder.bind(this.countrySelector, "location");

                this.content.addComponent(new Label("  "));

                tf = new TextField(param.getDescription());
                tf.setWidth("100%");
                tf.setNullRepresentation("");
                tf.setRequired(true);
                tf.setRequiredError("Please provide the endpoint of the provider");
                this.content.addComponent(tf);
                this.item.addItemProperty(CloudProviderProfile.PROVIDER_ENDPOINT, new ObjectProperty<String>(""));
                this.binder.bind(tf, CloudProviderProfile.PROVIDER_ENDPOINT);

                // tf = new TextField("Location");
                // tf.setWidth("100%");
                // tf.setRequired(true);
                // tf.setRequiredError("Please provide the location of the provider");
                // this.content.addComponent(tf);
                // this.item.addItemProperty("location", new ObjectProperty<String>(""));
                // this.binder.bind(tf, "location");
            }
            if ((param = profile.findAccountParameter(CloudProviderProfile.PROVIDER_ACCOUNT_LOGIN)) != null) {
                tf = new TextField(param.getDescription());
                tf.setWidth("100%");
                tf.setNullRepresentation("");
                tf.setRequired(true);
                tf.setRequiredError("Please provide a value");
                this.content.addComponent(tf);
                this.item.addItemProperty(CloudProviderProfile.PROVIDER_ACCOUNT_LOGIN, new ObjectProperty<String>(""));
                this.binder.bind(tf, CloudProviderProfile.PROVIDER_ACCOUNT_LOGIN);
            }
            if ((param = profile.findAccountParameter(CloudProviderProfile.PROVIDER_ACCOUNT_PASSWORD)) != null) {
                PasswordField pf = new PasswordField(param.getDescription());
                pf.setWidth("100%");
                pf.setNullRepresentation("");
                pf.setRequired(true);
                pf.setRequiredError("Please provide a value");
                this.content.addComponent(pf);
                this.item.addItemProperty(CloudProviderProfile.PROVIDER_ACCOUNT_PASSWORD, new ObjectProperty<String>(""));
                this.binder.bind(pf, CloudProviderProfile.PROVIDER_ACCOUNT_PASSWORD);
            }
            for (CloudProviderProfile.AccountParameter accountParam : profile.getAccountParameters()) {
                if (CloudProviderProfile.PROVIDER_ACCOUNT_PASSWORD.equals(accountParam.getAlias())) {
                    continue;
                }
                if (CloudProviderProfile.PROVIDER_ACCOUNT_LOGIN.equals(accountParam.getAlias())) {
                    continue;
                }
                if (CloudProviderProfile.PROVIDER_ENDPOINT.equals(accountParam.getAlias())) {
                    continue;
                }
                tf = new TextField(accountParam.getDescription());
                tf.setWidth("100%");
                tf.setNullRepresentation("");
                tf.setRequired(true);
                tf.setRequiredError("Please provide a value");
                this.content.addComponent(tf);
                this.item.addItemProperty(accountParam.getName(), new ObjectProperty<String>(""));
                this.binder.bind(tf, accountParam.getName());
            }
        }

        @Override
        public String getCaption() {
            return "Account parameters";
        }

        @Override
        public Component getContent() {
            return this.panel;
        }

        @Override
        public boolean onAdvance() {
            if (this.binder == null) {
                return false;
            }
            try {
                this.binder.commit();
            } catch (CommitException e) {
                return false;
            }
            if (this.countrySelector != null) {
                return this.countrySelector.getValue() != null && !((String) this.countrySelector.getValue()).isEmpty();
            }
            return true;
        }

        @Override
        public boolean onBack() {
            return true;
        }

    }

    private class ImportOptionsStep implements WizardStep {
        FormLayout content;

        CheckBox importImagesCheckBox;

        CheckBox importMyImagesOnly;

        CheckBox importHardwareConfigs;

        CheckBox importNetworks;

        ImportOptionsStep() {
            this.content = new FormLayout();
            this.content.setSizeFull();
            this.content.setMargin(true);

            this.importImagesCheckBox = new CheckBox("Import images");
            this.importImagesCheckBox.setValue(true);
            this.importImagesCheckBox.addValueChangeListener(new Property.ValueChangeListener() {

                @Override
                public void valueChange(final ValueChangeEvent event) {
                    ImportOptionsStep.this.importMyImagesOnly.setEnabled(ImportOptionsStep.this.importImagesCheckBox.getValue());
                }
            });
            this.content.addComponent(this.importImagesCheckBox);
            this.importMyImagesOnly = new CheckBox("Import only images owned by the account");
            this.importMyImagesOnly.setValue(false);
            this.content.addComponent(this.importMyImagesOnly);
            this.importHardwareConfigs = new CheckBox("Import hardware configurations");
            this.importHardwareConfigs.setValue(true);
            this.content.addComponent(this.importHardwareConfigs);
            this.importNetworks = new CheckBox("Import networks");
            this.importNetworks.setValue(true);
            this.content.addComponent(this.importNetworks);
        }

        @Override
        public String getCaption() {
            return "Import options";
        }

        @Override
        public Component getContent() {
            return this.content;
        }

        @Override
        public boolean onAdvance() {
            return true;
        }

        @Override
        public boolean onBack() {
            return true;
        }

    }

}
