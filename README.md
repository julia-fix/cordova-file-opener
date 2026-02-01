# cordova-file-opener

Open a local file in the appropriate native app. Works with files stored in `window.cordova.file.cacheDirectory`.

## Install

```
cordova plugin add https://github.com/julia-fix/cordova-file-opener.git
```

## Usage

```js
window.FileOpener.open(
  fileEntry.nativeURL,
  function () {
    console.log("OPENED");
  },
  function (err) {
    console.log("ERROR", err); // NO_PATH, INVALID_PATH, NOT_FOUND, NO_APP, FAILED
  },
);

window.FileOpener.save(
  fileEntry.nativeURL,
  function () {
    console.log("SAVED");
  },
  function (err) {
    console.log("ERROR", err); // NO_PATH, INVALID_PATH, NOT_FOUND, CANCELED
  },
);
```

## Notes

- Android uses a `FileProvider` and grants read permission at runtime.
- iOS uses `UIDocumentInteractionController` and shows an “Open In…” menu.
- If you pass a `cdvfile://` URL, the JS wrapper resolves it to a `file://` URL when possible so the system UI shows a normal filename/icon.

## Error codes

- `NO_PATH` – empty or missing path.
- `INVALID_PATH` – malformed path/URL.
- `NOT_FOUND` – file does not exist.
- `NO_APP` – no app can open the file.
- `FAILED` – unexpected native error while trying to open.
- `CANCELED` – user canceled the save picker.
- `NOT_SUPPORTED` – operation not supported on this platform.
