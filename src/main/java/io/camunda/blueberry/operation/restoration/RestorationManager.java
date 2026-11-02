package io.camunda.blueberry.operation.restoration;

import io.camunda.blueberry.exception.OperationException;
import io.camunda.blueberry.operation.backup.BackupCompleteness;
import io.camunda.blueberry.operation.backup.BackupManager;
import io.camunda.blueberry.platform.version.CamundaVersionProfile;
import io.camunda.blueberry.platform.version.CamundaVersionService;
import io.camunda.blueberry.platform.version.RestorePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestorationManager {

    private final BackupManager backupManager;
    private final CamundaVersionService versionService;
    private RestorationJob currentRestorationJob;
    Logger logger = LoggerFactory.getLogger(RestorationManager.class);

    public RestorationManager(BackupManager backupManager, CamundaVersionService versionService) {
        this.backupManager = backupManager;
        this.versionService = versionService;
    }

    public RestorePreflight.Result preflight(long backupId) throws OperationException {
        List<BackupCompleteness.Detail> details = backupManager.getBackupCompletenessDetails();
        return RestorePreflight.evaluate(backupId, details, versionService.getActiveProfile());
    }

    public List<RestorePhase> phasesForActiveVersion() {
        return versionService.getActiveProfile().restorePhases();
    }

    public synchronized void startRestoration(long backupId) throws OperationException {
        if (currentRestorationJob != null
                && currentRestorationJob.getJobStatus() == RestorationJob.JOBSTATUS.INPROGRESS) {
            throw new OperationException(null, 409, "Restore in progress",
                    "Restore already in progress for backup " + currentRestorationJob.getBackupId());
        }

        RestorePreflight.Result preflight = preflight(backupId);
        if (!preflight.allowed()) {
            throw new OperationException(null, 400, "Restore not allowed", preflight.message());
        }

        CamundaVersionProfile profile = versionService.getActiveProfile();
        RestorationJob job = new RestorationJob(backupId, profile);
        currentRestorationJob = job;

        Thread thread = new Thread(() -> {
            try {
                job.run();
            } catch (Exception e) {
                logger.error("Restore failed for backup {}", backupId, e);
            }
        });
        thread.start();
    }

    public RestorationJob getCurrentRestorationJob() {
        return currentRestorationJob;
    }
}
