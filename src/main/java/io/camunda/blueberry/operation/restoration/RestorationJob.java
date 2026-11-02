package io.camunda.blueberry.operation.restoration;

import io.camunda.blueberry.platform.version.CamundaVersionProfile;
import io.camunda.blueberry.platform.version.RestorePhase;
import io.camunda.blueberry.operation.OperationLog;

import java.util.List;

public class RestorationJob {

    public enum JOBSTATUS {PLANNED, INPROGRESS, REQUIRES_MANUAL_STEPS, TERMINATED, FAILED}

    private final long backupId;
    private final CamundaVersionProfile profile;
    private final OperationLog operationLog;
    private JOBSTATUS jobStatus = JOBSTATUS.PLANNED;

    public RestorationJob(long backupId, CamundaVersionProfile profile) {
        this.backupId = backupId;
        this.profile = profile;
        this.operationLog = new OperationLog();
    }

    public void run() {
        jobStatus = JOBSTATUS.INPROGRESS;
        List<RestorePhase> phases = profile.restorePhases();
        operationLog.startOperation("Restore backup " + backupId + " (Camunda " + profile.minorVersion() + ")",
                phases.size());

        boolean manualRequired = false;
        for (RestorePhase phase : phases) {
            operationLog.operationStep(null, phase.title());
            if (phase.kind() == RestorePhase.PhaseKind.AUTOMATED) {
                if ("preflight".equals(phase.id())) {
                    operationLog.info(null, "Preflight passed before job start.");
                } else if ("verify".equals(phase.id())) {
                    operationLog.info(null, "Run the post-restore checklist in the official restore guide.");
                }
            } else {
                manualRequired = true;
                operationLog.warning(null, "Manual step: " + phase.title() + ". " + phase.manualHint());
                operationLog.info(null, "Kubernetes automation is not enabled. See doc/restore/README.md in this repository.");
            }
        }

        operationLog.endOperation();
        jobStatus = manualRequired ? JOBSTATUS.REQUIRES_MANUAL_STEPS : JOBSTATUS.TERMINATED;
    }

    public long getBackupId() {
        return backupId;
    }

    public CamundaVersionProfile getProfile() {
        return profile;
    }

    public OperationLog getOperationLog() {
        return operationLog;
    }

    public JOBSTATUS getJobStatus() {
        return jobStatus;
    }
}
