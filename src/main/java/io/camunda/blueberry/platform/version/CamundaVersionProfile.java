package io.camunda.blueberry.platform.version;

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
