package za.co.neroland.nerospace.mixin;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StorageAliasAuditTest {

    @Test
    void warningOnlyWhenAnInjectionIsMissing() {
        assertNull(StorageAliasAudit.warning(true, true, true));
        String miss = StorageAliasAudit.warning(true, false, true);
        assertTrue(miss.contains("holder-by-id=false"));
        assertTrue(miss.contains("nerolandcore"));
    }
}
