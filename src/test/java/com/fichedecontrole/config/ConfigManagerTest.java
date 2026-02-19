package com.fichedecontrole.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests pour ConfigManager
 */
class ConfigManagerTest {

    @Test
    void testGetList_shouldReturnArray() {
        String[] result = ConfigManager.getList("nature.demande");
        assertThat(result).isNotEmpty();
        // Vérifier que le premier élément contient "PG+PC" (évite les problèmes d'encodage UTF-8)
        assertThat(result[0]).contains("PG+PC");
    }

    @Test
    void testGetList_emptyPrefix_shouldReturnEmptyArray() {
        String[] result = ConfigManager.getList("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void testGetValue_existingKey_shouldReturnValue() {
        String result = ConfigManager.getValue("commentaire.sans.parametrage");
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void testGetValue_nonExistingKey_shouldReturnNull() {
        String result = ConfigManager.getValue("key.does.not.exist");
        assertThat(result).isNull();
    }

    @Test
    void testGetValue_withDefault_shouldReturnDefault() {
        String result = ConfigManager.getValue("key.does.not.exist", "default");
        assertThat(result).isEqualTo("default");
    }
}
