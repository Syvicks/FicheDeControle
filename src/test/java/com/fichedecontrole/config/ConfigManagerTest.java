package com.fichedecontrole.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests pour ConfigManager
 */
class ConfigManagerTest {

    @Test
    void testGetList_shouldReturnArray() {
        String[] result = ConfigManager.getList("formule");
        assertThat(result).isNotEmpty();
        assertThat(result[0]).isEqualTo("TPSS");
    }

    @Test
    void testGetList_emptyPrefix_shouldReturnEmptyArray() {
        String[] result = ConfigManager.getList("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void testGetValue_existingKey_shouldReturnValue() {
        String result = ConfigManager.getValue("parametreur.0");
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
