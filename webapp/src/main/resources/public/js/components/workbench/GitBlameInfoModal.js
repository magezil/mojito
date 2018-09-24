import PropTypes from 'prop-types';
import React from "react";
import {FormattedMessage, injectIntl} from "react-intl";
import {Button, ButtonGroup, ButtonToolbar, FormControl, Modal} from "react-bootstrap";
import TextUnit from "../../sdk/TextUnit";

let TextUnitsGitBlameModal = React.createClass({

    propTypes() {
        return {
            "isShowGitBlameModal": PropTypes.bool.isRequired,
            "repoIds": PropTypes.array,
            "repositories": PropTypes.array
        };
    },

    getDefaultProps() {
        return {
            "isShowGitBlameModal": false,
            "repoIds": [],
            "repositories": []
        };
    },

    /**
     * @returns {{
     *      currentReviewState: {string} The current review state of the selected textunits in case of bulk operation or the textunit passed in as prop otherwise.
     *      comment: {string} The target comment to be prepopulated in textarea. In case of bulk operation, this is left blank.
     *  }}
     */
    getInitialState() {

        return {
            "repoIds": this.props.repoIds,
            "repositories": this.props.repositories
        };
    },

    /**
     * Closes the modal and calls the parent action handler to mark the modal as closed
     */
    closeModal() {
        this.setState({
            isShowModal: false
        });
        this.props.onCloseModal();
    },

    render() {
        return (
            <Modal show={this.props.isShowModal} onHide={this.closeModal}>
                <Modal.Header closeButton>
                    <Modal.Title><FormattedMessage id="textUnit.gitBlame.title"/></Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>Repository name</div>
                    <div>Author name</div>
                    <div>Author email</div>
                    <div>Commit name</div>
                    <div>Commit date</div>
                    <div>Location/usages (OpenGrok link?) </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.closeModal}>
                        <FormattedMessage id="textUnit.gitBlame.close"/>
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
});

export default injectIntl(TextUnitsGitBlameModal);
