# cordova-file-opener

Open a local file in the appropriate native app. Works with files stored in `window.cordova.file.cacheDirectory`.

## Install (local)

```
cordova plugin add /Volumes/Files/projects/kinolift/cordova-file-opener
```

## Usage

```js
window.FileOpener.open(fileEntry.nativeURL, function () {
  console.log('OPENED');
}, function (err) {
  console.log('ERROR', err); // NO_PATH, INVALID_PATH, NOT_FOUND, NO_APP, FAILED
});
```

## Notes
- Android uses a `FileProvider` and grants read permission at runtime.
- iOS uses `UIDocumentInteractionController` and shows an “Open In…” menu.

## Error codes
- `NO_PATH` – empty or missing path.
- `INVALID_PATH` – malformed path/URL.
- `NOT_FOUND` – file does not exist.
- `NO_APP` – no app can open the file.
- `FAILED` – unexpected native error while trying to open.

