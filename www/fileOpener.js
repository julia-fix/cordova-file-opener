var exec = require('cordova/exec');

var FileOpener = {};

function normalizeCallbacks(options, successCallback, errorCallback) {
    if (typeof options === 'function') {
        return { options: null, successCallback: options, errorCallback: successCallback };
    }
    return { options: options || null, successCallback: successCallback, errorCallback: errorCallback };
}

function resolvePath(path, done) {
    if (typeof path !== 'string') {
        done(path);
        return;
    }
    if (path.indexOf('cdvfile://') === 0 && typeof window.resolveLocalFileSystemURL === 'function') {
        window.resolveLocalFileSystemURL(
            path,
            function (entry) {
                done(entry && (entry.nativeURL || (typeof entry.toURL === 'function' && entry.toURL())) || path);
            },
            function () {
                done(path);
            }
        );
        return;
    }
    done(path);
}

FileOpener.open = function (path, options, successCallback, errorCallback) {
    var normalized = normalizeCallbacks(options, successCallback, errorCallback);
    if (!path) {
        if (typeof normalized.errorCallback === 'function') {
            normalized.errorCallback('NO_PATH');
        }
        return;
    }
    resolvePath(path, function (resolvedPath) {
        exec(normalized.successCallback, normalized.errorCallback, 'FileOpener', 'open', [resolvedPath]);
    });
};

FileOpener.save = function (path, options, successCallback, errorCallback) {
    var normalized = normalizeCallbacks(options, successCallback, errorCallback);
    if (!path) {
        if (typeof normalized.errorCallback === 'function') {
            normalized.errorCallback('NO_PATH');
        }
        return;
    }
    resolvePath(path, function (resolvedPath) {
        exec(normalized.successCallback, normalized.errorCallback, 'FileOpener', 'save', [resolvedPath]);
    });
};

module.exports = FileOpener;
