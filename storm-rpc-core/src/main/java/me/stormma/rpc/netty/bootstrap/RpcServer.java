package me.stormma.rpc.netty.bootstrap;

import com.google.common.collect.Maps;
import me.stormma.annoation.Provider;
import me.stormma.rpc.model.ServerInfo;
import me.stormma.rpc.registry.ServiceRegistry;
import me.stormma.rpc.utils.ServiceNameUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author stormma stormmaybin@gmail.com
 */
public class RpcServer implements Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private Map<String, Object> providerBeans = Maps.newConcurrentMap();

    private final ServiceRegistry serviceRegistry;

    private final ServerInfo serverInfo;

    public RpcServer(ServiceRegistry serviceRegistry, ServerInfo serverInfo) {
        this.serviceRegistry = serviceRegistry;
        this.serverInfo = serverInfo;
    }

    @Override
    public void start(String basePackage) {
        registerProviderBean2Map(basePackage);
        registerProviderService2Registry();
    }

    @Override
    public void close() {
        serviceRegistry.shutdown();
    }

    private void registerProviderBean2Map(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> providerClass = reflections.getTypesAnnotatedWith(Provider.class);
        for (Class<?> clazz : providerClass) {
            Provider provider = clazz.getAnnotation(Provider.class);
            Class<?> interfaceClass = provider.interfaceClass();
            String version = provider.version();
            String serviceName = ServiceNameUtils.getServiceName(interfaceClass, version);
            Object providerBean;
            try {
                 providerBean = clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            providerBeans.put(serviceName, providerBean);
        }
    }

    private void registerProviderService2Registry() {
        String serviceAddress = ServiceNameUtils.getServiceAddress(this.serverInfo);
        for (String serviceName : providerBeans.keySet()) {
            serviceRegistry.register(serviceName, serverInfo);
        }
    }
}