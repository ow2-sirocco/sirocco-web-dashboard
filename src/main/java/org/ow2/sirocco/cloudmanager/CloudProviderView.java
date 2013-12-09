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

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.core.api.ICloudProviderManager;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderAccount;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderLocation;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.CloudProviderProfile;

import com.vaadin.cdi.UIScoped;
import com.vaadin.data.Property;
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
import com.vaadin.ui.VerticalSplitPanel;

@UIScoped
public class CloudProviderView extends VerticalSplitPanel implements ValueChangeListener {
    private static final long serialVersionUID = 1L;

    private Table providerAccountTable;

    BeanContainer<String, CloudProviderAccountBean> providerAccounts;

    private ProviderAccountDetailView detailView;

    @Inject
    ICloudProviderManager cloudProviderManager;

    @Inject
    private ProviderAccountCreationWizard providerAccountCreationWizard;

    public CloudProviderView() {
        this.setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();

        HorizontalLayout actionButtonHeader = new HorizontalLayout();
        actionButtonHeader.setMargin(true);
        actionButtonHeader.setSpacing(true);
        actionButtonHeader.setWidth("100%");
        actionButtonHeader.setHeight("50px");

        Button button = new Button("Add Provider Account...");
        button.setIcon(new ThemeResource("img/add.png"));
        button.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                CloudProviderView.this.providerAccountCreationWizard.init(CloudProviderView.this);
                UI.getCurrent().addWindow(CloudProviderView.this.providerAccountCreationWizard);
            }
        });
        actionButtonHeader.addComponent(button);

        Label spacer = new Label();
        spacer.setWidth("100%");
        actionButtonHeader.addComponent(spacer);
        actionButtonHeader.setExpandRatio(spacer, 1.0f);

        button = new Button("Refresh", new ClickListener() {

            @Override
            public void buttonClick(final ClickEvent event) {
                CloudProviderView.this.refresh();
            }
        });
        button.setIcon(new ThemeResource("img/refresh.png"));
        actionButtonHeader.addComponent(button);

        verticalLayout.addComponent(actionButtonHeader);
        verticalLayout.addComponent(this.providerAccountTable = this.createCloudProviderAccountTable());
        verticalLayout.setExpandRatio(this.providerAccountTable, 1.0f);

        this.setFirstComponent(verticalLayout);
        this.setSecondComponent(this.detailView = new ProviderAccountDetailView(this));
        this.setSplitPosition(60.0f);
    }

    void refresh() {
        this.providerAccountTable.getContainerDataSource().removeAllItems();
        try {
            for (CloudProviderAccount providerAccount : this.cloudProviderManager.getCloudProviderAccountsByTenant(((MyUI) UI
                .getCurrent()).getTenantId())) {
                CloudProviderProfile profile = this.cloudProviderManager.getCloudProviderProfileByType(providerAccount
                    .getCloudProvider().getCloudProviderType());
                this.providerAccounts.addBean(new CloudProviderAccountBean(providerAccount, profile));
            }
        } catch (CloudProviderException e) {
            e.printStackTrace();
        }
        this.valueChange(null);
    }

    Table createCloudProviderAccountTable() {
        this.providerAccounts = new BeanContainer<String, CloudProviderAccountBean>(CloudProviderAccountBean.class);
        this.providerAccounts.setBeanIdProperty("id");

        Table table = new Table();
        // Add Table columns
        table.addContainerProperty("name", String.class, "");
        table.addContainerProperty("type", String.class, "");
        table.addContainerProperty("endpoint", String.class, "");
        table.addContainerProperty("login", String.class, "");
        table.addContainerProperty("locations", String.class, "");

        table.setContainerDataSource(this.providerAccounts);
        table.setSizeFull();
        table.setPageLength(0);

        table.addGeneratedColumn("locations", new LocationsColumnGenerator());

        table.setVisibleColumns("name", "type", "endpoint", "login", "locations");
        table.setSelectable(true);
        table.setImmediate(true);

        table.addValueChangeListener(this);

        return table;
    }

    private static class LocationsColumnGenerator implements Table.ColumnGenerator {
        public com.vaadin.ui.Component generateCell(final Table source, final Object itemId, final Object columnId) {
            Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
            String locations = (String) prop.getValue();
            Label label = new Label(locations, ContentMode.PREFORMATTED);
            return label;
        }
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {
        Object selectedAccountId = this.providerAccountTable.getValue();
        if (selectedAccountId != null) {
            this.detailView.update(this.providerAccounts.getItem(selectedAccountId).getBean());
        } else {
            this.detailView.hide();
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }

    CloudProviderAccountBean updateProviderAccountAttribute(final CloudProviderAccountBean accountBean, final String attribute,
        final String value) {
        this.providerAccountTable.getItem(accountBean.getId()).getItemProperty(attribute).setValue(value);
        return this.providerAccounts.getItem(accountBean.getId()).getBean();
    }

    public static class CloudProviderAccountBean {
        CloudProviderAccount account;

        CloudProviderProfile profile;

        String id;

        String type;

        String name;

        String endpoint;

        String login;

        String locations;

        public CloudProviderAccountBean(final CloudProviderAccount account, final CloudProviderProfile profile) {
            this.account = account;
            this.profile = profile;
            this.id = account.getUuid();
            this.type = account.getCloudProvider().getCloudProviderType();
            this.name = account.getCloudProvider().getDescription();
            this.endpoint = account.getCloudProvider().getEndpoint();
            this.login = account.getLogin();
            StringBuffer locBuffer = new StringBuffer();
            for (CloudProviderLocation location : account.getCloudProvider().getCloudProviderLocations()) {
                locBuffer.append(location.getCountryName());
                if (location.getStateName() != null) {
                    locBuffer.append(" (" + location.getStateName() + ")");
                }
                locBuffer.append("\n");
            }
            this.locations = locBuffer.toString();
        }

        public String getId() {
            return this.id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getType() {
            return this.type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getName() {
            return this.name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getEndpoint() {
            return this.endpoint;
        }

        public void setEndpoint(final String endpoint) {
            this.endpoint = endpoint;
        }

        public String getLogin() {
            return this.login;
        }

        public void setLogin(final String login) {
            this.login = login;
        }

        public String getLocations() {
            return this.locations;
        }

        public void setLocations(final String locations) {
            this.locations = locations;
        }

    }
}
