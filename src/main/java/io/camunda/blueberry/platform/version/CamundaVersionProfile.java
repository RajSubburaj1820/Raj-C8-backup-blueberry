package io.camunda.blueberry.platform.version;

import java.util.List;
import java.util.Map;

/**
 * Procedure and documentation profile for a Camunda minor release (8.7–8.10).
 */
public record CamundaVersionProfile(
        String minorVersion,
        String docsBackupUrl,
        String docsRestoreUrl,
        boolean zeebeExportPauseSoftQuery,
        String minHelmChartVersionForZeebeRestore
) {
    public Map<String, Object> toMap() {
        return Map.of(
                "minorVersion", minorVersion,
                "docsBackupUrl", docsBackupUrl,
                "docsRestoreUrl", docsRestoreUrl,
                "zeebeExportPauseSoftQuery", zeebeExportPauseSoftQuery,
                "minHelmChartVersionForZeebeRestore", minHelmChartVersionForZeebeRestore
        );
    }

    /**
     * Ordered restore phases for this Camunda minor (ES path). Kubernetes broker steps stay manual until K8sConnect is live.
     */
    public List<RestorePhase> restorePhases() {
        String doc = docsRestoreUrl;
        return List.of(
                RestorePhase.automated("preflight", "Validate backup completeness"),
                RestorePhase.manual("es_templates", "Restore Elasticsearch templates",
                        "Follow " + doc + " — start components briefly on a clean datastore if required."),
                RestorePhase.manual("es_snapshots", "Restore Elasticsearch snapshots (Operate, Tasklist, Optimize, Zeebe records)",
                        "Use the same backup ID for every snapshot. See " + doc),
                RestorePhase.manual("zeebe_scale_down", "Scale down Zeebe brokers",
                        "Manual Kubernetes step. See doc/restore/README.md in this repository."),
                RestorePhase.manual("zeebe_broker_restore", "Restore Zeebe broker data (ZEEBE_RESTORE)",
                        "Requires Helm chart >= " + minHelmChartVersionForZeebeRestore + ". See " + doc),
                RestorePhase.manual("zeebe_scale_up", "Scale up Zeebe brokers",
                        "Manual Kubernetes step after restore pods complete."),
                RestorePhase.automated("verify", "Post-restore verification checklist")
        );
    }

    public static CamundaVersionProfile forMinor(String minor) {
        String base = "https://docs.camunda.io/docs/" + minor + "/self-managed/operational-guides/backup-restore/";
        return switch (minor) {
            case "8.8" -> new CamundaVersionProfile("8.8", base + "backup/", base + "restore/", true, "12.1.0");
            case "8.9" -> new CamundaVersionProfile("8.9", base + "backup/", base + "restore/", true, "12.1.0");
            case "8.10" -> new CamundaVersionProfile("8.10", base + "backup/", base + "restore/", true, "12.1.0");
            default -> new CamundaVersionProfile("8.7",
                    "https://docs.camunda.io/docs/8.7/self-managed/operational-guides/backup-restore/backup/",
                    "https://docs.camunda.io/docs/8.7/self-managed/operational-guides/backup-restore/restore/",
                    true,
                    "12.1.0");
        };
    }
}
