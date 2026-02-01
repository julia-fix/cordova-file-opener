#import <Cordova/CDVPlugin.h>

@interface SDFileOpener : CDVPlugin
- (void)open:(CDVInvokedUrlCommand*)command;
- (void)save:(CDVInvokedUrlCommand*)command;
@end
