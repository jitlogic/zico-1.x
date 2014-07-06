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
package com.jitlogic.zico.main;



import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.EnumSet;
import java.util.Properties;

public class ZicoMain {

    private int port;
    private String homeDir;

    private Server server;
    private WebAppContext webapp;

    private Properties props;


    public static void main(String[] args) throws Exception {
        new ZicoMain().run();
    }


    public void run() throws Exception {
        configure();

        initServer();
        initSecurity();

        server.start();
        server.join();
    }


    private void initServer() {
        server = new Server(port);
        ProtectionDomain domain = Server.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();

        webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setDescriptor(location.toExternalForm() + "/WEB-INF/web.xml");
        webapp.setServer(server);
        webapp.setWar(location.toExternalForm());
        webapp.setTempDirectory(new File(homeDir, "tmp"));

        server.setHandler(webapp);
    }


    private void initFormSecurity() {
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__FORM_AUTH);
        constraint.setRoles(new String[]{"VIEWER","ADMIN"});
        constraint.setAuthenticate(true);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/*");

        FormAuthenticator authenticator = new FormAuthenticator("/login.html", "/login-fail.html", false);

        LoginService service = new ZicoLoginService();


        ConstraintSecurityHandler handler = new ConstraintSecurityHandler();
        handler.addConstraintMapping(mapping);
        handler.setLoginService(service);
        handler.setAuthenticator(authenticator);

        webapp.setSecurityHandler(handler);
    }


    private void initCasSecurity() {
        FilterHolder authFilter = webapp.addFilter(
                "org.jasig.cas.client.authentication.AuthenticationFilter", "/*",
                EnumSet.of(DispatcherType.REQUEST));

        authFilter.setInitParameter("casServerLoginUrl", props.getProperty("auth.cas.url") + "/login");
        authFilter.setInitParameter("service", props.getProperty("auth.slac.url"));

        FilterHolder validationFilter = webapp.addFilter(
                "org.jasig.cas.client.validation.Cas10TicketValidationFilter", "/*",
                EnumSet.of(DispatcherType.REQUEST)
        );

        validationFilter.setInitParameter("casServerUrlPrefix", props.getProperty("auth.cas.url"));
        validationFilter.setInitParameter("service", props.getProperty("auth.slac.url"));

        webapp.addFilter(
                "org.jasig.cas.client.util.HttpServletRequestWrapperFilter", "/*",
                EnumSet.of(DispatcherType.REQUEST));

    }


    private void initSecurity() {
        String auth = props.getProperty("auth", "form").trim();

        if ("anonymous".equalsIgnoreCase(auth)) {
            System.err.println("Starting SLAC without any authentication (fake user).");
        } else if ("form".equalsIgnoreCase(auth)) {
            System.err.println("Starting SLAC with FORM security.");
            initFormSecurity();
        } else if ("cas".equalsIgnoreCase(auth)) {
            System.err.println("Starting SLAC with CAS security.");
            initCasSecurity();
        }

    }


    private void configure() throws IOException {

        homeDir = System.getProperty("zico.home.dir");

        if (homeDir == null) {
            System.err.println("ERROR: Missing home dir property: add -Dzico.home.dir=<path-to-collector-home> to JVM args.");
            System.exit(1);
        }

        String strPort = System.getProperty("zico.http.port", "8642").trim();

        props = new Properties();

        InputStream fis = null;
        try {
            fis = new FileInputStream(new File(homeDir, "zico.properties"));
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Cannot open zico.properties file: " + e.getMessage());
            System.exit(1);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        strPort = props.getProperty("zico.http.port", strPort).trim();

        try {
            port = Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            System.err.println("Invalid HTTP port setting (not a number): " + strPort);
            System.exit(1);
        }
    }

}
