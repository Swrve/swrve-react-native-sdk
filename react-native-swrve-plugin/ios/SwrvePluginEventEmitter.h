#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

NS_ASSUME_NONNULL_BEGIN

extern NSString *const PUSH_EVENT_NAME;
extern NSString *const SILENT_PUSH_EVENT_NAME;
extern NSString *const PUSH_EVENT_PAYLOAD;

extern NSString *const CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP;
extern NSString *const CALLBACK_KEY_EMBEDDED_MESSAGE_MAP;
extern NSString *const CALLBACK_KEY_EMBEDDED_CONTROL_ACTION;

extern NSString *const CALLBACK_KEY_MESSAGE_DETAIL_MAP;
extern NSString *const CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON;
extern NSString *const CALLBACK_KEY_MESSAGE_DETAIL_ACTION;

extern NSString *const CALLBACK_KEY_DEEPLINK_ACTION_STRING;

extern NSString *const RESOURCES_UPDATED_EVENT_NAME;
extern NSString *const EMBEDDED_CALLBACK_EVENT_NAME;
extern NSString *const MESSAGE_DETAILS_DELEGATE_EVENT_NAME;
extern NSString *const DEEPLINK_DELEGATE_EVENT_NAME;


@interface SwrvePluginEventEmitter : RCTEventEmitter <RCTBridgeModule>

@end

NS_ASSUME_NONNULL_END
