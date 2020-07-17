#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

NS_ASSUME_NONNULL_BEGIN

extern NSString *const PUSH_EVENT_NAME;
extern NSString *const SILENT_PUSH_EVENT_NAME;
extern NSString *const PUSH_EVENT_PAYLOAD;

extern NSString *const CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL;
extern NSString *const CALLBACK_KEY_CUSTOM_BUTTON_ACTION;
extern NSString *const CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT;
extern NSString *const CALLBACK_KEY_DISMISS_BUTTON_NAME;
extern NSString *const CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT;

extern NSString *const RESOURCES_UPDATED_EVENT_NAME;
extern NSString *const MESSAGE_CALLBACK_INSTALL_EVENT_NAME;
extern NSString *const MESSAGE_CALLBACK_CUSTOM_EVENT_NAME;
extern NSString *const MESSAGE_CALLBACK_DISMISS_EVENT_NAME;
extern NSString *const MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME;

@interface SwrvePluginEventEmitter : RCTEventEmitter <RCTBridgeModule>

@end

NS_ASSUME_NONNULL_END
