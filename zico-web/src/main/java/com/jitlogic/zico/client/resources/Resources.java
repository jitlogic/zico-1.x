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
package com.jitlogic.zico.client.resources;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundle {

    public final static Resources INSTANCE = GWT.create(Resources.class);

    @Source("Zico.css")
    ZicoCssResources zicoCssResources();


    @Source("../images/add.png")
    ImageResource addIcon();

    @Source("../images/attachment.png")
    ImageResource attachment();

    @Source("../images/backup.png")
    ImageResource backupIcon();

    @Source("../images/cancel.png")
    ImageResource cancel();

    @Source("../images/clear.png")
    ImageResource clearIcon();

    @Source("../images/clock.png")
    ImageResource clockIcon();

    @Source("../images/date-next.png")
    ImageResource dateNext();

    @Source("../images/date-previous.png")
    ImageResource datePrev();

    @Source("../images/disable.png")
    ImageResource disableIcon();

    @Source("../images/edit.png")
    ImageResource editIcon();

    @Source("../images/enable.png")
    ImageResource enableIcon();

    @Source("../images/eql.png")
    ImageResource eqlIcon();

    @Source("../images/error-mark.png")
    ImageResource errorMarkIcon();

    @Source("../images/exception-thrown.png")
    ImageResource exceptionIcon();

    @Source("../images/expander-collapse.png")
    ImageResource expanderCollapse();

    @Source("../images/expander-expand.png")
    ImageResource expanderExpand();

    @Source("../images/expand.png")
    ImageResource expandIcon();

    @Source("../images/filter.png")
    ImageResource filterIcon();

    @Source("../images/goto.png")
    ImageResource gotoIcon();

    @Source("../images/go-down-search.png")
    ImageResource goDownIcon();

    @Source("../images/go-next.png")
    ImageResource goNextIcon();

    @Source("../images/go-previous.png")
    ImageResource goPrevIcon();

    @Source("../images/go-up-search.png")
    ImageResource goUpIcon();

    @Source("../images/key.png")
    ImageResource keyIcon();

    @Source("../images/lightning-go.png")
    ImageResource lightningGoIcon();

    @Source("../images/list-columns.png")
    ImageResource listColumnsIcon();

    @Source("../images/logout.png")
    ImageResource logoutIcon();

    @Source("../images/method-attrs.png")
    ImageResource methodAttrsIcon();

    @Source("../images/method-rank.png")
    ImageResource methodRankIcon();

    @Source("../images/method-tree.png")
    ImageResource methodTreeIcon();

    @Source("../images/msgbox-ok.png")
    ImageResource msgBoxOkIcon();

    @Source("../images/refresh.png")
    ImageResource refreshIcon();

    @Source("../images/remove.png")
    ImageResource removeIcon();

    @Source("../images/search.png")
    ImageResource searchIcon();

    @Source("../images/sort.png")
    ImageResource sort();

    @Source("../images/time.png")
    ImageResource timeIcon();

    @Source("../images/treePlus.png")
    ImageResource treeMinusIcon();

    @Source("../images/treeMinusSlim.png")
    ImageResource treeMinusSlimIcon();

    @Source("../images/treeMinus.png")
    ImageResource treePlusIcon();

    @Source("../images/treePlusSlim.png")
    ImageResource treePlusSlimIcon();

    @Source("../images/users.png")
    ImageResource usersIcon();

    @Source("../images/zoom-in-5.png")
    ImageResource zoomIn();

    @Source("../images/zoom-out-5.png")
    ImageResource zoomOut();
}
