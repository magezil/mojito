import alt from "../../alt";
import Error from "../../utils/Error";
import TextUnit from "../../sdk/TextUnit";
import TextUnitDataSource from "../../actions/workbench/TextUnitDataSource";
import WorkbenchActions from "../../actions/workbench/WorkbenchActions";
import SearchParamsStore from "./SearchParamsStore";
import {StatusCommonTypes} from "../../components/screenshots/StatusCommon";
import GitBlameActions from "../../actions/workbench/GitBlameActions";

class GitBlameStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(GitBlameActions);
        this.registerAsync(TextUnitDataSource);
    }

    setDefaultState() {
        this.show = false;
        this.textUnit = null;
        this.gitBlameWithUsage = null;
    }

    close() {
        this.show = false;
    }

    openWithTextUnit(textUnit) {
        console.log("GitBlameStore::openWithTextUnit");

        this.show = true;
        this.textUnit = textUnit;
        this.getInstance().getGitBlameInfo(textUnit);

    }

    onGetGitBlameInfoSuccess(gitBlameWithUsage) {
        console.log("GitBlameStore::onGetGitBlameInfoSuccess");
        this.gitBlameWithUsage = gitBlameWithUsage;
        console.log(this.gitBlameWithUsage);
    }

    onGetGitBlameInfoError(errorResponse) {
        console.log("GitBlameStore::onGetGitBlameInfoError");
    }
}

export default alt.createStore(GitBlameStore, 'GitBlameStore');
