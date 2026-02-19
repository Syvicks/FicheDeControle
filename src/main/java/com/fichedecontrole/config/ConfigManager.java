package com.fichedecontrole.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Gestionnaire de configuration (classe utilitaire finale)
 * Charge les listes de valeurs depuis application.properties
 *
 * Thread-safe grâce à l'initialization-on-demand holder idiom.
 * Supporte la configuration externe (./config/application.properties)
 * avec fallback vers la configuration embarquée dans le JAR.
 */
public final class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    /**
     * Holder pour l'initialisation thread-safe des properties
     * (Initialization-on-demand holder idiom)
     */
    private static class PropertiesHolder {
        private static final Properties INSTANCE = loadProperties();

        private static Properties loadProperties() {
            Properties props = new Properties();

            // Tenter de charger depuis ./config/ d'abord (configuration externe)
            File externalConfig = new File("config/application.properties");
            if (externalConfig.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(externalConfig), StandardCharsets.UTF_8)) {
                    props.load(reader);
                    logger.info("Configuration chargee depuis : {}", externalConfig.getAbsolutePath());
                    return props;
                } catch (IOException e) {
                    logger.warn("Impossible de charger la configuration externe, fallback vers le classpath", e);
                }
            }

            // Sinon charger depuis le classpath (embarqué dans le JAR)
            try (InputStream input = ConfigManager.class.getResourceAsStream("/application.properties")) {
                if (input == null) {
                    logger.error("Fichier application.properties introuvable dans le classpath");
                    throw new RuntimeException("Fichier application.properties introuvable");
                }
                props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                logger.info("Configuration chargee depuis le classpath avec {} proprietes", props.size());
            } catch (IOException e) {
                logger.error("Erreur lors du chargement de application.properties", e);
            }

            return props;
        }
    }

    /**
     * Constructeur privé pour empêcher l'instanciation
     */
    private ConfigManager() {
        throw new UnsupportedOperationException("Classe utilitaire - ne peut pas être instanciee");
    }

    /**
     * Récupère une liste de valeurs depuis les properties
     * @param prefix Préfixe des clés (ex: "nature.demande")
     * @return Array de String avec toutes les valeurs
     */
    public static String[] getList(String prefix) {
        List<String> values = new ArrayList<>();
        int index = 0;

        while (true) {
            String key = prefix + "." + index;
            String value = PropertiesHolder.INSTANCE.getProperty(key);

            if (value == null) {
                break;
            }

            values.add(value);
            index++;
        }

        return values.toArray(new String[0]);
    }

    /**
     * Récupère une valeur simple
     * @param key La clé de la propriété
     * @return La valeur ou null si absente
     */
    public static String getValue(String key) {
        return PropertiesHolder.INSTANCE.getProperty(key);
    }

    /**
     * Récupère une valeur avec défaut
     * @param key La clé de la propriété
     * @param defaultValue La valeur par défaut
     * @return La valeur ou defaultValue si absente
     */
    public static String getValue(String key, String defaultValue) {
        return PropertiesHolder.INSTANCE.getProperty(key, defaultValue);
    }
}
