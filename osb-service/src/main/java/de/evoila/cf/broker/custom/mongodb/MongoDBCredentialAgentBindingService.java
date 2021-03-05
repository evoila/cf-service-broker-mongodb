package de.evoila.cf.broker.custom.mongodb;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.ServiceInstanceBinding;
import de.evoila.cf.broker.model.ServiceInstanceBindingRequest;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.repository.*;
import de.evoila.cf.broker.service.AsyncBindingService;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.cpi.bosh.MongoDBBoshPlatformService;
import de.evoila.cf.security.credentials.agent.CredentialAgentClientHandler;
import de.evoila.cf.security.model.CreateCredentialAgentResponse;
import org.assertj.core.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "config.sb.credential_agent_enabled", havingValue = "true")
public class MongoDBCredentialAgentBindingService extends BindingServiceImpl {

    private static Logger log = LoggerFactory.getLogger(MongoDBBoshPlatformService.class);

    private MongoDBBoshPlatformService boshPlatformService;

    public MongoDBCredentialAgentBindingService(BindingRepository bindingRepository,
                                                ServiceDefinitionRepository serviceDefinitionRepository,
                                                ServiceInstanceRepository serviceInstanceRepository,
                                                RouteBindingRepository routeBindingRepository,
                                                JobRepository jobRepository,
                                                AsyncBindingService asyncBindingService,
                                                PlatformRepository platformRepository,
                                                MongoDBBoshPlatformService boshPlatformService) {
        super(bindingRepository, serviceDefinitionRepository, serviceInstanceRepository,
                routeBindingRepository, jobRepository, asyncBindingService, platformRepository);
        this.boshPlatformService = boshPlatformService;
    }

    @Override
    protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void unbindService(ServiceInstanceBinding binding,
                                 ServiceInstance serviceInstance,
                                 Plan plan) throws ServiceBrokerException {
        ServerAddress host = serviceInstance.getHosts().stream().findFirst().orElseThrow();
        CredentialAgentClientHandler agentClientHandler = new CredentialAgentClientHandler(host);
        agentClientHandler.deleteCredentials(binding.getId());
        agentClientHandler.shutdown();
    }

    @Override
    protected Map<String, Object> createCredentials(String bindingId,
                                                    ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance,
                                                    Plan plan,
                                                    ServerAddress serverAddress) throws PlatformException, ServiceBrokerException {

        //boshPlatformService.startCredentialAgent(serviceInstance);

        ServerAddress mongodbHost = serviceInstance.getHosts().stream().findFirst().orElse(serverAddress);

        CredentialAgentClientHandler agentClientHandler = new CredentialAgentClientHandler(mongodbHost);
        CreateCredentialAgentResponse createCredentialAgentResponse = agentClientHandler.putCredentials(bindingId);
        agentClientHandler.shutdown();

        return Maps.newHashMap("credhub-ref", createCredentialAgentResponse.getCredhubRef());
    }
}
