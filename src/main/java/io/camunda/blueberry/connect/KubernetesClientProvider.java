package io.camunda.blueberry.connect;

import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.exception.OperationException;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared Fabric8 client for Kubernetes API calls (restore, exploration, configuration rules).
 */
@Component
public class KubernetesClientProvider {

    private final BlueberryConfig blueberryConfig;
    private KubernetesClient client;
    private final Logger logger = LoggerFactory.getLogger(KubernetesClientProvider.class);

    public KubernetesClientProvider(BlueberryConfig blueberryConfig) {
        this.blueberryConfig = blueberryConfig;
    }

    public synchronized KubernetesClient client() throws OperationException {
        if (client != null) {
            return client;
        }
        try {
            String kubeConfig = blueberryConfig.getKubeConfig();
            if (kubeConfig != null && !kubeConfig.isBlank()) {
                logger.info("Kubernetes client from kubeconfig {}", kubeConfig);
                String contents = Files.readString(Path.of(kubeConfig));
                Config config = Config.fromKubeconfig(contents);
                client = new KubernetesClientBuilder().withConfig(config).build();
            } else {
                logger.info("Kubernetes client from default kubeconfig / in-cluster config");
                client = new KubernetesClientBuilder().build();
            }
            return client;
        } catch (Exception e) {
            throw new OperationException(OperationException.BLUEBERRYERRORCODE.KUBERNETES_CLIENT, 500,
                    "Kubernetes connection failed", e.getMessage());
        }
    }

    /**
     * Is the Kubernetes API reachable, and usable in the namespace? List one deployment of the namespace.
     * The version of the cluster is not used: the client can't read the version answer of recent clusters (emulationMajor).
     */
    public boolean ping() {
        try {
            client().apps().deployments().inNamespace(blueberryConfig.getNamespace())
                    .list(new ListOptionsBuilder().withLimit(1L).build());
            return true;
        } catch (Exception e) {
            logger.debug("Kubernetes ping failed: {}", e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
