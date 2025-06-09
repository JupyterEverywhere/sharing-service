package org.jupytereverywhere.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

/**
 * Service for retrieving application build information.
 */
@Log4j2
@Service
public class ApplicationInfoService {

    private final BuildProperties buildProperties;

    public ApplicationInfoService(@Autowired(required = false) BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * Gets the application version from build properties.
     * Falls back to a default if build properties are not available.
     * 
     * @return the application version
     */
    public String getVersion() {
        if (buildProperties != null) {
            return buildProperties.getVersion();
        }
        log.warn("BuildProperties not available, using fallback version");
        return "unknown";
    }

    /**
     * Gets the application name from build properties.
     * Falls back to a default if build properties are not available.
     * 
     * @return the application name
     */
    public String getName() {
        if (buildProperties != null) {
            return buildProperties.getName();
        }
        return "sharing-service";
    }

    /**
     * Gets the application group from build properties.
     * Falls back to a default if build properties are not available.
     * 
     * @return the application group
     */
    public String getGroup() {
        if (buildProperties != null) {
            return buildProperties.getGroup();
        }
        return "org.jupytereverywhere";
    }
}
