import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonGroup, ButtonToolbar, FormControl, InputGroup, Modal} from "react-bootstrap";

let GitBlameInfoModal = React.createClass({

    propTypes() {
        return {
            "show": PropTypes.bool.isRequired,
            "textUnit": PropTypes.object.isRequired,
            "gitBlameWithUsage": PropTypes.object.isRequired
        };
    },

    getDefaultProps() {
        return {
            "show": false,
            "textUnit": null,
            "gitBlameWithUsage": null
        };
    },

    getTitle() {
        if (this.props.textUnit == null)
            return "";
        return this.props.textUnit.getName();
    },

    displayInfo(title, data) {
        if (data == null)
            return (
                <tr className={"git-blame-table"}>
                    <td className={"git-blame-label git-blame-unused"}><label>{title}</label></td>
                    <td className={"git-blame-unused"}>---</td>
                </tr>
            );
        return (
            <tr className={"git-blame-table"}>
                <td className={"git-blame-label"}><label>{title}</label></td>
                <td className={"git-blame-info"}>{data}</td>
            </tr>
        );
    },

    getTextUnitInfo() {
        if (this.props.textUnit === null)
            return "";
        let textUnitFields = {"TmTextUnitId": this.props.textUnit.getTmTextUnitId(),
            "Repository": this.props.textUnit.getRepositoryName(),
            "AssetPath": this.props.textUnit.getAssetPath(),
            "Source": this.props.textUnit.getSource(),
            "Locale": this.props.textUnit.getTargetLocale(),
            "Comment": this.props.textUnit.getComment()};

        let textUnitInfo = [];

        for (let key in textUnitFields) {
            textUnitInfo.push(this.displayInfo(key, textUnitFields[key]));
        }

        console.log(this.props.textUnit);
        return (textUnitInfo);
    },

    getGitBlameInfo(property, title) {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["gitBlame"] == null)
            return this.displayInfo(title, null);
        return this.displayInfo(title, this.props.gitBlameWithUsage["gitBlame"][property]);
    },

    getAuthorName() {
        return this.getGitBlameInfo("authorName", "Author");
    },

    getAuthorEmail() {
        return this.getGitBlameInfo("authorEmail", "Email");
    },

    getCommitName() {
        return this.getGitBlameInfo("commitName", "Commit");
    },

    getCommitTime() {
        return this.getGitBlameInfo("commitTime", "Commit date");
    },

    getUsages() {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["usages"] == null)
            return this.displayInfo("Location", null);
        return this.displayInfo("Location", this.props.gitBlameWithUsage["usages"].join(", "));
    },

    getOpenGrokLocation() {
        let textUnit = this.props.textUnit;
        if (textUnit == null || this.props.gitBlameWithUsage == null)
            return "";
        let links = [];
        let repo = textUnit.getRepositoryName();
        for (let usage of this.props.gitBlameWithUsage["usages"]) {
            let link = "opengrok.pinadmin.com/xref/";
            if (repo === "pinboard")
                link += "Pinboard/";
            link += usage;
            link = link.replace(":", "#");
            link = "https://" + link;
            links.push(<tr><td></td><td className={"link-location"}><a href={link}>{link}</a></td></tr>);
        }

        return links;
    },

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal() {
        this.props.onCloseModal();
    },

    render() {
        return (
            <Modal show={this.props.show} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title>{this.getTitle()}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <table>
                        <tbody>

                        {this.getTextUnitInfo()}
                        <tr>
                            <td colSpan={"2"}>
                                <label>Git blame information</label>
                                {this.props.loading ? (<span className="glyphicon glyphicon-refresh spinning" />) : ""}
                            </td>
                        </tr>
                            {this.getAuthorName()}
                            {this.getAuthorEmail()}
                            {this.getCommitName()}
                            {this.getCommitTime()}
                            {this.getUsages()}
                            {this.getOpenGrokLocation()}
                        </tbody>
                    </table>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.closeModal}>
                        <FormattedMessage id="textUnit.gitBlame.close"/>
                    </Button>
                </Modal.Footer>
            </Modal>
    );
    }
});

export default injectIntl(GitBlameInfoModal);
