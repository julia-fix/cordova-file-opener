#import "SDFileOpener.h"
#import <UIKit/UIKit.h>

@interface SDFileOpener () <UIDocumentInteractionControllerDelegate>
@property (nonatomic, strong) UIDocumentInteractionController *documentController;
@end

@implementation SDFileOpener

- (void)open:(CDVInvokedUrlCommand*)command {
    NSString *path = [command.arguments firstObject];
    if (path == nil || [path length] == 0) {
        [self sendError:@"NO_PATH" command:command];
        return;
    }

    NSURL *url = [NSURL URLWithString:path];
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

- (void)sendError:(NSString *)message command:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

@end
