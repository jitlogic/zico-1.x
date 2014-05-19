/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.DataGrid;

public interface ZicoDataGridResources extends DataGrid.Resources {

    public static ZicoDataGridResources INSTANCE = GWT.create(ZicoDataGridResources.class);

    @Source({DataGrid.Style.DEFAULT_CSS, "ZicoDataGrid.css"})
    DataGrid.Style dataGridStyle();
}

