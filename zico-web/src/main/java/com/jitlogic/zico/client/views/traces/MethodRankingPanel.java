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


import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.api.TraceDataService;
import com.jitlogic.zico.shared.data.MethodRankInfo;
import com.jitlogic.zico.shared.data.TraceInfo;
import com.jitlogic.zico.shared.data.TraceRecordRankQuery;
import com.jitlogic.zico.widgets.client.MessageDisplay;
import com.jitlogic.zico.widgets.client.ResizableHeader;
import com.jitlogic.zico.widgets.client.ZicoDataGridResources;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.List;

public class MethodRankingPanel extends Composite {
    interface MethodRankingPanelUiBinder extends UiBinder<Widget, MethodRankingPanel> { }
    private static MethodRankingPanelUiBinder ourUiBinder = GWT.create(MethodRankingPanelUiBinder.class);

    @UiField(provided = true)
    DataGrid<MethodRankInfo> rankGrid;

    private TraceDataService traceDataService;
    private TraceInfo traceInfo;
    private MessageDisplay md;

    private ListDataProvider<MethodRankInfo> rankStore;
    private SingleSelectionModel<MethodRankInfo> selectionModel;

    @Inject
    public MethodRankingPanel(TraceDataService traceDataService, MessageDisplay md, @Assisted TraceInfo traceInfo) {
        this.traceDataService = traceDataService;
        this.traceInfo = traceInfo;
        this.md = md;

        createRankingGrid();

        initWidget(ourUiBinder.createAndBindUi(this));

        loadData("calls", "DESC");
    }


    private final static ProvidesKey<MethodRankInfo> KEY_PROVIDER = new ProvidesKey<MethodRankInfo>() {
        @Override
        public Object getKey(MethodRankInfo item) {
            return item.getMethod();
        }
    };


    private void createRankingGrid() {

        rankGrid = new DataGrid<MethodRankInfo>(1024*1024, ZicoDataGridResources.INSTANCE, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<MethodRankInfo>(KEY_PROVIDER);
        rankGrid.setSelectionModel(selectionModel);

        Column<MethodRankInfo,String> colMethod = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return m.getMethod();
            }
        };
        rankGrid.addColumn(colMethod, new ResizableHeader<MethodRankInfo>("Method", rankGrid, colMethod));
        rankGrid.setColumnWidth(colMethod, 100, Style.Unit.PCT);

        Column<MethodRankInfo,String> colCalls = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return "" + m.getCalls();
            }
        };
        rankGrid.addColumn(colCalls, new ResizableHeader<MethodRankInfo>("Calls", rankGrid, colCalls));
        rankGrid.setColumnWidth(colCalls, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colErrors = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ""+m.getErrors();
            }
        };
        rankGrid.addColumn(colErrors, new ResizableHeader<MethodRankInfo>("Errors", rankGrid, colErrors));
        rankGrid.setColumnWidth(colErrors, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getTime());
            }
        };
        rankGrid.addColumn(colErrors, new ResizableHeader<MethodRankInfo>("Time", rankGrid, colTime));
        rankGrid.setColumnWidth(colTime, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colMinTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getMinTime());
            }
        };
        rankGrid.addColumn(colMinTime, new ResizableHeader<MethodRankInfo>("MinT", rankGrid, colMinTime));
        rankGrid.setColumnWidth(colMinTime, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colMaxTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getMaxTime());
            }
        };
        rankGrid.addColumn(colMaxTime, new ResizableHeader<MethodRankInfo>("MaxT", rankGrid, colMaxTime));
        rankGrid.setColumnWidth(colMaxTime, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colAvgTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getAvgTime());
            }
        };
        rankGrid.addColumn(colAvgTime, new ResizableHeader<MethodRankInfo>("AvgT", rankGrid, colAvgTime));
        rankGrid.setColumnWidth(colAvgTime, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colBareTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getBareTime());
            }
        };
        rankGrid.addColumn(colBareTime, new ResizableHeader<MethodRankInfo>("BT", rankGrid, colBareTime));
        rankGrid.setColumnWidth(colBareTime, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colMaxBareTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getMaxBareTime());
            }
        };
        rankGrid.addColumn(colMaxBareTime, new ResizableHeader<MethodRankInfo>("MaxBT", rankGrid, colMaxBareTime));
        rankGrid.setColumnWidth(colMaxBareTime, 50, Style.Unit.PX);

        Column<MethodRankInfo,String> colMinBareTime = new Column<MethodRankInfo, String>(new TextCell()) {
            @Override
            public String getValue(MethodRankInfo m) {
                return ClientUtil.formatDuration(m.getMinBareTime());
            }
        };
        rankGrid.addColumn(colMinBareTime, new ResizableHeader<MethodRankInfo>("MinBT", rankGrid, colMinBareTime));
        rankGrid.setColumnWidth(colMinBareTime, 50, Style.Unit.PX);

        rankStore = new ListDataProvider<MethodRankInfo>(KEY_PROVIDER);
        rankStore.addDataDisplay(rankGrid);

    }

    private static final String MDS = "MethodRankingPanel";

    private void loadData(String orderBy, String orderDir) {
        md.info(MDS, "Loading data...");

        TraceRecordRankQuery q = new TraceRecordRankQuery();
        q.setHostname(traceInfo.getHostName());
        q.setTraceOffs(traceInfo.getDataOffs());
        q.setOrderBy(orderBy);
        q.setOrderDesc(orderDir);

        traceDataService.rankRecords(q, new MethodCallback<List<MethodRankInfo>>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                md.error(MDS, "Error loading method rank data", e);
            }

            @Override
            public void onSuccess(Method method, List<MethodRankInfo> ranking) {
                rankStore.getList().clear();
                rankStore.getList().addAll(ranking);
                md.clear(MDS);
            }
        });
    }
}
