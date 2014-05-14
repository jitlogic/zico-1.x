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

package com.jitlogic.zico.client;

import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.views.StatusBar;

/**
 * Created by rlewczuk on 14.05.14.
 */
public interface MessageDisplay {
    void info(String source, String msg);

    void error(String source, String msg);

    void error(String source, String msg, ServerFailure e);

    void message(String source, StatusBar.MessageType type, String msg);

    void clear(String source);
}
