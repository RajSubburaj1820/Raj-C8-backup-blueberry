// -----------------------------------------------------------
//
// Restore
//
// List backups and prepare restore (automation in a later release)
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";
import {Button, InlineNotification, Tag} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";
import OverlayTrigger from "react-bootstrap/OverlayTrigger";
import Tooltip from "react-bootstrap/Tooltip";


class Restore extends React.Component {

    constructor(_props) {
        super();
        this.state = {
            listBackups: [],
            display: {loading: false},
            status: ""
        };
        this.refreshListBackup = this.refreshListBackup.bind(this);
        this.refreshListBackupCallback = this.refreshListBackupCallback.bind(this);
    }

    componentDidMount() {
        this.refreshListBackup();
    }

    render() {
        return (
            <div className={"container"}>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Restore</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Select a complete backup to start a restore. Partial backups cannot be restored.
                        </InlineNotification>
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
            </div>
        )
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
                <Button size="sm" disabled title="Restore automation is not available in this release">
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
