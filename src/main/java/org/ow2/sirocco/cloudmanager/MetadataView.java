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

import com.vaadin.data.Container;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class MetadataView extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private static final String ADD_METADATA_ITEM_ID = "addRow";

    public static interface Callback {
        void updateResourceMetadata(Map<String, String> metadata);
    }

    private Callback callback;

    private Map<String, String> metadata;

    private Table metadataTable;

    public MetadataView(final Callback callback) {
        this.callback = callback;
        this.setSizeFull();
        this.setMargin(true);
        this.metadataTable = new Table();
        this.metadataTable.setStyleName("metadata-table");
        this.addComponent(this.metadataTable);
        this.metadataTable.setPageLength(0);
        this.metadataTable.addContainerProperty("key", String.class, null);
        this.metadataTable.addContainerProperty("value", String.class, null);
        this.metadataTable.addGeneratedColumn("", new Table.ColumnGenerator() {
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                if (itemId.equals(MetadataView.ADD_METADATA_ITEM_ID)) {
                    Button addButton = new Button("Add");
                    addButton.addClickListener(new ClickListener() {
                        public void buttonClick(final ClickEvent event) {
                            String key = MetadataView.this.metadataTable.getItem(MetadataView.ADD_METADATA_ITEM_ID)
                                .getItemProperty("key").getValue().toString();
                            String value = MetadataView.this.metadataTable.getItem(MetadataView.ADD_METADATA_ITEM_ID)
                                .getItemProperty("value").getValue().toString();
                            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                                MetadataView.this.metadata.put(key, value);
                                MetadataView.this.callback.updateResourceMetadata(MetadataView.this.metadata);
                            }
                        }
                    });
                    return addButton;
                } else {
                    Button removeButton = new Button("x");
                    removeButton.addClickListener(new ClickListener() {
                        public void buttonClick(final ClickEvent event) {
                            String key = (String) MetadataView.this.metadataTable.getItem(itemId).getItemProperty("key")
                                .getValue();
                            MetadataView.this.metadata.remove(key);
                            MetadataView.this.callback.updateResourceMetadata(MetadataView.this.metadata);
                        }
                    });
                    return removeButton;
                }
            }
        });
        this.metadataTable.setTableFieldFactory(new TableFieldFactory() {

            @Override
            public Field<?> createField(final Container container, final Object itemId, final Object propertyId,
                final Component uiContext) {
                if (itemId.equals(MetadataView.ADD_METADATA_ITEM_ID)) {
                    if (propertyId.equals("key")) {
                        return new TextField();
                    } else if (propertyId.equals("value")) {
                        return new TextField();
                    }
                }
                return null;
            }
        });
        this.metadataTable.setEditable(true);

        this.metadataTable.setColumnWidth("key", 200);
        this.metadataTable.setColumnWidth("value", 200);
    }

    public void init(final Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata);
        this.metadataTable.getContainerDataSource().removeAllItems();
        for (Map.Entry<String, String> prop : this.metadata.entrySet()) {
            this.metadataTable.addItem(new Object[] {prop.getKey(), prop.getValue()}, prop.getKey());
        }
        this.metadataTable.addItem(new Object[] {"", ""}, MetadataView.ADD_METADATA_ITEM_ID);
    }

}
