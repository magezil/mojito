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

    displayInfo(label, data) {

        let gitBlameClass = "";
        if (data == null) {
            gitBlameClass = " git-blame-unused";
            data = "---";
        }

        return (
            <div className={"row git-blame"}>
                <label className={"col-sm-3 git-blame-label" + gitBlameClass}>{label}</label>
                <div className={"col-sm-9 git-blame-info" + gitBlameClass}>{data}</div>
            </div>
        );
    },

    renderTextUnitInfo() {
        if (this.props.textUnit === null)
            return "";

        console.log(this.props.textUnit);
        return (
            <div>
                {this.displayInfo("TmTextUnitId", this.props.textUnit.getTmTextUnitId())}
                {this.displayInfo("Repository", this.props.textUnit.getRepositoryName())}
                {this.displayInfo("AssetPath", this.props.textUnit.getAssetPath())}
                {this.displayInfo("Source", this.props.textUnit.getSource())}
                {this.displayInfo("Locale", this.props.textUnit.getTargetLocale())}
                {this.displayInfo("Comment", this.props.textUnit.getComment())}
            </div>);
    },

    renderGitBlameInfo() {
        return (
            <div>
                {this.displayInfo("Author", this.getAuthorName())}
                {this.displayInfo("Email", this.getAuthorEmail())}
                {this.displayInfo("Commit", this.getCommitName())}
                {this.displayInfo("Commit date", this.getCommitTime())}
                {this.displayInfo("Location", this.getOpenGrokLocation())}
            </div>
        )
    },

    getGitBlameProperty(property) {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["gitBlame"] == null)
            return null;
        return this.props.gitBlameWithUsage["gitBlame"][property];
    },

    getAuthorName() {
        return this.getGitBlameProperty("authorName");
    },

    getAuthorEmail() {
        return this.getGitBlameProperty("authorEmail");
    },

    getCommitName() {
        return this.getGitBlameProperty("commitName");
    },

    getCommitTime() {
        return this.getGitBlameProperty("commitTime");
    },

    getUsages() {
        if (this.props.gitBlameWithUsage == null || this.props.gitBlameWithUsage["usages"] == null)
            return null;
        return this.props.gitBlameWithUsage["usages"];
    },

    getOpenGrokLocation() {
        let textUnit = this.props.textUnit;
        let usages = this.getUsages();
        if (textUnit == null || usages == null || usages.length === 0)
            return null;
        let links = [];
        let repo = textUnit.getRepositoryName();
        for (let usage of usages) {
            let link = "opengrok.pinadmin.com/xref/";
            if (repo === "pinboard")
                link += "Pinboard/";
            link += usage;
            link = link.replace(":", "#");
            link = "https://" + link;
            links.push(<div><a href={link}>{usage}</a></div>);
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
                    <div className={"row"}>
                        <div className={"col-sm-4 git-blame-label"}><h4>Text unit information</h4></div>
                    </div>
                    <div>
                        {this.renderTextUnitInfo()}
                        <hr />
                    <div className={"row"}>
                        <div className={"col-sm-4 git-blame-label"}><h4>Git blame information</h4></div>
                        <div className={"col-sm-8 git-blame-info"}>{this.props.loading ? (<span className="glyphicon glyphicon-refresh spinning" />) : ""}</div>
                    </div>
                        {this.renderGitBlameInfo()}

                    </div>
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
