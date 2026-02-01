var exec = require('cordova/exec');

var FileOpener = {};

FileOpener.open = function (path, successCallback, errorCallback) {
    if (!path) {
        if (typeof errorCallback === 'function') {
            errorCallback('NO_PATH');
        }
        return;
    }
    exec(successCallback, errorCallback, 'FileOpener', 'open', [path]);
};

module.exports = FileOpener;
