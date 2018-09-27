import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";

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

    getRepositoryName() {
        if (this.props.textUnit === null)
            return "";
        return "Repository: " + this.props.textUnit.getRepositoryName()
    },

    getGitBlameInfo(property, string) {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["gitBlame"] == null)
            return "";
        return string + ": " + this.props.gitBlameWithUsage["gitBlame"][property];
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
            return "";
        if (this.props.gitBlameWithUsage["usages"].length === 0)
            return "";
        return "Location: " + this.props.gitBlameWithUsage["usages"].join(", ");
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
                    <Modal.Title><FormattedMessage id="textUnit.gitBlame.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>{this.getRepositoryName()} </div>
                    <div>{this.getAuthorName()}</div>
                    <div>{this.getAuthorEmail()}</div>
                    <div>{this.getCommitName()}</div>
                    <div>{this.getCommitTime()}</div>
                    <div>{this.getUsages()} </div>
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
