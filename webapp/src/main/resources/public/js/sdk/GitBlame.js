import _ from "lodash";

/**
 * Used to communicate with textunits WS (search for translated/untranslated strings, create/update new translation)
 *
 * A TextUnit is bound to a TmTextUnit and a target locale. It represents a current translation or an untranslated string.
 *
 * A TmTextUnit is the entity that represent only a string that needs to be translated, it's not bound to a target locale.
 * Multiple TmTextUnitVariant (translation) are linked to a TmTextUnit.
 *
 */
export default
class GitBlame {

    constructor(data) {
        this.data = data || {};
    }

    /**
     * TextUnit id.
     *
     * When the TextUnit is returned by the TextUnitClient:
     * - If id is not null, it means a current translation exists (and this id is mapping to the TMTextUnitCurrentVariant id).
     * - If id is undefined, it means there is no translation yet (more precisely, there is no TMTextUnitCurrentVariant for
     * the TMTextUnit and locale).
     *
     * @returns {int}
     */
    getId() {
        return this.data.id;
    }

    setId(id) {
        this.data.id = id;
    }

    /**
     * TmTextUnit id.
     *
     * Must be provided when creating/updating a TextUnit
     *
     * @returns {int}
     */
    getTmTextUnitId() {
        return this.data.tmTextUnitId;
    }

    setTmTextUnitId(tmTextUnitId) {
        this.data.tmTextUnitId = tmTextUnitId;
    }

    getAuthorEmail() {
        return this.data.authorEmail;
    }

    setAuthorEmail(authorEmail) {
        this.data.authorEmail = authorEmail;
    }

    getAuthorName() {
        return this.data.authorName;
    }

    setAuthorName(authorName) {
        this.data.authorName = authorName;
    }

    getCommitTime() {
        return this.data.commitTime;
    }

    setCommitTime(commitTime) {
        this.data.commitTime = commitTime;
    }

    getCommitName() {
        return this.data.commitName;
    }

    setCommitName(commitName) {
        this.data.commitName = commitName;
    }

    /**
     * According to the current implementation of the web service, the combination
     * of tmTextUnitId and localeId is a unique key for the TextUnit object. The
     * implementation of this function must be updated if the TextUnit definition is
     * changed in the web service.
     * @returns {string} A string to uniquely identify a TextUnit object
     */
    // getTextUnitKey() {
    //     return this.getTmTextUnitId() + "_" + this.getLocaleId();
    // }

    // equals(textUnit) {
    //     return this.getTextUnitKey() === textUnit.getTextUnitKey();
    // }

    static toGitBlame(jsonTextUnits) {

        var gitBlame = [];

        for (let gitBlame of jsonTextUnits) {
            gitBlame.push(GitBlame.toGitBlame(gitBlame));
        }

        return gitBlame;
    }

    static toGitBlame(jsonTextUnit) {

        return new GitBlame(jsonTextUnit);
    }
}

