#import "SDFileOpener.h"
#import <UIKit/UIKit.h>

@interface SDFileOpener () <UIDocumentInteractionControllerDelegate, UIDocumentPickerDelegate>
@property (nonatomic, strong) UIDocumentInteractionController *documentController;
@property (nonatomic, copy) NSString *pendingCallbackId;
@end

@implementation SDFileOpener

- (void)open:(CDVInvokedUrlCommand*)command {
    NSString *path = [command.arguments firstObject];
    if (path == nil || [path length] == 0) {
        [self sendError:@"NO_PATH" command:command];
        return;
    }

    NSURL *url = [self fileURLForPath:path];
    if (url == nil || url.scheme == nil) {
        url = [NSURL fileURLWithPath:path];
    }

    if (url == nil) {
        [self sendError:@"INVALID_PATH" command:command];
        return;
    }

    if ([url isFileURL]) {
        if (![[NSFileManager defaultManager] fileExistsAtPath:[url path]]) {
            [self sendError:@"NOT_FOUND" command:command];
            return;
        }
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        self.documentController = [UIDocumentInteractionController interactionControllerWithURL:url];
        self.documentController.delegate = self;

        UIView *view = self.viewController.view;
        CGRect rect = view.bounds;
        BOOL presented = [self.documentController presentOpenInMenuFromRect:rect inView:view animated:YES];
        if (presented) {
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OPENED"];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        } else {
            [self sendError:@"NO_APP" command:command];
        }
    });
}

- (void)save:(CDVInvokedUrlCommand*)command {
    if (self.pendingCallbackId != nil) {
        [self sendError:@"BUSY" command:command];
        return;
    }

    NSString *path = [command.arguments firstObject];
    if (path == nil || [path length] == 0) {
        [self sendError:@"NO_PATH" command:command];
        return;
    }

    NSURL *url = [self fileURLForPath:path];
    if (url == nil || ![url isFileURL]) {
        [self sendError:@"INVALID_PATH" command:command];
        return;
    }

    if (![[NSFileManager defaultManager] fileExistsAtPath:[url path]]) {
        [self sendError:@"NOT_FOUND" command:command];
        return;
    }

    self.pendingCallbackId = command.callbackId;
    dispatch_async(dispatch_get_main_queue(), ^{
        UIDocumentPickerViewController *picker = nil;
        if (@available(iOS 14.0, *)) {
            picker = [[UIDocumentPickerViewController alloc] initForExportingURLs:@[url]];
        } else {
            picker = [[UIDocumentPickerViewController alloc] initWithURL:url inMode:UIDocumentPickerModeExportToService];
        }
        picker.delegate = self;
        picker.modalPresentationStyle = UIModalPresentationFormSheet;
        [self.viewController presentViewController:picker animated:YES completion:nil];
    });
}

- (NSURL *)fileURLForPath:(NSString *)path {
    NSURL *url = [NSURL URLWithString:path];
    if (url == nil) {
        return nil;
    }
    if (url.scheme == nil) {
        return [NSURL fileURLWithPath:path];
    }
    if ([url.scheme isEqualToString:@"file"]) {
        return url;
    }
    return url;
}

- (void)sendError:(NSString *)message command:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

#pragma mark - UIDocumentPickerDelegate

- (void)documentPicker:(UIDocumentPickerViewController *)controller didPickDocumentsAtURLs:(NSArray<NSURL *> *)urls {
    if (self.pendingCallbackId == nil) {
        return;
    }
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"SAVED"];
    [self.commandDelegate sendPluginResult:result callbackId:self.pendingCallbackId];
    self.pendingCallbackId = nil;
}

- (void)documentPickerWasCancelled:(UIDocumentPickerViewController *)controller {
    if (self.pendingCallbackId == nil) {
        return;
    }
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"CANCELED"];
    [self.commandDelegate sendPluginResult:result callbackId:self.pendingCallbackId];
    self.pendingCallbackId = nil;
}

@end
