/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.client.views.traces;


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.api.TraceDataService;
import com.jitlogic.zico.shared.data.KeyValuePair;
import com.jitlogic.zico.shared.data.SymbolicExceptionInfo;
import com.jitlogic.zico.shared.data.TraceRecordInfo;
import com.jitlogic.zico.shared.data.TraceRecordListQuery;
import com.jitlogic.zico.widgets.client.IsPopupWindow;
import com.jitlogic.zico.widgets.client.MessageDisplay;
import com.jitlogic.zico.widgets.client.PopupWindow;
import com.jitlogic.zico.widgets.client.ZicoDataGridResources;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.*;

public class MethodAttrsDialog implements IsPopupWindow {
    interface MethodAttrsDialogUiBinder extends UiBinder<Widget, MethodAttrsDialog> { };
    private static MethodAttrsDialogUiBinder uiBinder = GWT.create(MethodAttrsDialogUiBinder.class);

    @UiField
    DockLayoutPanel container;

    @UiField(provided=true)
    DataGrid<String[]> attrGrid;

    @UiField
    TextArea txtAttrVal;

    @UiField
    Label lblAttrName;


    private ListDataProvider<String[]> attrStore;
    private SingleSelectionModel<String[]> selectionModel;

    private TraceDataService traceDataService;

    private PopupWindow window;

    private MessageDisplay md;

    @Inject
    public MethodAttrsDialog(TraceDataService traceDataService, MessageDisplay md,
                             @Assisted("hostName") String hostName, @Assisted Long dataOffs,
                             @Assisted String path, @Assisted("minTime") Long minTime) {

        this.traceDataService = traceDataService;
        this.md = md;

        setupGrid();

        window = new PopupWindow(uiBinder.createAndBindUi(this));

        window.setCaption("Trace Details");
        window.resizeAndCenter(900, 600);

        loadTraceDetail(hostName, dataOffs, path, minTime);
    }


    private static final String MDS = "MethodAttrsDialog";

    private void loadTraceDetail(String hostName, Long dataOffs, String path, Long minTime) {
        md.info(MDS, "Loading method attributes...");
        TraceRecordListQuery query = new TraceRecordListQuery();
        query.setHostName(hostName);
        query.setTraceOffs(dataOffs);
        query.setMinTime(minTime);
        query.setPath(path);

        traceDataService.getRecord(query, new MethodCallback<TraceRecordInfo>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Cannot load method/trace details", e);
            }

            @Override
            public void onSuccess(Method method, TraceRecordInfo tr) {
                fillTraceDetail(tr);
                md.clear(MDS);
            }
        });
    }


    private void fillTraceDetail(TraceRecordInfo tr) {
        List<String[]> attrs = new ArrayList<String[]>();

        StringBuilder sb = new StringBuilder();

        if (tr.getClock() != 0) {
            sb.append("Timestamp: ");
            sb.append(ClientUtil.TSTAMP_FORMAT2.format(new Date(tr.getClock())));
            sb.append("\n\n");
        }

        sb.append(tr.getMethod());
        sb.append("\n\n");


        if (tr.getAttributes() != null) {
            for (KeyValuePair e : tr.getAttributes()) {
                String key = e.getKey(), val = e.getValue() != null ? e.getValue() : "";
                attrs.add(new String[]{key, val});
                val = val.indexOf("\n") != -1 ? val.substring(0, val.indexOf('\n')) + "..." : val;
                if (val.length() > 80) {
                    val = val.substring(0, 80) + "...";
                }
                sb.append(key + "=" + val + "\n");
            }
        }

        Collections.sort(attrs, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[0].compareTo(o2[0]);
            }
        });

        if (attrs.size() > 0) {
            attrs.add(0, new String[]{"(all)", sb.toString()});
        }

        SymbolicExceptionInfo e = tr.getExceptionInfo();
        sb = new StringBuilder();
        while (e != null) {
            sb.append(e.getExClass() + ": " + e.getMessage() + "\n");
            for (String s : e.getStackTrace()) {
                sb.append(s + "\n");
            }

            e = e.getCause();

            if (e != null) {
                sb.append("\nCaused by: ");
            }
        }

        if (sb.length() > 0) {
            attrs.add(new String[]{"(exception)", sb.toString()});
        }

        if (attrs.size() > 0) {
            attrStore.getList().addAll(attrs);
            lblAttrName.setText("Selected attribute: " + attrs.get(0)[0]);
            txtAttrVal.setText(attrs.get(0)[1]);
        } else {
            txtAttrVal.setText(tr.getMethod() + "\n\n" + "This method has no attributes and hasn't thrown any exception.");
        }
    }

    private ProvidesKey<String[]> KEY_PROVIDER = new ProvidesKey<String[]>() {
        @Override
        public Object getKey(String[] item) {
            return item[0];
        }
    };

    private Cell<String> ATTR_CELL = new AbstractCell<String>() {
        @Override
        public void render(Context context, String value, SafeHtmlBuilder sb) {
            String color = "blue";
            if ("(all)".equals(value)) {
                color = "black";
            }
            if ("(exception)".equals(value)) {
                color = "red";
            }
            sb.appendHtmlConstant("<span style=\"color: " + color + "; font-size: small;\"><b>");
            sb.append(SafeHtmlUtils.fromString("" + value));
            sb.appendHtmlConstant("</b></span>");
        }
    };


    private void setupGrid() {
        attrGrid = new DataGrid<String[]>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<String[]>(KEY_PROVIDER);
        attrGrid.setSelectionModel(selectionModel);

        Column<String[], String> colAttribute = new Column<String[], String>(ATTR_CELL) {
            @Override
            public String getValue(String[] attr) {
                return attr[0];
            }
        };
        attrGrid.addColumn(colAttribute);
        attrGrid.setColumnWidth(colAttribute, 100, Style.Unit.PCT);

        attrStore = new ListDataProvider<String[]>(KEY_PROVIDER);
        attrStore.addDataDisplay(attrGrid);

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                String[] item = selectionModel.getSelectedObject();
                if (item != null) {
                    lblAttrName.setText("Selected attribute: " + item[0]);
                    txtAttrVal.setText(item[1]);
                }
            }
        });

        attrGrid.setSize("100%", "100%");
    }


    @Override
    public PopupWindow asPopupWindow() {
        return window;
    }


}
