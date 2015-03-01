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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.jitlogic.zico.client.resources.Resources;
import com.jitlogic.zico.shared.data.KeyValuePair;
import com.jitlogic.zico.shared.data.SymbolicExceptionInfo;
import com.jitlogic.zico.shared.data.TraceRecordInfo;

public class MethodDetailCell extends AbstractCell<TraceRecordInfo> {

    private String methodAttributeKey = Resources.INSTANCE.zicoCssResources().methodAttributeKey();
    private String methodAttributeVal = Resources.INSTANCE.zicoCssResources().methodAttributeVal();
    private String methodErrorClassName = Resources.INSTANCE.zicoCssResources().methodErrorClassName();
    private String methodErrorMessage = Resources.INSTANCE.zicoCssResources().methodErrorMessage();
    private String methodErrorStack = Resources.INSTANCE.zicoCssResources().methodErrorStack();

    @Override
    public void render(Context context, TraceRecordInfo tr, SafeHtmlBuilder sb) {
        if (tr.getAttributes() != null) {
            sb.appendHtmlConstant("<table border=\"0\" cellspacing=\"2\"><tbody>");
            for (KeyValuePair e : tr.getAttributes()) {
                sb.appendHtmlConstant("<tr><td align=\"right\" class=\"" + methodAttributeKey + "\"><b>");
                sb.append(SafeHtmlUtils.fromString(e.getKey()));
                sb.appendHtmlConstant("</b></td><td><div class=\"" + methodAttributeVal + "\">");
                sb.append(SafeHtmlUtils.fromString(e.getValue() != null ? e.getValue().toString() : ""));
                sb.appendHtmlConstant("</div></td></tr>");
            }
            sb.appendHtmlConstant("</tbody></table>");
        }
        if (tr.getExceptionInfo() != null) {
            SymbolicExceptionInfo e = tr.getExceptionInfo();
            sb.appendHtmlConstant("<div class=\"" + methodErrorClassName + "\">");
            sb.append(SafeHtmlUtils.fromString("Caught: " + e.getExClass()));
            sb.appendHtmlConstant("</div>");
            sb.appendHtmlConstant("<div class=\"" + methodErrorMessage + "\">");
            sb.append(SafeHtmlUtils.fromString("" + e.getMessage()));
            sb.appendHtmlConstant("</b></div>");
            int i = 0;
            for (String s : e.getStackTrace()) {
                sb.appendHtmlConstant("<div class=\"" + methodErrorStack + "\">");
                sb.append(SafeHtmlUtils.fromString("" + s));
                sb.appendHtmlConstant("</div>");
                i++;
                if (i > 5) {
                    sb.appendHtmlConstant("<div class=\"" + methodErrorMessage + "\">");
                    sb.append(SafeHtmlUtils.fromString("...  (double click on this method to see full stack trace)"));
                    sb.appendHtmlConstant("</div>");
                    break;
                }
            }
        }
    }
}
