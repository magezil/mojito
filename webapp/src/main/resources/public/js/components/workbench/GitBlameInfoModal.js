import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";
import TextUnit from "../../sdk/TextUnit";
import GitBlameStore from "alt/src/store/AltStore";

let TextUnitsGitBlameModal = React.createClass({

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
        return this.props.textUnit.getRepositoryName()
    },

    getGitBlameInfo(property) {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["gitBlame"] == null)
            return "";
        return this.props.gitBlameWithUsage["gitBlame"][property]
    },

    getAuthorName() {
        return this.getGitBlameInfo("authorName");
    },

    getAuthorEmail() {
        return this.getGitBlameInfo("authorEmail");
    },

    getCommitName() {
        return this.getGitBlameInfo("commitName");
    },

    getCommitTime() {
        return this.getGitBlameInfo("commitTime");
    },

    getUsages() {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["usages"] == null)
            return "";
        return this.props.gitBlameWithUsage["usages"].join(", ");
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

export default injectIntl(TextUnitsGitBlameModal);
