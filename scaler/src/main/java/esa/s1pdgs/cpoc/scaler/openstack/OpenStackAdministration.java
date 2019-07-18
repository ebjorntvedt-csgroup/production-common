package esa.s1pdgs.cpoc.scaler.openstack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.compute.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import esa.s1pdgs.cpoc.common.errors.os.OsEntityException;
import esa.s1pdgs.cpoc.scaler.openstack.model.ServerDesc;
import esa.s1pdgs.cpoc.scaler.openstack.model.ServerDesc.ServerDescBuilder;
import esa.s1pdgs.cpoc.scaler.openstack.model.VolumeDesc;
import esa.s1pdgs.cpoc.scaler.openstack.services.ServerService;
import esa.s1pdgs.cpoc.scaler.openstack.services.VolumeService;

/**
 * @author Viveris Technologies
 */
@Service
public class OpenStackAdministration {

    /**
     * Logger
     */
    private static final Logger LOGGER =
            LogManager.getLogger(OpenStackAdministration.class);

    /**
     * OS client factory
     */
    private final OpenStackClientFactory osClientFactory;

    /**
     * Openstack properties
     */
    private final OpenStackServerProperties osProperties;

    /**
     * Service for managing servers
     */
    private final ServerService serverService;

    /**
     * Service for managing volumes
     */
    private final VolumeService volumeService;

    /**
     * Build the open stack client
     * 
     * @return
     */
    private OSClientV3 osClient() {
        return osClientFactory.osClient(osProperties);
    }

    /**
     * @param osProperties
     * @param serverService
     * @param volumeService
     */
    @Autowired
    public OpenStackAdministration(final OpenStackClientFactory osClientFactory,
            final OpenStackServerProperties osProperties,
            final ServerService serverService,
            final VolumeService volumeService) {
        this.osClientFactory = osClientFactory;
        this.osProperties = osProperties;
        this.serverService = serverService;
        this.volumeService = volumeService;
    }
    
    public String lookUpServerId(String serverName) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put("name", "^" + osProperties.getServerWrapper() + "*");
        for (Server server :  osClient().compute().servers().list()) {
            if (server.getName().equals(serverName)) {
            	return server.getId();
            }
        }
        
        return null;
    }

    /**
     * Remove the server with given identifier
     * 
     * @param serverId
     * @throws OsEntityException
     */
    public void deleteServer(final String serverId) throws OsEntityException {
        final OSClientV3 osClient = osClient();
        LOGGER.info("Deleting server '{}' via OpenStack API",serverId);
        final Server s = serverService.get(osClient, serverId);
        if (s == null) {
        	 LOGGER.warn("[serverId {}] Server was requested to delete, but does not exist", serverId);
        }
        final OpenStackServerProperties.ServerProperties serverProperties =
                osProperties.getServerWrapper();
        if (serverProperties.isFloatActivation()) {
            String floatingIPID =
                    serverService.getFloatingIpIdForServer(osClient, serverId);
            if (StringUtils.isEmpty(floatingIPID)) {
                LOGGER.warn("[serverId {}] No floating ip to delete", serverId);
            } else {
                LOGGER.debug("[serverId {}] Deleting floating ip {}", serverId,
                        floatingIPID);
                serverService.deleteFloatingIp(osClient, floatingIPID);
            }
        }
        serverService.delete(osClient, serverId);
        if (serverProperties.isBootableOnVolume()) {
            for (String v : s.getOsExtendedVolumesAttached()) {
                LOGGER.debug("[serverId {}] Deleting volume {}", serverId, v);
                volumeService.deleteVolume(osClient, v);
            }
        }
    }

    /**
     * Create a server named with given prefix
     * 
     * @param logPrefix
     * @param uniqueVMID
     * @return
     * @throws OsEntityException
     */
    public String createServerForL1Wrappers(final String logPrefix,
            final AtomicInteger uniqueVMID) throws OsEntityException {
        String vmID = uniqueVMID.getAndIncrement() + "-"
                + UUID.randomUUID().toString().substring(0, 4);
        return createServerForL1Wrappers(logPrefix, vmID);
    }

    /**
     * Create a server named with given prefix
     * 
     * @param logPrefix
     * @param uniqueVMID
     * @return
     * @throws OsEntityException
     */
    protected String createServerForL1Wrappers(final String logPrefix,
            String vmID) throws OsEntityException {
        OSClientV3 osClient = osClient();
        OpenStackServerProperties.VolumeProperties volumeProperties =
                osProperties.getVolumeWrapper();
        OpenStackServerProperties.ServerProperties serverProperties =
                osProperties.getServerWrapper();
        String serverName = serverProperties.getPrefixName() + "-" + vmID;
        String volumeName =
                volumeProperties.getPrefixName() + "-" + vmID + "-volume";

        // Create volume
        String volumeId = "";
        if (serverProperties.isBootableOnVolume()) {
            LOGGER.info("{} [serverName {}] Starting creating volume {}",
                    logPrefix, serverName, volumeName);
            VolumeDesc v = VolumeDesc.builder().name(volumeName).bootable(true)
                    .description(volumeProperties.getDescription())
                    .imageRef(serverProperties.getImageRef())
                    .size(volumeProperties.getSize())
                    .volumeType(volumeProperties.getVolumeType())
                    .zone(volumeProperties.getZone()).build();
            volumeId = volumeService.createVolumeAndBoot(osClient, v);
        }

        // Create server and boot on given volume
        LOGGER.info("{} [serverName {}] Starting creating server and booting",
                logPrefix, serverName);
        ServerDescBuilder builderS = ServerDesc.builder().name(serverName)
                .keySecurity(serverProperties.getKeySecurity())
                .securityGroups(serverProperties.getSecurityGroups())
                .flavor(serverProperties.getFlavor())
                .availableZone(serverProperties.getAvailableZone())
                .networks(serverProperties.getNetworks());
        if (serverProperties.isBootableOnVolume()) {
            builderS.bootOnVolumeInformation(volumeId,
                    serverProperties.getBootDeviceName());
        } else {
            builderS.imageRef(serverProperties.getImageRef());
        }
        String serverId =
                serverService.createAndBootServer(osClient, builderS.build());

        // Create floating IP
        if (serverProperties.isFloatActivation()) {
            LOGGER.info(
                    "{} [serverName {}] [serverId {}] Starting creating floating ip",
                    logPrefix, serverName, serverId);
            serverService.createFloatingIp(osClient, serverId,
                    serverProperties.getFloatingNetwork());
        }

        return serverId;
    }

    /**
     * Delete the invalid servers
     * 
     * @throws OsEntityException
     */
    public void deleteInvalidServers() throws OsEntityException {
        OSClientV3 osClient = this.osClient();
        OpenStackServerProperties.ServerProperties serverProperties =
                osProperties.getServerWrapper();
        Map<String, String> invalidServers = serverService.getServerIds(
                osClient, serverProperties.getPrefixName(), "ERROR");
        for (String serverId : invalidServers.keySet()) {
            LOGGER.info("Deletion of invalid server {}",
                    invalidServers.get(serverId));
            deleteServer(serverId);
        }

    }

    /**
     * delete the invalid volumes
     * 
     * @throws OsEntityException
     */
    public void deleteInvalidVolumes() throws OsEntityException {
        OSClientV3 osClient = this.osClient();
        OpenStackServerProperties.VolumeProperties volumeProperties =
                osProperties.getVolumeWrapper();
        Map<String, String> invalidVolumes = volumeService.getVolumeIds(
                osClient, volumeProperties.getPrefixName(), "ERROR");
        for (String volumeId : invalidVolumes.keySet()) {
            LOGGER.info("Deletion of invalid server {}",
                    invalidVolumes.get(volumeId));
            volumeService.deleteVolume(osClient, volumeId);
        }

    }

}
