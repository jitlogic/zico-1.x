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
package com.jitlogic.zico.core;


import com.jitlogic.zico.shared.data.UserInfo;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

@Singleton
public class UserManager {

    private final static Logger log = LoggerFactory.getLogger(UserManager.class);

    private DB db;
    private ConcurrentNavigableMap<String,UserInfo> users;

    private ZicoConfig config;

    private Object userRealm;

    private Method mUpdate, mRemove;

    private boolean sumode;

    private UserContext userContext;

    private DBFactory dbf;

    @Inject
    public UserManager(ZicoConfig config, UserContext userContext, DBFactory dbf) {

        this.config = config;
        this.userContext = userContext;
        this.dbf = dbf;

        try {
            Class<?> clazz = Class.forName("com.jitlogic.zico.main.ZicoLoginService");
            Method getInstance = clazz.getDeclaredMethod("getInstance");
            userRealm = getInstance.invoke(clazz);
            mUpdate = clazz.getDeclaredMethod("update", String.class, String.class, Boolean.class);
            mRemove = clazz.getDeclaredMethod("remove", String.class);
        } catch (Exception e) {
            log.warn("Cannot obtain reference to Jetty user realm: " + e.getMessage());
        }

        sumode = config.boolCfg("zico.su.mode", false);

        open();
    }


    public synchronized void open() {

        if (db != null) {
            return;
        }

        db = dbf.openDB(ZorkaUtil.path(config.getConfDir(), "users.db"));
        users = db.getTreeMap("USERS");

        File jsonFile = new File(config.getConfDir(), "users.json");

        if (users.size() == 0 && jsonFile.exists()) {
            log.info("User DB is empty but JSON dump file found. Importing...");
            Reader reader = null;
            try {
                reader = new FileReader(jsonFile);
                JSONObject json = new JSONObject(new JSONTokener(reader));
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    UserInfo user = fromJSON(json.getJSONObject(names.getString(i)));
                    users.put(user.getUserName(), user);
                }
                db.commit();
                log.info("User DB import finished successfully.");
            } catch (IOException e) {
                log.error("Cannot import user db from JSON data", e);
            } catch (JSONException e) {
                log.error("Cannot import user db from JSON data", e);
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (IOException e) { }
                }
            }
        }

        if (userRealm != null) {
            for (UserInfo user : users.values()) {
                updateRealm(user.getUserName(), user.getPassword(), user.isAdmin());
            }

            if (sumode || users.size() == 0) {
                log.info("SU mode enabled or user database is empty. Adding default 'admin' user with 'zico' password.");
                updateRealm("admin", "zico", true);
            }
        } else {
            log.error("Cannot initialize ZICO user realm. Logging in to collector will not be possible. Check jetty-web.xml for correctness.");
        }

    }

    private void updateRealm(String username, String password, boolean isAdmin) {
        if (userRealm != null && mUpdate != null) {
            try {
                mUpdate.invoke(userRealm, username, password, isAdmin);
            } catch (Exception e) {
                log.error("Cannot update user realm", e);
            }
        }
    }

    public synchronized void close() {
        if (db != null) {
            dbf.closeDB(db);
            db = null;
            users = null;
        }
    }


    public void export() {
        Writer writer = null;
        try {
            writer = new FileWriter(new File(config.getConfDir(), "users.json"));
            JSONObject obj = new JSONObject();
            for (Map.Entry<String,UserInfo> e : users.entrySet()) {
                obj.put(e.getKey(), toJSON(e.getValue()));
            }
            obj.write(writer);
        } catch (JSONException e) {
            log.error("Cannot export user DB", e);
        } catch (IOException e) {

        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException e) { }
            }
        }
    }


    public void checkHostAccess(String hostname) {
        if (!userContext.isInRole("ADMIN")
         && !userContext.getUser().getAllowedHosts().contains(hostname)) {
            throw new ZicoRuntimeException("Insufficient privileges");
        }
    }


    public UserInfo create() {
        return new UserInfo();
    }


    public UserInfo find(String username) {
        return users.get(username);
    }




    public List<UserInfo> findAll() {
        List<UserInfo> lst = new ArrayList<UserInfo>(users.size());
        lst.addAll(users.values());
        return lst;
    }


    public void persist(UserInfo user) {
        users.put(user.getUserName(), user);
        db.commit();

        updateRealm(user.getUserName(), user.getPassword(), user.isAdmin());
    }


    public void remove(UserInfo user) {
        if (userRealm != null && mRemove != null) {
            try {
                mRemove.invoke(userRealm, user.getUserName());
            } catch (Exception e) {
                log.error("Cannot remove user from realm", e);
            }
        }

        users.remove(user.getUserName());
        db.commit();
    }

    public static UserInfo fromJSON(JSONObject obj)  throws JSONException {
        UserInfo u = new UserInfo();
        u.setUserName(obj.getString("username"));
        u.setRealName(obj.getString("realname"));
        u.setAdmin(obj.getBoolean("admin"));
        u.setPassword(obj.optString("password"));

        JSONArray hosts = obj.getJSONArray("hosts");

        List<String> allowedHosts = new ArrayList<>();

        for (int i = 0; i < hosts.length(); i++) {
            allowedHosts.add(hosts.getString(i));
        }

        u.setAllowedHosts(allowedHosts);
        return u;
    }

    public static JSONObject toJSON(UserInfo u) {
        JSONObject json = new JSONObject();

        json.put("username", u.getUserName());
        json.put("realname", u.getRealName());
        json.put("password", u.getPassword());
        json.put("admin", u.isAdmin());

        JSONArray hosts = new JSONArray();

        for (String host : u.getAllowedHosts()) {
            hosts.put(host);
        }

        json.put("hosts", hosts);

        return json;
    }

}
