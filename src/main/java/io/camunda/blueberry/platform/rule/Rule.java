package io.camunda.blueberry.platform.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule
 */
public interface Rule {

    boolean validRule();

    String getName();

    /* Explain what the rule is doing, what it check */
    String getExplanations();

    List<String> getUrlDocumentation();

    /* Check the rule */
    RuleInfo check();

    /**
     * Configure : run the configuration to create the environnement to run backups
     *
     * @return
     */
    RuleInfo configure();


    /**
     * INFO is only used on a verification: it gives an information, it is neither a success nor an error
     */
    enum RuleStatus {DEACTIVATED, CORRECT, FAILED, INPROGRESS, FAILEDBUTWILLBEFIXED, INFO}


    /**
     * A rule is executed, and this is the information returned for this execution
     */
    class RuleInfo {
        private final Rule rule;
        private final StringBuilder details = new StringBuilder();
        private final StringBuilder errors = new StringBuilder();
        private final List<Tuple> listVerifications = new ArrayList();
        private boolean valid;
        private RuleStatus status = RuleStatus.INPROGRESS;

        public RuleInfo(Rule rule) {
            this.rule = rule;
        }

        public String getName() {
            return rule.getName();
        }

        public void addDetails(String details) {
            this.details.append(details + ";");
        }

        public void addError(String error) {
            this.errors.append(error + ";");
        }

        public String getDetails() {
            if (status == RuleStatus.DEACTIVATED) {
                return "The component is deactivated, check is not necessary.";
            }
            return details.toString();
        }

        public String getErrors() {
            return errors.toString();
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public RuleStatus getStatus() {
            return status;
        }

        public void setStatus(RuleStatus status) {
            this.status = status;
        }

        public boolean inProgress() {
            return status == RuleStatus.INPROGRESS;
        }

        public List<Tuple> getListVerifications() {
            return listVerifications;
        }

        public void addVerifications(String action, RuleStatus actionStatus, String command) {
            this.listVerifications.add(new Tuple(action, actionStatus, command));
        }

        public void addVerificationsAssertBoolean(String action, boolean status, String command) {
            this.listVerifications.add(new Tuple(action, status ? RuleStatus.CORRECT : RuleStatus.FAILED, command));
        }

        public void addVerificationsButWillBeFixed(String action, RuleStatus actionStatus, String command) {
            if (actionStatus.equals(RuleStatus.FAILED)) {
                actionStatus = RuleStatus.FAILEDBUTWILLBEFIXED;
            }
            this.listVerifications.add(new Tuple(action, actionStatus, command));
        }

        /**
         * Same as addVerifications, with an error message shown under the action
         */
        public void addVerificationsWithError(String action, RuleStatus actionStatus, String command, String error) {
            this.listVerifications.add(new Tuple(action, actionStatus, command, error));
        }

        /**
         * Add an information line (status INFO): it does not change the status of the rule
         */
        public void addInfo(String action, String command, String message) {
            this.listVerifications.add(new Tuple(action, RuleStatus.INFO, command, message));
        }

        /**
         * Give the result of the access to /actuator/env: is it possible to read the repository name from the component,
         * and where the value used by the rule comes from
         *
         * @param envUrl         url of /actuator/env of the component
         * @param result         result of the access to the actuator
         * @param repositoryName name of the repository used by the rule
         */
        public void addActuatorEnvInfo(String componentName, String envUrl, AccessParameterValue.ResultParameter result,
                                       Object repositoryName) {
            String action = "Access the actuator " + envUrl;
            if (result.accessActuator) {
                addInfo(action, envUrl,
                        "Can access the actuator. The repository name [" + repositoryName + "] is read from " + componentName + ".");
            } else if (result.masked) {
                addInfo(action, envUrl,
                        "Can access the actuator, but " + componentName + " hides the values of env (******). "
                                + "The repository name [" + repositoryName + "] comes from the blueberry configuration. "
                                + "To read it from the component, set management.endpoint.env.show-values to ALWAYS.");
            } else if (result.envReachable) {
                addInfo(action, envUrl,
                        "Can access the actuator, but the variable of the repository name is not defined in " + componentName
                                + ". The repository name [" + repositoryName + "] comes from the blueberry configuration.");
            } else {
                addInfo(action, envUrl,
                        "Can't access the actuator (it is optional). The repository name [" + repositoryName
                                + "] comes from the blueberry configuration. Expose 'env' on " + componentName
                                + " to read the value from the component.");
            }
        }

        public Rule getRule() {
            return this.rule;
        }

        public record Tuple(String action, RuleStatus actionStatus, String command, String error) {
            public Tuple(String action, RuleStatus actionStatus, String command) {
                this(action, actionStatus, command, null);
            }
        }
    }
}