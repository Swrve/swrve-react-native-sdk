#import <React/RCTBridgeModule.h>
#import <SwrveSDK/SwrveSDK.h>

@protocol SwrvePluginNativeProtocol <NSObject>

+ (void)initWithAppID:(int)appId apiKey:(nonnull NSString *)apiKey config:(nullable SwrveConfig *)config;
+ (void)handleDeeplink:(nonnull NSURL *)url;
+ (void)handleDeferredDeeplink:(nonnull NSURL *)url;
+ (BOOL)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
    withBackgroundCompletionHandler:(void (^)(UIBackgroundFetchResult, NSDictionary *))completionHandler API_AVAILABLE(ios(7.0));

@end

@interface SwrvePlugin : NSObject <RCTBridgeModule, SwrvePluginNativeProtocol>

NS_ASSUME_NONNULL_BEGIN

@property(nonatomic, strong, nonnull) Swrve *swrveInstance;

- (void)startWithUserId:(NSString *)userId;

- (void)stopTracking;

- (void)identifyWithUserIdentity:(nonnull NSString *)userIdentity
                            resolver:(RCTPromiseResolveBlock)resolve
                            rejecter:(RCTPromiseRejectBlock)reject;

- (void)userUpdateWithattributes:(nonnull NSDictionary *)attributes;

- (void)userUpdateWithName:(nonnull NSString *)name
                      date:(nonnull NSString *)dateString;

- (void)eventWithName:(nonnull NSString *)eventName
         eventPayload:(nullable NSDictionary *)eventPayload;

- (void)sendQueuedEvents;

- (void)currencyGivenWithCurrency:(nonnull NSString *)currency
                         quantity:(NSInteger)quantity;

- (void)purchaseItemWithName:(nonnull NSString *)itemName
                    currency:(nonnull NSString *)currency
                    quantity:(NSInteger)quantity
                        cost:(NSInteger)cost;

- (void)unvalidatedIapWithlocalCost:(double)localcost
                      localCurrency:(nonnull NSString *)localCurrency
                          productId:(nonnull NSString *)productId
                           quantity:(int)quantity;

- (void)unvalidatedIapWithlocalCost:(double)localCost
                      localCurrency:(nonnull NSString *)localCurrency
                          productId:(nonnull NSString *)productId
                           quantity:(int)quantity
                          rewardMap:(nonnull NSDictionary *)rewardMap;

- (void)getApiKeyWithResolver:(RCTPromiseResolveBlock)resolve
                     rejecter:(RCTPromiseRejectBlock)reject;

- (void)getUserIdWithResolver:(RCTPromiseResolveBlock)resolve
                     rejecter:(RCTPromiseRejectBlock)reject;

- (void)getExternalUserIdWithResolver:(RCTPromiseResolveBlock)resolve
                             rejecter:(RCTPromiseRejectBlock)reject;

- (void)isStartedWithResolver:(RCTPromiseResolveBlock)resolve
                     rejecter:(RCTPromiseRejectBlock)reject;

- (void)getUserResourcesWithResolver:(RCTPromiseResolveBlock)resolve
                            rejecter:(RCTPromiseRejectBlock)reject;

- (void)getUserResourcesDiffWithResolver:(RCTPromiseResolveBlock)resolve
                                rejecter:(RCTPromiseRejectBlock)reject;

- (void)getMessageCenterCampaignsWithPersonalization:(NSDictionary * _Nullable) personalization
                                     resolver:(RCTPromiseResolveBlock)resolve
                                     rejecter:(RCTPromiseRejectBlock)reject;

- (void)getPersonalizedTextWithText:(NSString *)text andPersonalization:(NSDictionary *)personalizationProperties
                           resolver:(RCTPromiseResolveBlock)resolve
                           rejecter:(RCTPromiseRejectBlock)reject;

- (void)getPersonalizedEmbeddedMessageDataWithId:(NSInteger)campaignId andPersonalization:(NSDictionary *)personalizationProperties
                                        resolver:(RCTPromiseResolveBlock)resolve
                                        rejecter:(RCTPromiseRejectBlock)reject;

- (void)showMessageCenterCampaignWithId:(NSInteger)campaignId withPersonalization:(NSDictionary * _Nullable) personalization;

- (void)removeMessageCenterCampaignWithId:(NSInteger)campaignId;

- (void)markMessageCenterCampaignAsSeenWithId:(NSInteger)campaignId;

- (void)markEmbeddedMessageCampaignAsSeenWithId:(NSInteger)campaignId;

- (void)markEmbeddedMessageButtonAsPressedWithId:(NSInteger)campaignId forButton:(NSString *)buttonName;

- (void)refreshCampaignsAndResources;

- (void)getRealTimeUserPropertiesWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

- (void)startedListening;

- (void)stoppedListening;

@end

NS_ASSUME_NONNULL_END
