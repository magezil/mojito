import Error from "../../utils/Error";
import TextUnitError from "../../utils/TextUnitError";
import TextUnitClient from "../../sdk/TextUnitClient";
import WorkbenchActions from "./WorkbenchActions";

const TextUnitDataSource = {
    performSaveTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            console.log("TextUnitDataSource.performSaveTextUnit ");

            return TextUnitClient.saveTextUnit(textUnit)
                .catch(error => {
                    throw new TextUnitError(Error.IDS.TEXTUNIT_SAVE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.saveTextUnitSuccess,
        error: WorkbenchActions.saveTextUnitError
    },
    performCheckAndSaveTextUnit: {

        remote(searchResultsStoreState, textUnit) {
            console.log("TextUnitDataSource.performCheckAndSaveTextUnit ");

            return TextUnitClient.checkTextUnitIntegrity(textUnit)
                .then(checkResult => {
                    if (checkResult && !checkResult.checkResult) {
                        throw new TextUnitError(Error.IDS.TEXTUNIT_CHECK_FAILED, textUnit);
                    }
                }).then(() => {
                    return TextUnitClient.saveTextUnit(textUnit)
                        .catch(error => {
                            throw new TextUnitError(Error.IDS.TEXTUNIT_SAVE_FAILED, textUnit);
                        });
                });
        },
        success: WorkbenchActions.checkAndSaveTextUnitSuccess,
        error: WorkbenchActions.checkAndSaveTextUnitError
    },
    deleteTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            console.log("TextUnitDataSource.deleteTextUnit ");

            return TextUnitClient.deleteCurrentTranslation(textUnit)
                .catch(error => {
                    throw new TextUnitError(Error.IDS.TEXTUNIT_DELETE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.deleteTextUnitsSuccess,
        error: WorkbenchActions.deleteTextUnitsError
    },

    saveVirtualAssetTextUnit: {
        remote(searchResultsStoreState, textUnit) {
            console.log("TextUnitDataSource.saveVirtualAssetTextUnit ");

            return TextUnitClient.saveVirtualAssetTextUnit(textUnit)
                .catch(error => {
                    throw new TextUnitError(Error.IDS.VIRTUAL_ASSET_TEXTUNIT_SAVE_FAILED, textUnit);
                });
        },
        success: WorkbenchActions.saveVirtualAssetTextUnitSuccess,
        error: WorkbenchActions.saveVirtualAssetTextUnitError
    },

    getInfo: {
        remote(textUnit) {
            console.log("TextUnitDataSource.getInfo ");

            return TextUnitClient.getInfo(textUnit);

        },
        success: WorkbenchActions.getInfoSuccess,
        error: WorkbenchActions.getInfoError
    }
};

export default TextUnitDataSource;
