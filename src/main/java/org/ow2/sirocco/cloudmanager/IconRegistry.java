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

import com.vaadin.server.ThemeResource;

public class IconRegistry {
    private static IconRegistry instance;

    private ThemeResource loadingIcon;

    private ThemeResource greenBallIcon;

    private ThemeResource redBallIcon;

    private ThemeResource yellowBallIcon;

    private IconRegistry() {
    }

    public static IconRegistry getInstance() {
        if (IconRegistry.instance == null) {
            IconRegistry.instance = new IconRegistry();
        }
        return IconRegistry.instance;
    }

    public ThemeResource getLoadingIcon() {
        if (this.loadingIcon == null) {
            this.loadingIcon = new ThemeResource("img/loading.gif");
        }
        return this.loadingIcon;
    }

    public ThemeResource getGreenBallIcon() {
        if (this.greenBallIcon == null) {
            this.greenBallIcon = new ThemeResource("img/ball_green.gif");
        }
        return this.greenBallIcon;
    }

    public ThemeResource getRedBallIcon() {
        if (this.redBallIcon == null) {
            this.redBallIcon = new ThemeResource("img/ball_red.gif");
        }
        return this.redBallIcon;
    }

    public ThemeResource getYellowBallIcon() {
        if (this.yellowBallIcon == null) {
            this.yellowBallIcon = new ThemeResource("img/ball_yellow.gif");
        }
        return this.yellowBallIcon;
    }

}
