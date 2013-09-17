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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.ow2.sirocco.cloudmanager.core.api.IUserManager;
import org.ow2.sirocco.cloudmanager.core.api.IdentityContext;
import org.ow2.sirocco.cloudmanager.core.api.exception.CloudProviderException;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.Tenant;
import org.ow2.sirocco.cloudmanager.model.cimi.extension.User;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.cdi.CDIUI;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletService;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@CDIUI
@Theme("mytheme")
@Push(PushMode.MANUAL)
@SuppressWarnings("serial")
public class MyUI extends UI {
    private VerticalLayout inventoryContainer;

    @Inject
    private MachineView machineView;

    @Inject
    private MachineImageView machineImageView;

    @Inject
    private VolumeView volumeView;

    @Inject
    private NetworkView networkView;

    @Inject
    private IUserManager userManager;

    @Inject
    private IdentityContext identityContext;

    private String userName;

    private String tenantId;

    private ExecutorService executorService;

    private Poller pollingTask;

    @Override
    protected void init(final VaadinRequest request) {
        this.userName = request.getUserPrincipal().getName();
        this.tenantId = "1";
        this.identityContext.setUserName(this.userName);
        this.identityContext.setTenantId(this.tenantId);

        this.getPage().setTitle("Sirocco Dashboard");
        final VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        this.setContent(layout);

        // Top header *********************
        HorizontalLayout header = new HorizontalLayout();
        header.setMargin(true);
        header.setWidth("100%");
        header.setHeight("70px");
        header.setStyleName("topHeader");

        // logo
        Resource res = new ThemeResource("img/sirocco_small_logo.png");
        Image image = new Image(null, res);
        header.addComponent(image);

        // spacer
        Label spacer = new Label();
        spacer.setWidth("100%");
        header.addComponent(spacer);
        header.setExpandRatio(spacer, 1.0f);

        HorizontalLayout rightButtons = new HorizontalLayout();
        rightButtons.setStyleName("topHeader");
        rightButtons.setSpacing(true);

        this.userName = request.getUserPrincipal().getName();
        User user = null;
        try {
            user = this.userManager.getUserByUsername(this.userName);
        } catch (CloudProviderException e) {
            e.printStackTrace();
        }

        Label label = new Label("Tenant:");
        label.setStyleName("topHeaderLabel");
        rightButtons.addComponent(label);
        final ComboBox tenantSelect = new ComboBox();
        tenantSelect.setTextInputAllowed(false);
        tenantSelect.setNullSelectionAllowed(false);
        for (Tenant tenant : user.getTenants()) {
            tenantSelect.addItem(tenant.getName());
        }
        tenantSelect.setValue(user.getTenants().iterator().next().getName());
        tenantSelect.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(final ValueChangeEvent event) {
                Notification.show("Switching to tenant " + tenantSelect.getValue());

            }
        });
        tenantSelect.setImmediate(true);
        rightButtons.addComponent(tenantSelect);

        // logged user name

        label = new Label("Logged in as: " + this.userName);
        label.setStyleName("topHeaderLabel");
        rightButtons.addComponent(label);

        // sign out button
        Button button = new Button("Sign Out");
        // button.setStyleName(BaseTheme.BUTTON_LINK);
        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(final ClickEvent event) {
                MyUI.this.logout();
            }
        });
        rightButtons.addComponent(button);

        header.addComponent(rightButtons);
        layout.addComponent(header);

        // Split view
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSizeFull();
        splitPanel.setFirstComponent(this.createLeftMenu());

        this.inventoryContainer = new VerticalLayout();
        this.inventoryContainer.setSizeFull();

        this.inventoryContainer.addComponent(this.machineView);

        splitPanel.setSecondComponent(this.inventoryContainer);
        splitPanel.setSplitPosition(15);

        layout.addComponent(splitPanel);
        layout.setExpandRatio(splitPanel, 1.0f);

        this.executorService = Executors.newSingleThreadExecutor();
        this.pollingTask = new Poller();
        this.executorService.submit(this.pollingTask);

    }

    Accordion createLeftMenu() {
        Accordion accordion = new Accordion();
        accordion.setStyleName("menuTitle");
        accordion.setSizeFull();
        accordion.addTab(this.createResourceTree(), "Resources", null);
        accordion.addTab(this.createProviderTree(), "Providers", null);
        accordion.addTab(this.createUserTree(), "Users & Tenants", null);
        return accordion;
    }

    Tree createResourceTree() {
        final Tree resourceTree = new Tree("Resources");
        resourceTree.setSizeFull();
        resourceTree.addItem("Machines");
        resourceTree.setChildrenAllowed("Machines", false);
        resourceTree.addItem("Images");
        resourceTree.setChildrenAllowed("Images", false);
        resourceTree.addItem("Volumes");
        resourceTree.setChildrenAllowed("Volumes", false);
        resourceTree.addItem("Networks");
        resourceTree.setChildrenAllowed("Networks", false);
        resourceTree.addItem("KeyPairs");
        resourceTree.setChildrenAllowed("KeyPairs", false);

        resourceTree.select("Machines");
        resourceTree.setImmediate(true);
        resourceTree.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(final ValueChangeEvent event) {
                switch ((String) resourceTree.getValue()) {
                case "Machines":
                    MyUI.this.inventoryContainer.replaceComponent(MyUI.this.inventoryContainer.getComponent(0),
                        MyUI.this.machineView);
                    break;
                case "Images":
                    MyUI.this.inventoryContainer.replaceComponent(MyUI.this.inventoryContainer.getComponent(0),
                        MyUI.this.machineImageView);
                    break;
                case "Volumes":
                    MyUI.this.inventoryContainer.replaceComponent(MyUI.this.inventoryContainer.getComponent(0),
                        MyUI.this.volumeView);
                    break;
                case "Networks":
                    MyUI.this.inventoryContainer.replaceComponent(MyUI.this.inventoryContainer.getComponent(0),
                        MyUI.this.networkView);
                    break;
                }
            }
        });
        return resourceTree;
    }

    Tree createProviderTree() {
        Tree resourceTree = new Tree("Providers");
        resourceTree.setSizeFull();
        resourceTree.addItem("Providers");
        resourceTree.setChildrenAllowed("Providers", false);
        return resourceTree;
    }

    Tree createUserTree() {
        Tree resourceTree = new Tree("Users");
        resourceTree.setSizeFull();
        resourceTree.addItem("Users");
        resourceTree.setChildrenAllowed("Users", false);
        resourceTree.addItem("Tenants");
        resourceTree.setChildrenAllowed("Tenants", false);
        return resourceTree;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    @Override
    public void detach() {
        this.pollingTask.cancel();
        this.executorService.shutdownNow();
        System.out.println("DETACHED");
        super.detach();
    }

    private void logout() {
        this.getUI().getSession().close();
        // UI.getCurrent().close();

        // Invalidate underlying session instead if login info is stored there
        VaadinService.getCurrentRequest().getWrappedSession().invalidate();

        // Redirect to avoid keeping the removed UI open in the browser
        this.getUI().getPage().setLocation(VaadinServletService.getCurrentServletRequest().getContextPath() + "/logout.jsp");
    }

    class Poller implements Runnable {
        volatile boolean cancelled;

        void cancel() {
            this.cancelled = true;
        }

        @Override
        public void run() {
            while (!this.cancelled) {
                try {
                    Thread.sleep(1000 * 10);
                } catch (InterruptedException e) {
                    break;
                }
                MyUI.this.access(new Runnable() {
                    @Override
                    public void run() {
                        // System.out.println("POLL start");
                        try {
                            MyUI.this.machineView.pollMachines();
                            MyUI.this.volumeView.pollVolumes();
                            MyUI.this.networkView.pollNetworks();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // System.out.println("POLL end");
                    }
                });
            }

        }
    }

}
