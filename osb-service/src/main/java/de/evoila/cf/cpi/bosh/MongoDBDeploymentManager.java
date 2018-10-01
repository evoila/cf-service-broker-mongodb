package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

public class MongoDBDeploymentManager extends DeploymentManager {

    public static final String DATADIR = "datadir";
    public static final String REPLICA_SET_NAME = "replica-set-name";
    public static final String VERSION = "version";

    public MongoDBDeploymentManager(BoshProperties boshProperties, Environment environment){
        super(boshProperties, environment);
    }

    @Override
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan, Map<String, Object> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        if (customParameters != null && !customParameters.isEmpty())
            properties.putAll(customParameters);

        log.debug("Updating Deployment Manifest, replacing parameters");

        HashMap<String, Object> manifestProperties = (HashMap<String, Object>) manifest.getInstanceGroups().stream().findAny().get().getProperties();
        HashMap<String, Object> mongodb_exporter = (HashMap<String, Object>) manifestProperties.get("mongodb_exporter");
        HashMap<String, Object> mongodb = (HashMap<String, Object>) manifestProperties.get("mongodb");
        HashMap<String, Object> auth = (HashMap<String, Object>) mongodb.get("auth");
        HashMap<String, Object> replset = (HashMap<String, Object>) auth.get("replica-set");

        if(replset == null)
            auth.put("replica-set", new HashMap<>());

        String password = new RandomString(15).nextString();
        serviceInstance.setUsername(auth.get("user").toString());
        serviceInstance.setPassword(password);

        mongodb_exporter.put("password", password);
        auth.put("password", password);

        if(!replset.containsKey("keyfile")) {
            replset.put("keyfile", new RandomString(1024).nextString());
        }

        if(!properties.containsKey(REPLICA_SET_NAME)){
            properties.put(REPLICA_SET_NAME, "repSet");
        }

        replset.put("name", properties.get(REPLICA_SET_NAME));
        serviceInstance.getParameters().put("replicaSet", (String) properties.get(REPLICA_SET_NAME));

        if(properties.containsKey(DATADIR)){
            mongodb.put(DATADIR, properties.get(DATADIR));
        }

        if(properties.containsKey(VERSION)) {
            mongodb.put(VERSION, properties.get(VERSION));
        }

        this.updateInstanceGroupConfiguration(manifest, plan);
    }

}
