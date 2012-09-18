package brooklyn.entity.proxy.nginx;


import static java.lang.String.format;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.basic.lifecycle.ScriptHelper;
import brooklyn.entity.trait.Startable;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.NetworkUtils;
import brooklyn.util.task.Tasks;

/**
 * Start a {@link NginxController} in a {@link brooklyn.location.Location} accessible over ssh.
 */
public class NginxSshDriver extends AbstractSoftwareProcessSshDriver implements NginxDriver {
    public static final Logger log = LoggerFactory.getLogger(NginxSshDriver.class);

    protected boolean customizationCompleted = false;

    public NginxSshDriver(NginxController entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    protected String getLogFileLocation() {
        return format("%s/logs/error.log", getRunDir());
    }

    protected Integer getHttpPort() {
        return entity.getAttribute(NginxController.PROXY_HTTP_PORT);
    }

    @Override
    public void postLaunch() {
        entity.setAttribute(Attributes.HTTP_PORT, getHttpPort());
        super.postLaunch();
    }

    @Override
    public void install() {
        newScript("disable requiretty").
            setFlag("allocatePTY", true).
            body.append(CommonCommands.sudo("bash -c 'sed -i s/.*requiretty.*/#brooklyn-removed-require-tty/ /etc/sudoers'")).
            execute();
        
        String nginxUrl = format("http://nginx.org/download/nginx-%s.tar.gz", getVersion());
        String nginxSaveAs = format("nginx-%s.tar.gz", getVersion());
        String stickyModuleUrl = "http://nginx-sticky-module.googlecode.com/files/nginx-sticky-module-1.0.tar.gz";
        String stickyModuleSaveAs = "nginx-sticky-module-1.0.tar.gz";
        boolean sticky = ((NginxController) entity).isSticky();

        ScriptHelper script = newScript(INSTALLING);
        script.body.append(CommonCommands.INSTALL_TAR);
        MutableMap<String, String> installPackageFlags = MutableMap.of(
                "yum", "gcc make openssl-devel pcre-devel", 
                "apt", "gcc make libssl-dev zlib1g-dev libpcre3-dev",
                "port", null);
        script.body.append(CommonCommands.installPackage(installPackageFlags, "nginx-prerequisites"));
        script.body.append(CommonCommands.downloadUrlAs(nginxUrl, getEntityVersionLabel("/"), nginxSaveAs));
        script.body.append(format("tar xvzf %s", nginxSaveAs));
        script.body.append(format("cd %s/nginx-%s", getInstallDir(), getVersion()));

        if (sticky) {
            script.body.append("cd src");
            script.body.append(CommonCommands.downloadUrlAs(stickyModuleUrl, getEntityVersionLabel("/"), stickyModuleSaveAs));
            script.body.append(format("tar xvzf %s", stickyModuleSaveAs));
            script.body.append("cd ..");
        }

        script.body.append(
                "mkdir -p dist",
                "./configure"+
                    format(" --prefix=%s/nginx-%s/dist", getInstallDir(), getVersion()) +
                    " --with-http_ssl_module"+
                    (sticky ? format(" --add-module=%s/nginx-%s/src/nginx-sticky-module-1.0 ", getInstallDir(), getVersion()) : ""),
                "make install");

        script.header.prepend("set -x");
        script.gatherOutput();
        script.failOnNonZeroResultCode(false);
        int result = script.execute();
        
        if (result != 0) {
            String notes = "likely an error building nginx. consult the brooklyn log ssh output for further details.\n"+
                    "note that this Brooklyn nginx driver compiles nginx from source. " +
                    "it attempts to install common prerequisites but this does not always succeed.\n";
            OsDetails os = getMachine().getOsDetails();
            if (os.isMac()) {
                notes += "deploying to Mac OS X, you will require Xcode and Xcode command-line tools, and on " +
                		"some versions the pcre library (e.g. using macports, sudo port install pcre).\n";
            }
            if (os.isWindows()) {
                notes += "this nginx driver is not designed for windows, unless cygwin is installed, and you are patient.\n";
            }
            if (getEntity().getApplication().getClass().getCanonicalName().startsWith("brooklyn.demo.")) {
                // this is maybe naughty ... but since we use nginx in the first demo example,
                // and since it's actually pretty complicated, let's give a little extra hand-holding
                notes +=
                		"if debugging this is all a bit much and you just want to run a demo, " +
                		"you have two fairly friendly options.\n" +
                		"1. you can use a well known cloud, like AWS or Rackspace, where this should run " +
                		"in a tried-and-tested Ubuntu or CentOS environment, without any problems " +
                		"(and if it does let us know and we'll fix it!).\n"+
                		"2. or you can just use the demo without nginx, instead access the appserver instances directly.\n";
            }

            if (!script.getResultStderr().isEmpty())
                notes += "\n" + "STDERR\n" + script.getResultStderr()+"\n";
            if (!script.getResultStdout().isEmpty())
                notes += "\n" + "STDOUT\n" + script.getResultStdout()+"\n";
            
            Tasks.setExtraStatusDetails(notes.trim());
            
            throw new IllegalStateException("Installation of nginx failed (shell returned non-zero result "+result+")");
        }
    }

    @Override
    public void customize() {
        newScript(CUSTOMIZING).
            body.append(
                format("mkdir -p %s", getRunDir()),
                format("cp -R %s/nginx-%s/dist/{conf,html,logs,sbin} %s", getInstallDir(), getVersion(), getRunDir())
        ).execute();
        
        customizationCompleted = true;
        ((NginxController) entity).doExtraConfigurationDuringStart();
    }

    public boolean isCustomizationCompleted() {
        return customizationCompleted;
    }
    
    @Override
    public void launch() {
        // By default, nginx writes the pid of the master process to "logs/nginx.pid"
        NetworkUtils.checkPortsValid(MutableMap.of("httpPort", getHttpPort()));
        Map flags = MutableMap.of("usePidFile", false);

        newScript(flags, LAUNCHING).
                body.append(
                format("cd %s", getRunDir()),
                format("nohup ./sbin/nginx -p %s/ -c conf/server.conf > ./console 2>&1 &", getRunDir())
        ).execute();
    }

    @Override
    public boolean isRunning() {
        Map flags = MutableMap.of("usePidFile", "logs/nginx.pid");
        return newScript(flags, CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        // Don't `kill -9`, as that doesn't stop the worker processes
        String pidFile = "logs/nginx.pid";
        Map flags = MutableMap.of("usePidFile", false);

        newScript(flags, STOPPING).
                body.append(
                format("cd %s", getRunDir()),
                format("export PID=`cat %s`", pidFile),
                "[[ -n \"$PID\" ]] || exit 0",
                "kill $PID"
        ).execute();
    }

    @Override
    public void restart() {
        try {
            if (isRunning()) {
                stop();
            }
        } catch (Exception e) {
            log.debug(getEntity() + " stop failed during restart (but wasn't in stop state, so not surprising): " + e);
        }
        launch();
    }
    
    public void reload() {
        // FIXME There is a race on stop()+reload(). Nginx can simultaneously be stopping and also reconfiguring 
        // (e.g. due to a cluster-resize). The restart() call below means that nginx can be restarted after stop 
        // has executed. Looking at lifecycle reduces the race, but we need some proper synchronization...
        
        Lifecycle lifecycle = entity.getAttribute(NginxController.SERVICE_STATE);
        Boolean serviceUp = entity.getAttribute(Startable.SERVICE_UP);

        if (lifecycle==Lifecycle.STOPPING || lifecycle==Lifecycle.STOPPED || lifecycle==Lifecycle.DESTROYED) {
            log.debug("Ignoring reload of nginx "+entity+", becausing in state "+lifecycle);
            return;
        }
        
        if (serviceUp == null || serviceUp == false) {
            //if it hasn't come up completely then do restart instead
            log.debug("Reload of nginx "+entity+" is doing restart because has not come up");
            restart();
            return;
        }
        
        Map flags = MutableMap.of("usePidFile", "logs/nginx.pid");
        newScript(flags, RESTARTING).
                body.append(
                format("cd %s", getRunDir()),
                format("./sbin/nginx -p %s/ -c conf/server.conf -s reload", getRunDir())
        ).execute();
    }
}