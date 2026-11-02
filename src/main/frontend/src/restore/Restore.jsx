// -----------------------------------------------------------
//
// Restore
//
// List backups, preflight, start restore plan, monitor progress
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";
import {Button, InlineNotification, Tag} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";
import OverlayTrigger from "react-bootstrap/OverlayTrigger";
import Tooltip from "react-bootstrap/Tooltip";
import {Card, Modal} from "react-bootstrap";


class Restore extends React.Component {

    constructor(_props) {
        super();
        this.state = {
            listBackups: [],
            phases: [],
            display: {loading: false},
            status: "",
            selectedBackup: null,
            showConfirm: false,
            preflight: null,
            restoreMonitor: {statusRestore: "READY"},
            preflightLoading: false
        };
        this.refreshListBackup = this.refreshListBackup.bind(this);
        this.refreshListBackupCallback = this.refreshListBackupCallback.bind(this);
        this.monitorRestore = this.monitorRestore.bind(this);
    }

    componentDidMount() {
        this.refreshListBackup();
        this.loadPhases();
        this.monitorRestore();
        this.monitorInterval = setInterval(this.monitorRestore, 5000);
    }

    componentWillUnmount() {
        clearInterval(this.monitorInterval);
    }

    loadPhases() {
        RestCallService.getInstance().getJson('blueberry/api/restore/phases?', this, (httpPayload) => {
            if (!httpPayload.isError()) {
                this.setState({phases: httpPayload.getData().phases || []});
            }
        });
    }

    render() {
        return (
            <div className={"container"}>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Restore</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Select a complete backup to start a restore plan. Kubernetes steps are manual until cluster automation is enabled.
                        </InlineNotification>
                        {this.state.status &&
                            <InlineNotification kind="error" hideCloseButton="true" lowContrast="false">
                                {this.state.status}
                            </InlineNotification>}
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => this.refreshListBackup()}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Refresh List Backup
                        </Button>
                    </div>
                </div>

                {this.renderMonitorCard()}

                <table id="runnersTable" className="table is-hoverable is-fullwidth">
                    <thead>
                    <tr>
                        <th>Backup ID</th>
                        <th>Date</th>
                        <th>Status</th>
                        <th>Components</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    {this.state.listBackups ? this.state.listBackups.map((row) =>
                        <tr key={row.backupId}>
                            <td>{row.backupId}</td>
                            <td>{row.backupTime}</td>
                            <td>{this.renderStatusTag(row)}</td>
                            <td>
                                {row.presentComponents && row.presentComponents.map((c, i) =>
                                    <Tag key={i} type="blue">{c}</Tag>)}
                                {row.missingComponents && row.missingComponents.length > 0 &&
                                    row.missingComponents.map((c, i) =>
                                        <Tag key={"m" + i} type="red">missing {c}</Tag>)}
                            </td>
                            <td>{this.renderRestoreButton(row)}</td>
                        </tr>
                    ) : null}
                    </tbody>
                </table>

                {this.state.phases.length > 0 &&
                    <Card style={{marginTop: "16px"}}>
                        <Card.Header>Restore phases (Camunda version profile)</Card.Header>
                        <Card.Body>
                            <ol>
                                {this.state.phases.map((p) =>
                                    <li key={p.id}>
                                        {p.title} {p.kind === "MANUAL" && <Tag type="gray">Manual</Tag>}
                                    </li>)}
                            </ol>
                        </Card.Body>
                    </Card>}

                <Modal show={this.state.showConfirm} onHide={() => this.setState({showConfirm: false})}>
                    <Modal.Header closeButton>
                        <Modal.Title>Confirm restore for backup {this.state.selectedBackup?.backupId}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p>This action plans a destructive restore. The platform may be unavailable during restore.</p>
                        {this.state.preflightLoading && <p>Checking preflight...</p>}
                        {this.state.preflight && !this.state.preflight.allowed &&
                            <InlineNotification kind="error" hideCloseButton="true">
                                {this.state.preflight.message}
                            </InlineNotification>}
                        {this.state.preflight && this.state.preflight.allowed &&
                            <p>{this.state.preflight.message}</p>}
                        {this.state.preflight?.docsRestoreUrl &&
                            <p><a href={this.state.preflight.docsRestoreUrl} target="_blank" rel="noreferrer">Official restore guide</a> (Camunda {this.state.preflight.camundaMinorVersion})</p>}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button kind="secondary" onClick={() => this.setState({showConfirm: false})}>Cancel</Button>
                        <Button kind="danger"
                                disabled={!this.state.preflight?.allowed || this.state.preflightLoading}
                                onClick={() => this.startRestore()}>
                            Start restore
                        </Button>
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }

    renderMonitorCard() {
        const m = this.state.restoreMonitor;
        if (!m || m.statusRestore === "READY") {
            return null;
        }
        return (
            <Card style={{marginTop: "12px", marginBottom: "12px"}}>
                <Card.Header>Restore progress</Card.Header>
                <Card.Body>
                    <p>Status: <Tag type="blue">{m.statusRestore}</Tag> Backup ID: {m.backupId}</p>
                    <p>Step {m.step}/{m.totalNumberOfSteps}: {m.stepName}</p>
                    {m.docsRestoreUrl &&
                        <p><a href={m.docsRestoreUrl} target="_blank" rel="noreferrer">Restore documentation</a></p>}
                    {m.messages && m.messages.slice(-5).map((msg, i) =>
                        <div key={i} style={{fontSize: "0.85rem"}}>{msg.message}</div>)}
                </Card.Body>
            </Card>
        );
    }

    renderStatusTag(row) {
        const status = row.backupStatus;
        if (status === "PARTIALBACKUP" || row.complete === false) {
            return <Tag type="red">Partial</Tag>;
        }
        if (status === "COMPLETED" || row.complete === true) {
            return <Tag type="green">Complete</Tag>;
        }
        if (status === "INPROGRESS") {
            return <Tag type="blue">In progress</Tag>;
        }
        if (status === "FAILED") {
            return <Tag type="red">Failed</Tag>;
        }
        return <Tag type="gray">{status || "Unknown"}</Tag>;
    }

    renderRestoreButton(row) {
        const canRestore = row.complete === true && row.backupStatus !== "INPROGRESS";
        if (canRestore) {
            return (
                <Button size="sm" onClick={() => this.openConfirm(row)}>
                    Restore
                </Button>
            );
        }
        const reason = row.complete === false
            ? "This backup is incomplete. Restore is disabled until all active components have a backup for this ID."
            : "Restore is not available for this backup state.";
        return (
            <OverlayTrigger placement="top" overlay={<Tooltip id={"tt-" + row.backupId}>{reason}</Tooltip>}>
                <span className="d-inline-block">
                    <Button size="sm" disabled style={{pointerEvents: "none"}}>Restore</Button>
                </span>
            </OverlayTrigger>
        );
    }

    openConfirm(row) {
        this.setState({
            selectedBackup: row,
            showConfirm: true,
            preflight: null,
            preflightLoading: true
        });
        RestCallService.getInstance().getJson(
            'blueberry/api/restore/preflight?backupId=' + row.backupId + '&',
            this,
            (httpPayload) => {
                if (httpPayload.isError()) {
                    this.setState({preflightLoading: false, preflight: {allowed: false, message: httpPayload.getError()}});
                } else {
                    this.setState({preflightLoading: false, preflight: httpPayload.getData()});
                }
            });
    }

    startRestore() {
        const backupId = this.state.selectedBackup?.backupId;
        RestCallService.getInstance().postJson(
            'blueberry/api/restore/start?',
            {backupId: backupId},
            this,
            (httpPayload) => {
                if (httpPayload.isError()) {
                    this.setState({status: httpPayload.getError(), showConfirm: false});
                } else {
                    this.setState({showConfirm: false, status: ""});
                    this.monitorRestore();
                }
            });
    }

    monitorRestore() {
        RestCallService.getInstance().getJson('blueberry/api/restore/monitor?', this, (httpPayload) => {
            if (!httpPayload.isError()) {
                this.setState({restoreMonitor: httpPayload.getData()});
            }
        });
    }

    refreshListBackup() {
        const tz = new Date().getTimezoneOffset();
        let uri = 'blueberry/api/backup/list?timezoneoffset=' + tz;
        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        RestCallService.getInstance().getJson(uri, this, this.refreshListBackupCallback);
    }

    refreshListBackupCallback(httpPayload) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            this.setState({status: "Error loading backups"});
        } else {
            this.setState({listBackups: httpPayload.getData().listBackups || []});
        }
    }

    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }
}

export default Restore;
