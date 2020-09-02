#import "SwrvePlugin.h"
#import "SwrvePluginErrorCodes.h"
#import "SwrvePluginUtils.h"
#import "SwrvePluginEventEmitter.h"
#import "SwrveCallbackStateManager.h"
#import "SwrvePluginPushHandler.h"
#import <SwrveSDK/SwrveSDK.h>
#import <SwrveSDK/SwrveCampaign.h>

#define SWRVE_PLUGIN_VERSION "1.1.0"

@interface SwrvePlugin ()

@property (nonatomic, strong, nonnull) Swrve* swrveInstanceReference;

@end

@implementation SwrvePlugin

@dynamic swrveInstance;

RCT_EXPORT_MODULE()


#pragma mark Developer Native Interface

+ (void)initWithAppID:(int)appId apiKey:(NSString*)apiKey config:(SwrveConfig*)config {
    if (config == nil) {
        config = [SwrveConfig new];
    }
    
    if (config.pushEnabled) {
        config.pushResponseDelegate = SwrvePluginPushHandler.sharedInstance;
    }

    // Set the resource listener callback
    config.resourcesUpdatedCallback = ^() {
        [[NSNotificationCenter defaultCenter] postNotificationName:RESOURCES_UPDATED_EVENT_NAME object:self userInfo:nil];
    };
    
    [SwrveSDK sharedInstanceWithAppID:appId apiKey:apiKey config:config];

    // Set callbacks
    if([[SwrveSDK messaging] dismissButtonCallback] == nil) {
        [[SwrveSDK messaging] setDismissButtonCallback:^(NSString *campaignSubject, NSString *buttonName) {
           [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_DISMISS_EVENT_NAME object:self userInfo:@{CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT: campaignSubject, CALLBACK_KEY_DISMISS_BUTTON_NAME: buttonName}];
        }];
    }
    
    if([[SwrveSDK messaging] installButtonCallback] == nil) {
        [[SwrveSDK messaging] setInstallButtonCallback:^BOOL(NSString *appStoreUrl) {
            [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_INSTALL_EVENT_NAME object:self userInfo:@{CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL: appStoreUrl}];
            [SwrvePlugin handleInstall:appStoreUrl];
            return true;
        }];
    }
    
    if([[SwrveSDK messaging] customButtonCallback] == nil) {
        [[SwrveSDK messaging] setCustomButtonCallback:^(NSString *action) {
            [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_CUSTOM_EVENT_NAME object:self userInfo:@{CALLBACK_KEY_CUSTOM_BUTTON_ACTION: action}];
            [SwrvePlugin handleCustom:action];
        }];
    }
    
    if([[SwrveSDK messaging] clipboardButtonCallback] == nil) {
        [[SwrveSDK messaging] setClipboardButtonCallback:^(NSString *processedText) {
            [[NSNotificationCenter defaultCenter] postNotificationName:MESSAGE_CALLBACK_INSTALL_EVENT_NAME object:self userInfo:@{CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT: processedText}];
        }];
    }
    
    //Ensure we start the state manager for this moment
    [SwrveCallbackStateManager sharedInstance];
    
    [SwrvePlugin sendPluginVersion];
}

+ (void)handleDeeplink:(NSURL *)url {
    [SwrveSDK handleDeeplink:url];
}

+ (void)handleDeferredDeeplink:(NSURL *)url {
    [SwrveSDK handleDeferredDeeplink:url];
}

+ (void)installAction:(NSURL *)url {
    [SwrveSDK installAction:url];
}

+ (BOOL)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo withBackgroundCompletionHandler:(void (^)(UIBackgroundFetchResult, NSDictionary *))completionHandler API_AVAILABLE(ios(7.0))
{
    NSLog(@"SwrvePlugin - didReceiveRemoteNotification %@", userInfo);
    BOOL handled = [SwrveSDK didReceiveRemoteNotification:userInfo withBackgroundCompletionHandler:completionHandler];
    
    if (handled) {
       if ([userInfo objectForKey:SwrveSilentPushPayloadKey]) {
           NSString *json = [SwrvePluginUtils serializeDictonaryToJson:userInfo withKey:SwrveSilentPushPayloadKey];
           if (json) {
               [[NSNotificationCenter defaultCenter] postNotificationName:SILENT_PUSH_EVENT_NAME object:self userInfo:@{PUSH_EVENT_PAYLOAD: json}];
           }
       } else {
           NSString *json = [SwrvePluginUtils serializeDictonaryToJson:userInfo withKey:nil];
           if (json) {
               [[NSNotificationCenter defaultCenter] postNotificationName:PUSH_EVENT_NAME object:self userInfo:@{PUSH_EVENT_PAYLOAD: json}];
           }
       }
   }
    return handled;
}

+ (void) sendPluginVersion {
    if ([SwrveSDK started]) {
        [SwrveSDK userUpdate:[[NSDictionary alloc] initWithObjectsAndKeys:@SWRVE_PLUGIN_VERSION, @"swrve.react_native_plugin_version", nil]];
        [SwrveSDK sendQueuedEvents];
    }
}

#pragma mark SDK lifecycle

- (void) setSwrveInstance:(Swrve*)swrveInstance {
    self.swrveInstanceReference = swrveInstance;
}

- (Swrve*) swrveInstance {
    if (self.swrveInstanceReference) {
        return self.swrveInstanceReference;
    } else {
        self.swrveInstanceReference = [SwrveSDK sharedInstance];
        return self.swrveInstanceReference;
    }
}

#pragma mark - React Native Module

RCT_REMAP_METHOD(start,
                 startWithUserId:(NSString *)userId)
{
    NSLog(@"SwrvePlugin - start %@", userId);

    if (userId == nil || userId.length == 0) {
        [self.swrveInstance start];
        // Need to use dispatch_after or SDK will not be ready to send.
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            [SwrvePlugin sendPluginVersion];
        });
    } else {
        [self.swrveInstance startWithUserId:userId];
        // Need to use dispatch_after or SDK will not be ready to send.
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            [SwrvePlugin sendPluginVersion];
        });
    }
}

RCT_REMAP_METHOD(identify,
                 identifyWithUserIdentity:(nonnull NSString *)userIdentity
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    if (userIdentity == nil || userIdentity.length == 0) {
        reject(INVALID_ARGUMENT, @"No user identity supplied", generalSwrveError(0));
        return;
    }

    NSLog(@"SwrvePlugin - identify %@", userIdentity);

    [self.swrveInstance identify:userIdentity
             onSuccess:^(NSString * _Nonnull status, NSString * _Nonnull swrveUserId)
        {
            NSLog(@"SwrvePlugin - identify - success %@", swrveUserId);
            resolve(swrveUserId);
        }
               onError:^(NSInteger httpCode, NSString * _Nonnull errorMessage)
        {
            NSLog(@"SwrvePlugin - identify - error %@", errorMessage);
            reject(swrveResponseCode((int)httpCode), errorMessage, generalSwrveError(httpCode));
        }
     ];
}

RCT_REMAP_METHOD(event,
                 eventWithName:(nonnull NSString *)eventName
                 eventPayload:(nullable NSDictionary*)eventPayload)
{
    NSLog(@"SwrvePlugin - event %@ %@", eventName, eventPayload);

    if (eventPayload != nil && eventPayload.count > 0) {
        NSDictionary* stringsDict = [SwrvePluginUtils toDictionaryJustStrings:eventPayload];
        [self.swrveInstance event:eventName payload:stringsDict];
    } else {
        [self.swrveInstance event:eventName];
    }
}

RCT_EXPORT_METHOD(sendQueuedEvents)
{
    NSLog(@"SwrvePlugin - sendQueuedEvents");

    [self.swrveInstance sendQueuedEvents];
}

RCT_REMAP_METHOD(userUpdate,
                 userUpdateWithattributes:(nonnull NSDictionary*)attributes)
{
    NSLog(@"SwrvePlugin - userUpdate %@", attributes);

    NSDictionary* stringsDict = [SwrvePluginUtils toDictionaryJustStrings:attributes];
    [self.swrveInstance userUpdate: stringsDict];
}

RCT_REMAP_METHOD(userUpdateDate,
                 userUpdateWithName:(nonnull NSString*)name
                 date:(nonnull NSString*)dateString)
{
    NSLog(@"SwrvePlugin - userUpdateDate %@ %@", name, dateString);

    // Parse date coming in (for example "2016-12-02T15:39:47.699Z")
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    dateFormatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";
    NSDate *date = [dateFormatter dateFromString:dateString];

    if (date != nil) {
        [self.swrveInstance userUpdate:name withDate:date];
    } else {
        NSLog(@"SwrvePlugin - userUpdateDate unable to parse date string");
    }
}

RCT_REMAP_METHOD(currencyGiven,
                 currencyGivenWithCurrency:(nonnull NSString*)currency
                 quantity:(NSInteger)quantity)
{
    NSLog(@"SwrvePlugin - currencyGiven %@ %ld", currency, quantity);

    [self.swrveInstance currencyGiven:currency givenAmount:quantity];
}

RCT_REMAP_METHOD(purchase,
                 purchaseItemWithName:(nonnull NSString*)itemName
                 currency:(nonnull NSString*)currency
                 quantity:(NSInteger)quantity
                 cost:(NSInteger)cost)
{
    [self.swrveInstance purchaseItem:itemName currency:currency cost:(int)cost quantity:(int)quantity];
}

RCT_REMAP_METHOD(unvalidatedIap,
                 unvalidatedIapWithlocalCost:(double)localcost
                 localCurrency:(nonnull NSString*)localCurrency
                 productId:(nonnull NSString*)productId
                 quantity:(int)quantity)
{
    [self.swrveInstance unvalidatedIap:nil localCost:localcost localCurrency:localCurrency productId:productId productIdQuantity:quantity];
}

RCT_REMAP_METHOD(unvalidatedIapWithReward,
                 unvalidatedIapWithlocalCost:(double)localCost
                 localCurrency:(nonnull NSString*)localCurrency
                 productId:(nonnull NSString*)productId
                 quantity:(int)quantity
                 rewardMap:(nonnull NSDictionary*)rewardMap)
{
    SwrveIAPRewards* reward = [[SwrveIAPRewards alloc] init];

    NSArray *items = [rewardMap objectForKey:@"items"];

    if (items != nil && items.count > 0) {
        for (NSDictionary *item in items) {
            [reward addItem:[item objectForKey:@"name"] withQuantity:[[item objectForKey:@"amount"] longValue]];
        }
    }

    NSArray *currencies = [rewardMap objectForKey:@"currencies"];

    if (currencies != nil && [currencies count] != 0) {
        for (NSDictionary *currency in currencies) {
            [reward addCurrency:[currency objectForKey:@"name"] withAmount:[[currency objectForKey:@"amount"] longValue]];
        }
    }

    [self.swrveInstance unvalidatedIap:reward localCost:localCost localCurrency:localCurrency productId:productId productIdQuantity:quantity];
}

RCT_REMAP_METHOD(getApiKey, getApiKeyWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        NSString *apiKey = [self.swrveInstance apiKey];
        resolve(apiKey);
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];        
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(getUserId, getUserIdWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        NSString *userId = [self.swrveInstance userID];
        resolve(userId);
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];        
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(getExternalUserId, getExternalUserIdWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        NSString *externalUserId = [self.swrveInstance externalUserId];
        resolve(externalUserId);
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(isStarted, isStartedWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        BOOL isStarted = [self.swrveInstance started];
        resolve(@(isStarted));
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(getUserResources, getUserResourcesWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        [self.swrveInstance userResources:^(NSDictionary *resources, NSString *resourcesAsJSON) {
            resolve(resources);
        }];
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(getUserResourcesDiff, getUserResourcesDiffWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        [self.swrveInstance userResourcesDiff:^(NSDictionary *oldResourcesValues, NSDictionary *newResourcesValues, NSString *resourcesAsJSON) {
            NSMutableDictionary* userResourcesDiff = [NSMutableDictionary new];
            [userResourcesDiff setObject:oldResourcesValues forKey:@"oldResourcesValues"];
            [userResourcesDiff setObject:newResourcesValues forKey:@"newResourcesValues"];
            resolve(userResourcesDiff);
        }];
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(getMessageCenterCampaigns, getMessageCenterCampaignsWithPersonalization:(NSDictionary * _Nullable)personalization resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
    
        NSArray<SwrveCampaign *> *campaigns;
        if (personalization != nil) {
            campaigns = [[self.swrveInstance messaging] messageCenterCampaignsWithPersonalisation:personalization];           
        } else {
            campaigns = [[self.swrveInstance messaging] messageCenterCampaigns];
        }

        NSMutableArray *messageAsArray = [[NSMutableArray alloc] init];
        
        for (SwrveCampaign *campaign in campaigns) {
            NSMutableDictionary *campaignDictionary = [[NSMutableDictionary alloc] init];
            [campaignDictionary setValue:[NSNumber numberWithUnsignedInteger:[campaign ID]] forKey:@"ID"];
            [campaignDictionary setValue:[NSNumber numberWithUnsignedInteger:[campaign maxImpressions]] forKey:@"maxImpressions"];
            [campaignDictionary setValue:[campaign subject] forKey:@"subject"];
            [campaignDictionary setValue:[NSNumber numberWithUnsignedInteger:[[campaign dateStart] timeIntervalSince1970]] forKey:@"dateStart"];
            [campaignDictionary setValue:@([campaign messageCenter]) forKey:@"messageCenter"];
            
            NSMutableDictionary *stateDictionary = [NSMutableDictionary dictionaryWithDictionary:[[campaign state] asDictionary]];
            
            // Remove unused ID
            [stateDictionary removeObjectForKey:@"ID"];
            
            // convert the status to a readable format so its consistent across both platforms
            NSUInteger statusNumber = [[stateDictionary objectForKey:@"status"] integerValue];
            [stateDictionary setObject:[self translateCampaignStatus:statusNumber] forKey:@"status"];
            [campaignDictionary setObject:stateDictionary forKey:@"state"];
            [messageAsArray addObject:campaignDictionary];
        }
        
        resolve(messageAsArray);
        
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];
        reject(ex.name, ex.reason, error);
    }
}

RCT_REMAP_METHOD(showMessageCenterCampaign, showMessageCenterCampaignWithId:(NSInteger)campaignId withPersonalization:(NSDictionary * _Nullable) personalization) {
    
    SwrveCampaign *canditiate = [self findMessageCenterCampaignbyID:campaignId];
    if (canditiate) {
        [[self.swrveInstance messaging] showMessageCenterCampaign:canditiate withPersonalisation:personalization];
    } else {
        NSLog(@"SwrvePlugin - Unable to find campaign of id: %ld", campaignId);
    }
}

RCT_REMAP_METHOD(removeMessageCenterCampaign, removeMessageCenterCampaignWithId:(NSInteger)campaignId) {
    
    SwrveCampaign *canditiate = [self findMessageCenterCampaignbyID:campaignId];
    if (canditiate) {
        [[self.swrveInstance messaging] removeMessageCenterCampaign:canditiate];
    } else {
        NSLog(@"SwrvePlugin - Unable to find campaign of id: %ld", campaignId);
    }
}

RCT_REMAP_METHOD(markMessageCenterCampaignAsSeen, markMessageCenterCampaignAsSeenWithId:(NSInteger)campaignId) {
    
    SwrveCampaign *canditiate = [self findMessageCenterCampaignbyID:campaignId];
    if (canditiate) {
        [[self.swrveInstance messaging] markMessageCenterCampaignAsSeen:canditiate];
    } else {
        NSLog(@"SwrvePlugin - Unable to find campaign of id: %ld", campaignId);
    }
}

RCT_REMAP_METHOD(getRealTimeUserProperties, getRealTimeUserPropertiesWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    
    @try {
        [self.swrveInstance realTimeUserProperties:^(NSDictionary *properties) {
            resolve(properties);
        }];
    } @catch ( NSException *ex ) {
        NSError *error = [SwrvePluginUtils produceErrorFromException:ex];
        reject(ex.name, ex.reason, error);
    }
}

RCT_EXPORT_METHOD(refreshCampaignsAndResources) {
    [self.swrveInstance refreshCampaignsAndResources];
}

#pragma mark - listening functions

RCT_EXPORT_METHOD(startedListening) {
    SwrvePluginPushHandler.sharedInstance.shouldBufferEvents = NO;
    [SwrvePluginPushHandler.sharedInstance flushBufferedEvents];
}

RCT_EXPORT_METHOD(stoppedListening) {
    SwrvePluginPushHandler.sharedInstance.shouldBufferEvents = YES;
}

RCT_EXPORT_METHOD(listeningInstall) {
    SwrveCallbackStateManager.sharedInstance.isListeningInstall = YES;
}

RCT_EXPORT_METHOD(listeningCustom) {
    SwrveCallbackStateManager.sharedInstance.isListeningCustom = YES;
}

#pragma mark - helper methods


+ (void) handleInstall:(NSString *) nonProcessedAction {
    if (SwrveCallbackStateManager.sharedInstance.isListeningInstall == NO) {
        [SwrvePlugin handleAction:nonProcessedAction];
    }
}

+ (void) handleCustom:(NSString *) nonProcessedAction {
    if (SwrveCallbackStateManager.sharedInstance.isListeningCustom == NO) {
        [SwrvePlugin handleAction:nonProcessedAction];
    }
}

+ (void) handleAction:(NSString *) nonProcessedAction {
    if(nonProcessedAction != nil) {
        NSURL *url = [NSURL URLWithString:nonProcessedAction];
        if (url != nil) {
            if (@available(iOS 10.0, *)) {
                DebugLog(@"Action - %@ - handled.  Sending to application as URL", nonProcessedAction);
                [[UIApplication sharedApplication] openURL:url options:@{} completionHandler:^(BOOL success) {
                    DebugLog(@"Opening url [%@] successfully: %d", url, success);
                }];
            } else {
                DebugLog(@"Action not handled, not supported (should not reach this code)", nil);
            }
        } else {
            DebugLog(@"Action - %@ -  not handled. Action to be processed in react layer.", nonProcessedAction);
        }
    }
}

- (SwrveCampaign *) findMessageCenterCampaignbyID:(NSInteger) campaignId {
    NSArray<SwrveCampaign *> *campaigns = [[self.swrveInstance messaging] messageCenterCampaigns];
    SwrveCampaign *canditiate;
    
    for (SwrveCampaign *campaign in campaigns) {
        if ([campaign ID] == campaignId) {
            canditiate = campaign;
        }
    }

    return canditiate;
}

- (NSString *)translateCampaignStatus:(NSUInteger) status {
    switch (status){
        case SWRVE_CAMPAIGN_STATUS_UNSEEN:
            return @"Unseen";
            break;
        case SWRVE_CAMPAIGN_STATUS_SEEN:
            return @"Seen";
            break;
        case SWRVE_CAMPAIGN_STATUS_DELETED:
            return @"Deleted";
            break;
        default:
            return @"Unseen";
    }
}

@end
