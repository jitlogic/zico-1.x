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
package com.jitlogic.zico.core.eql;


import org.parboiled.errors.ParseError;

import java.util.ArrayList;
import java.util.List;

public class EqlParseException extends RuntimeException {

    private List<ParseError> parseErrors;

    public EqlParseException(String msg, List<ParseError> parseErrors) {
        super(msg);

        this.parseErrors = new ArrayList<ParseError>(parseErrors.size());
        this.parseErrors.addAll(parseErrors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessage() + "\n");

        for (ParseError e : parseErrors) {
            sb.append("(at position " + e.getStartIndex() + ")");
        }

        return sb.toString();
    }
}
