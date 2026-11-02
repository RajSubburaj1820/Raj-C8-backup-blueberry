package io.camunda.blueberry.operation.backup;

import io.camunda.blueberry.connect.BackupInfo;
import io.camunda.blueberry.connect.CamundaApplicationInt;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BackupManagerPartialTest {

    @Test
    void completeWhenAllRequiredComponentsPresent() {
        BackupInfo info = new BackupInfo();
        info.backupId = 10;
        info.status = BackupInfo.Status.COMPLETED;
        info.components = new HashSet<>(Set.of(
                CamundaApplicationInt.COMPONENT.OPERATE,
                CamundaApplicationInt.COMPONENT.TASKLIST,
                CamundaApplicationInt.COMPONENT.ZEEBE));

        Set<CamundaApplicationInt.COMPONENT> required = EnumSet.of(
                CamundaApplicationInt.COMPONENT.OPERATE,
                CamundaApplicationInt.COMPONENT.TASKLIST,
                CamundaApplicationInt.COMPONENT.ZEEBE);

        BackupCompleteness.Detail detail = BackupCompleteness.fromBackup(info, required);

        assertTrue(detail.complete());
        assertTrue(detail.missingComponents().isEmpty());
        assertEquals(3, detail.presentComponents().size());
    }

    @Test
    void incompleteWhenComponentMissing() {
        BackupInfo info = new BackupInfo();
        info.backupId = 11;
        info.status = BackupInfo.Status.COMPLETED;
        info.components = new HashSet<>(Set.of(CamundaApplicationInt.COMPONENT.OPERATE));

        Set<CamundaApplicationInt.COMPONENT> required = EnumSet.of(
                CamundaApplicationInt.COMPONENT.OPERATE,
                CamundaApplicationInt.COMPONENT.ZEEBE);

        BackupCompleteness.Detail detail = BackupCompleteness.fromBackup(info, required);

        assertFalse(detail.complete());
        assertEquals(1, detail.presentComponents().size());
        assertTrue(detail.missingComponents().contains("ZEEBE"));
    }

    @Test
    void partialBackupStatusAlignsWithMissingComponents() {
        BackupInfo info = new BackupInfo();
        info.backupId = 12;
        info.status = BackupInfo.Status.PARTIALBACKUP;
        info.components = new HashSet<>(Set.of(
                CamundaApplicationInt.COMPONENT.OPERATE,
                CamundaApplicationInt.COMPONENT.TASKLIST));

        Set<CamundaApplicationInt.COMPONENT> required = EnumSet.of(
                CamundaApplicationInt.COMPONENT.OPERATE,
                CamundaApplicationInt.COMPONENT.TASKLIST,
                CamundaApplicationInt.COMPONENT.ZEEBERECORD);

        BackupCompleteness.Detail detail = BackupCompleteness.fromBackup(info, required);

        assertFalse(detail.complete());
        assertTrue(detail.missingComponents().contains("ZEEBERECORD"));
    }
}
