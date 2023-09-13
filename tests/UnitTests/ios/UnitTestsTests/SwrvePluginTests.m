#import <XCTest/XCTest.h>
#import <OCMock/OCMock.h>

#import <SwrveSDK/Swrve.h>
#import <SwrveSDK/SwrveSDK.h>
#import <SwrveSDK/SwrveCampaign.h>
#import <SwrveSDK/SwrveCampaignStatus.h>
#import <SwrveSDK/SwrveMessageCenterDetails.h>
#import <SwrveSDK/SwrveMessageController+Private.h>
#import <SwrveSDKCommon/SwrveUtils.h>

#import "../../node_modules/react-native-swrve-plugin/ios/SwrvePlugin.h"
#import "../../node_modules/react-native-swrve-plugin/ios/SwrvePluginPushHandler.h"
#import "../../node_modules/react-native-swrve-plugin/ios/SwrvePluginEventEmitter.h"

@interface SwrvePluginTests : XCTestCase

@property (nonatomic, strong) Swrve *swrve;
@property (nonatomic, strong) SwrveMessageController *messaging;
@property (nonatomic, strong) SwrvePlugin *plugin;


@end

@interface SwrvePluginPushHandler (Tests)

- (void)resetStateForTesting;

@end

@interface SwrvePluginPushHandler ()

// Expose private method
- (void) handleNotificationUserInfo:(NSDictionary*)userInfo;

@end

@interface SwrvePlugin ()

// Expose private method
- (NSMutableDictionary *) getCache;

@end

@implementation SwrvePluginTests

- (void) setUp {
  self.swrve = OCMClassMock([Swrve class]);
  
  self.plugin = OCMPartialMock([SwrvePlugin new]);
  self.plugin.swrveInstance = self.swrve;
}

- (void) tearDown {
  // Put teardown code here. This method is called after the invocation of each test method in the class.
}

- (void) testStartWithUserId {
  [self.plugin startWithUserId:@"user-id"];
  
  OCMVerify([self.swrve startWithUserId:@"user-id"]);
}

- (void) testStopTracking {
  [self.plugin stopTracking];
  
  OCMVerify([self.swrve stopTracking]);
}

- (void) testStartWithoutUserId {
  [self.plugin startWithUserId:nil];
  
  OCMVerify([self.swrve start]);
}

- (void) testIdentifyInvalidId {
  [self.plugin identifyWithUserIdentity:@"" resolver:^(id result) {
    // Shouldn't get called
    XCTAssert(false);
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    // Should get called
    XCTAssertTrue(code.length > 0);
    XCTAssertTrue(message.length > 0);
    XCTAssertNotNil(error);
  }];
}

- (void) testIdentifyValidId {
  OCMStub([self.swrve start]);
  OCMStub([self.swrve identify:[OCMArg any]
                     onSuccess:[OCMArg invokeBlock]
                       onError:[OCMArg any]]);
  
  [self.plugin identifyWithUserIdentity:@"identity" resolver:^(id result) {
    // Couldn't get the 'invokeBlock' above to pass a value, which seems to be due to the success callback being typed 'id' (void*)
    // So just check it's the correct callback
    XCTAssert(true);
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    XCTAssert(false);
  }];
}

- (void) testIdentifyValidIdSwrveRejection {
  OCMStub([self.swrve start]);
  OCMStub([self.swrve identify:[OCMArg any]
                     onSuccess:[OCMArg any]
                       onError:[OCMArg invokeBlock]]);
  
  [self.plugin identifyWithUserIdentity:@"identity" resolver:^(id result) {
    
    XCTAssert(false);
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    XCTAssert(true);
  }];
}

/// Validate the dictionary from testUserUpdate
- (int)validateAttributes:(NSDictionary *)attributes {
  XCTAssert([[attributes objectForKey:@"string"] isEqualToString:@"string-value"]);
  XCTAssert([[attributes objectForKey:@"double"] isEqualToString:@"12.45"]);
  XCTAssert([[attributes objectForKey:@"int"] isEqualToString:@"1234"]);
  XCTAssert([[attributes objectForKey:@"boolean"] isEqualToString:@"1"]);
  return 0;
}

- (void) testUserUpdate {
  OCMStub([self.swrve userUpdate:[OCMArg any]]).andCall(self, @selector(validateAttributes:));
  
  // The Swrve API only supports NSDictionary<String,String> so we convert the non-string values to strings in the plugin
  // This is intended to help dynamic language developers use the API
  [self.plugin userUpdateWithattributes:@{
    @"string": @"string-value",
    @"boolean": @true,
    @"int": @1234,
    @"double": @12.45
  }];
  
  OCMVerify([self.swrve userUpdate:[OCMArg any]]);
}

- (void) testuserUpdateWithDate {
  NSString *dateString = @"2020-04-17T06:45:56.645Z";
  NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
  [formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"];
  NSLocale *posix = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
  [formatter setLocale:posix];
  NSDate *date = [formatter dateFromString:dateString];
  
  // Check that the date coming through to Swrve is the same as that above
  OCMStub([self.swrve userUpdate:[OCMArg isKindOfClass:NSString.class] withDate:[OCMArg checkWithBlock:^BOOL(id obj) {
    return [(NSDate*)obj isEqualToDate:date];
  }]]);
  
  [self.plugin userUpdateWithName:@"name" date:dateString];
}

- (void) testEventWithPayload {
  OCMStub([self.swrve event:[OCMArg any] payload:[OCMArg checkWithBlock:^BOOL(id obj) {
    [self validateAttributes:obj];
    return true;
  }]]);
  
  // The Swrve API only supports NSDictionary<String,String> so we convert the non-string values to strings in the plugin
  // This is intended to help dynamic language developers use the API
  [self.plugin eventWithName:@"event" eventPayload:@{
    @"string": @"string-value",
    @"boolean": @true,
    @"int": @1234,
    @"double": @12.45
  }];
  
  OCMVerify([self.swrve event:@"event" payload:[OCMArg any]]);
}

- (void) testSendQueuedEvents {
  [self.plugin sendQueuedEvents];
  
  OCMVerify([self.swrve sendQueuedEvents]);
}

- (void) testCurrencyGiven {
  [self.plugin currencyGivenWithCurrency:@"GBP" quantity:12];
  
  OCMVerify([self.swrve currencyGiven:@"GBP" givenAmount:12]);
}

- (void) testPurchase {
  [self.plugin purchaseItemWithName:@"item-name" currency:@"GBP" quantity:12 cost:22];
  
  OCMVerify([self.swrve purchaseItem:@"item-name" currency:@"GBP" cost:22 quantity:12]);
}

- (void) testUnvalidatedIap {
  [self.plugin unvalidatedIapWithlocalCost:23.4 localCurrency:@"Bob" productId:@"product-id" quantity:12];
  
  OCMVerify([self.swrve unvalidatedIap:nil localCost:23.4 localCurrency:@"Bob" productId:@"product-id" productIdQuantity:12]);
}

- (bool) checkSwrveRewards:(SwrveIAPRewards*)swrveRewards {
  NSDictionary* rewards = [(SwrveIAPRewards*)swrveRewards rewards];
  XCTAssert([[[rewards objectForKey:@"Pieces of eight"] objectForKey:@"amount"] isEqual:@33]);
  XCTAssert([[[rewards objectForKey:@"hoodie"] objectForKey:@"type"] isEqual:@"item"]);
  XCTAssert([[[rewards objectForKey:@"sword"] objectForKey:@"amount"] isEqual:@34]);
  return true;
}

- (void) testUnvalidatedIapWithReward {
  NSDictionary* sentRwards = @{
    @"items": @[
        @{
          @"name": @"hoodie",
          @"amount": @13
        },
        @{
          @"name": @"sword",
          @"amount": @34
        }
    ],
    @"currencies": @[
        @{
          @"name": @"Pieces of eight",
          @"amount": @33
        }
    ]
  };
  
  [self.plugin unvalidatedIapWithlocalCost:23 localCurrency:@"Gold" productId:@"product-id" quantity:12 rewardMap:sentRwards];
  
  OCMVerify([self.swrve unvalidatedIap:[OCMArg checkWithSelector:@selector(checkSwrveRewards:) onObject:self] localCost:23 localCurrency:@"Gold" productId:@"product-id" productIdQuantity:12]);
}

- (void) testGetApiKey {
   
  [self.plugin getApiKeyWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve apiKey]);
}

- (void) testGetUserId {
   
  [self.plugin getUserIdWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve userID]);
}

- (void) testGetExternalUserId {
   
  [self.plugin getExternalUserIdWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve externalUserId]);
}

- (void) testIsStarted {
   
  [self.plugin isStartedWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve started]);
}

- (void) testGetUserResources {
   
  [self.plugin getUserResourcesWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve userResources:[OCMArg any]]);
}

- (void) testGetUserResourcesDiff {
   
  [self.plugin getUserResourcesDiffWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve userResourcesDiffWithListener:[OCMArg any]]);
}

- (void) testGetRealTimeUserProperties {
   
  [self.plugin getRealTimeUserPropertiesWithResolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve realTimeUserProperties:[OCMArg any]]);
}

- (void) testRefreshResourcesAndCampaigns {
  
  [self.plugin refreshCampaignsAndResources];
  
  OCMVerify([self.swrve refreshCampaignsAndResources]);
}

- (void) testGetMessageCenterCampaigns {
  SwrveCampaign *campaignMock = OCMClassMock([SwrveCampaign class]);
  SwrveCampaignState *campaignStateMock = OCMClassMock([SwrveCampaignState class]);
  SwrveMessageCenterDetails *messageCenterDetailsMock = OCMClassMock([SwrveMessageCenterDetails class]);

  // Mock Campaign State
  OCMStub([campaignStateMock campaignID]).andReturn(44);
  OCMStub([campaignStateMock status]).andReturn(SWRVE_CAMPAIGN_STATUS_UNSEEN);
  OCMStub([campaignStateMock impressions]).andReturn(0);
  
  // Mock Campaign Message Center Details
  OCMStub([messageCenterDetailsMock subject]).andReturn(@"IAM message center subject");
//  OCMStub([messageCenterDetailsMock description]).andReturn(@"IAM message center description");
  OCMStub([messageCenterDetailsMock imageAccessibilityText]).andReturn(@"IAM message center image accesibility text");
  OCMStub([messageCenterDetailsMock imageUrl]).andReturn(@"https://faker.image.com/");
  OCMStub([messageCenterDetailsMock imageSha]).andReturn(@"0590479d0050002e99f411f72d0d635351134a12");

  // Mock Campaign
  OCMStub([campaignMock ID]).andReturn(44);
  OCMStub([campaignMock subject]).andReturn(@"IAM subject");
  OCMStub([campaignMock name]).andReturn(@"IAM name");
  OCMStub([campaignMock messageCenter]).andReturn(true);
  OCMStub([campaignMock maxImpressions]).andReturn(11111);
  OCMStub([campaignMock priority]).andReturn([NSNumber numberWithInt:(9999)]);
  OCMStub([campaignMock downloadDate]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
  OCMStub([campaignMock dateStart]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
  OCMStub([campaignMock dateEnd]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
  OCMStub([campaignMock messageCenterDetails]).andReturn(messageCenterDetailsMock);
  OCMStub([campaignStateMock status]).andReturn(campaignStateMock);

  // Mock Campaigns List
  NSArray *mockList = [NSArray arrayWithObject:campaignMock];
  OCMExpect([self.swrve messageCenterCampaigns]).andReturn(mockList);

  XCTestExpectation *promiseResolved = [self expectationWithDescription:@"promiseResolved"];

  [self.plugin getMessageCenterCampaignsWithPersonalization:nil resolver:^(id result) {

    XCTAssertNotNil(result, @"result should not be null");
    NSArray *messageCentre = (NSArray *)result;

    NSDictionary *firstCampaign = [messageCentre firstObject];
    XCTAssertNotNil(firstCampaign, @"Campaign from Message Center should not be null");
    XCTAssertEqualObjects([firstCampaign objectForKey:@"name"], @"IAM name");
    XCTAssertEqualObjects([firstCampaign objectForKey:@"subject"], @"IAM subject");
    XCTAssertEqual([[firstCampaign objectForKey:@"ID"] integerValue], 44);
    XCTAssertTrue([firstCampaign objectForKey:@"messageCenter"], @"messageCenter should be true");
    XCTAssertEqual([[firstCampaign objectForKey:@"priority"] integerValue], 9999);
    XCTAssertEqual([[firstCampaign objectForKey:@"maxImpressions"] integerValue], 11111);
    XCTAssertEqual([[firstCampaign objectForKey:@"downloadDate"] integerValue], 1362671700);
    XCTAssertEqual([[firstCampaign objectForKey:@"dateStart"] integerValue], 1362671700);
    XCTAssertEqual([[firstCampaign objectForKey:@"dateEnd"] integerValue], 1362671700);

    NSDictionary *firstCampaignState = [firstCampaign objectForKey:@"state"];
    XCTAssertEqual([[firstCampaignState objectForKey:@"next"] integerValue], 0);
    XCTAssertEqualObjects([firstCampaignState objectForKey:@"status"], @"Unseen");
    XCTAssertEqual([[firstCampaignState objectForKey:@"impressions"] integerValue], 0);
    
    NSDictionary *firstCampaignMessageCenterDetails = [firstCampaign objectForKey:@"messageCenterDetails"];
    XCTAssertEqualObjects([firstCampaignMessageCenterDetails objectForKey:@"subject"], @"IAM message center subject");
//    XCTAssertEqualObjects([firstCampaignMessageCenterDetails objectForKey:@"description"], @"IAM message center description");
    XCTAssertEqualObjects([firstCampaignMessageCenterDetails objectForKey:@"imageAccessibilityText"], @"IAM message center image accesibility text");
    XCTAssertEqualObjects([firstCampaignMessageCenterDetails objectForKey:@"imageURL"], @"https://faker.image.com/");
    XCTAssertEqualObjects([firstCampaignMessageCenterDetails objectForKey:@"imageSha"], @"0590479d0050002e99f411f72d0d635351134a12");

    [promiseResolved fulfill];

  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    XCTFail(@"Rejected: this should not be rejected");
  }];

  // waiting for resolver to complete
  [self waitForExpectationsWithTimeout:15 handler:^(NSError *error) {
      if (error) {
          XCTFail(@"Ran out of time: GetMessageCenter");
      }
  }];
  
  OCMVerify([self.swrve messageCenterCampaigns]);
}

- (void) testGetMessageCenterCampaign {
  SwrveCampaign *campaignMock = OCMClassMock([SwrveCampaign class]);
  SwrveCampaignState *campaignStateMock = OCMClassMock([SwrveCampaignState class]);
  SwrveMessageCenterDetails *messageCenterDetailsMock = OCMClassMock([SwrveMessageCenterDetails class]);

  // Mock Campaign State
  OCMStub([campaignStateMock campaignID]).andReturn(44);
  OCMStub([campaignStateMock status]).andReturn(SWRVE_CAMPAIGN_STATUS_UNSEEN);
  OCMStub([campaignStateMock impressions]).andReturn(0);
  
  // Mock Campaign Message Center Details
  OCMStub([messageCenterDetailsMock subject]).andReturn(@"IAM message center subject");
//  OCMStub([messageCenterDetailsMock description]).andReturn(@"IAM message center description");
  OCMStub([messageCenterDetailsMock imageAccessibilityText]).andReturn(@"IAM message center image accesibility text");
  NSString *imageURL = @"https://faker.image.com/dummyimage.png";
  OCMStub([messageCenterDetailsMock imageUrl]).andReturn(imageURL);
  NSData *imageURLData = [imageURL dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion:YES];
  NSString *imageURLSha = [SwrveUtils sha1:imageURLData];
  [SwrvePluginTests createDummyAsset:imageURLSha];
  OCMStub([messageCenterDetailsMock imageSha]).andReturn(@"0590479d0050002e99f411f72d0d635351134a12");

  // Mock Campaign
  OCMStub([campaignMock ID]).andReturn(44);
  OCMStub([campaignMock subject]).andReturn(@"IAM subject");
  OCMStub([campaignMock name]).andReturn(@"IAM name");
  OCMStub([campaignMock messageCenter]).andReturn(true);
  OCMStub([campaignMock maxImpressions]).andReturn(11111);
  OCMStub([campaignMock priority]).andReturn([NSNumber numberWithInt:(9999)]);
  OCMStub([campaignMock downloadDate]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
  OCMStub([campaignMock dateStart]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
  OCMStub([campaignMock dateEnd]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
  OCMStub([campaignMock messageCenterDetails]).andReturn(messageCenterDetailsMock);
  OCMStub([campaignStateMock status]).andReturn(campaignStateMock);

  OCMExpect([self.swrve messageCenterCampaignWithID:44 andPersonalization:nil]).andReturn(campaignMock);

  XCTestExpectation *promiseResolved = [self expectationWithDescription:@"promiseResolved"];

  [self.plugin getMessageCenterCampaignWithId:44 andPersonalization:nil resolver:^(id result) {

    XCTAssertNotNil(result, @"result should not be null");

    NSDictionary *campaign = result;
    XCTAssertNotNil(campaign, @"Campaign from Message Center should not be null");
    XCTAssertEqualObjects([campaign objectForKey:@"name"], @"IAM name");
    XCTAssertEqualObjects([campaign objectForKey:@"subject"], @"IAM subject");
    XCTAssertEqual([[campaign objectForKey:@"ID"] integerValue], 44);
    XCTAssertTrue([campaign objectForKey:@"messageCenter"], @"messageCenter should be true");
    XCTAssertEqual([[campaign objectForKey:@"maxImpressions"] integerValue], 11111);
    XCTAssertEqual([[campaign objectForKey:@"downloadDate"] integerValue], 1362671700);
    XCTAssertEqual([[campaign objectForKey:@"dateStart"] integerValue], 1362671700);
    XCTAssertEqual([[campaign objectForKey:@"dateEnd"] integerValue], 1362671700);

    NSDictionary *campaignState = [campaign objectForKey:@"state"];
    XCTAssertEqual([[campaignState objectForKey:@"next"] integerValue], 0);
    XCTAssertEqualObjects([campaignState objectForKey:@"status"], @"Unseen");
    XCTAssertEqual([[campaignState objectForKey:@"impressions"] integerValue], 0);
    
    NSDictionary *campaignMessageCenterDetails = [campaign objectForKey:@"messageCenterDetails"];
    XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"subject"], @"IAM message center subject");
//    XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"description"], @"IAM message center description");
    XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"imageAccessibilityText"], @"IAM message center image accesibility text");
    XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"imageURL"], @"https://faker.image.com/dummyimage.png");
    NSString *cacheFolder = [SwrvePluginTests campaignCacheDirectory];
    NSString *assetFilePath = [cacheFolder stringByAppendingPathComponent:imageURLSha];
    XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"image"], assetFilePath);
    XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"imageSha"], @"0590479d0050002e99f411f72d0d635351134a12");

    [SwrvePluginTests removeDummyAsset:imageURLSha];
    [promiseResolved fulfill];

  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    XCTFail(@"Rejected: this should not be rejected");
  }];

  // waiting for resolver to complete
  [self waitForExpectationsWithTimeout:15 handler:^(NSError *error) {
      if (error) {
          XCTFail(@"Ran out of time: GetMessageCenter");
      }
  }];
  
  OCMVerify([self.swrve messageCenterCampaignWithID:44 andPersonalization:nil]);
}

- (void) testGetMessageCenterCampaignUsingFallback {
    SwrveCampaign *campaignMock = OCMClassMock([SwrveCampaign class]);
    SwrveCampaignState *campaignStateMock = OCMClassMock([SwrveCampaignState class]);
    SwrveMessageCenterDetails *messageCenterDetailsMock = OCMClassMock([SwrveMessageCenterDetails class]);

    // Mock Campaign Message Center Details
    OCMStub([messageCenterDetailsMock imageUrl]).andReturn(@"https://faker.image.com/dummyimage.png");
    NSString *imageSha = @"0590479d0050002e99f411f72d0d635351134a12";
    OCMStub([messageCenterDetailsMock imageSha]).andReturn(imageSha);
    [SwrvePluginTests createDummyAsset:imageSha]; // create dummy asset using the fallback instead of the url

    // Mock Campaign
    OCMStub([campaignMock ID]).andReturn(44);
    OCMStub([campaignMock subject]).andReturn(@"IAM subject");
    OCMStub([campaignMock name]).andReturn(@"IAM name");
    OCMStub([campaignMock messageCenter]).andReturn(true);
    OCMStub([campaignMock maxImpressions]).andReturn(11111);
    OCMStub([campaignMock priority]).andReturn([NSNumber numberWithInt:(9999)]);
    OCMStub([campaignMock downloadDate]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
    OCMStub([campaignMock dateStart]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
    OCMStub([campaignMock dateEnd]).andReturn([NSDate dateWithTimeIntervalSince1970:1362671700]);
    OCMStub([campaignMock messageCenterDetails]).andReturn(messageCenterDetailsMock);
    OCMStub([campaignStateMock status]).andReturn(campaignStateMock);

    OCMExpect([self.swrve messageCenterCampaignWithID:44 andPersonalization:nil]).andReturn(campaignMock);

    XCTestExpectation *promiseResolved = [self expectationWithDescription:@"promiseResolved"];

    [self.plugin getMessageCenterCampaignWithId:44 andPersonalization:nil resolver:^(id result) {

        XCTAssertNotNil(result, @"result should not be null");

        NSDictionary *campaign = result;
        NSDictionary *campaignMessageCenterDetails = [campaign objectForKey:@"messageCenterDetails"];
        XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"imageURL"], @"https://faker.image.com/dummyimage.png");
        NSString *cacheFolder = [SwrvePluginTests campaignCacheDirectory];
        NSString *assetFilePath = [cacheFolder stringByAppendingPathComponent:imageSha];
        XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"image"], assetFilePath); // verify that the fallback is the file path
        XCTAssertEqualObjects([campaignMessageCenterDetails objectForKey:@"imageSha"], @"0590479d0050002e99f411f72d0d635351134a12");

        [SwrvePluginTests removeDummyAsset:imageSha];
        [promiseResolved fulfill];

    } rejecter:^(NSString *code, NSString *message, NSError *error) {
        XCTFail(@"Rejected: this should not be rejected");
    }];

    // waiting for resolver to complete
    [self waitForExpectationsWithTimeout:15 handler:^(NSError *error) {
        if (error) {
            XCTFail(@"Ran out of time: GetMessageCenter");
        }
    }];

    OCMVerify([self.swrve messageCenterCampaignWithID:44 andPersonalization:nil]);
}


- (void) testShowMessageCenterCampaign {
  // Mock campaign response since we search by number
  SwrveCampaign *campaignMock = OCMClassMock([SwrveCampaign class]);
  OCMStub([campaignMock ID]).andReturn(88);

  NSArray *mockList = [NSArray arrayWithObject:campaignMock];
  OCMExpect([self.swrve messageCenterCampaigns]).andReturn(mockList);
  
  [self.plugin showMessageCenterCampaignWithId:88 withPersonalization:nil];
  OCMVerify([self.swrve messageCenterCampaigns]);
  OCMVerify([self.swrve showMessageCenterCampaign:campaignMock withPersonalization:nil]);

}

- (void) testRemoveMessageCenterCampaign {
  // Mock campaign response since we search by number
  SwrveCampaign *campaignMock = OCMClassMock([SwrveCampaign class]);
  OCMStub([campaignMock ID]).andReturn(44);

  NSArray *mockList = [NSArray arrayWithObject:campaignMock];
  OCMExpect([self.swrve messageCenterCampaigns]).andReturn(mockList);

  [self.plugin removeMessageCenterCampaignWithId:44];
  
  OCMVerify([self.swrve messageCenterCampaigns]);
  OCMVerify([self.swrve removeMessageCenterCampaign:campaignMock]);
}

- (void) testMarkMessageCenterCampaignAsSeen {
  SwrveCampaign *campaignMock = OCMClassMock([SwrveCampaign class]);
  OCMStub([campaignMock ID]).andReturn(55);

  NSArray *mockList = [NSArray arrayWithObject:campaignMock];
  OCMExpect([self.swrve messageCenterCampaigns]).andReturn(mockList);
  
  [self.plugin markMessageCenterCampaignAsSeenWithId:55];
  
  OCMVerify([self.swrve messageCenterCampaigns]);
  OCMVerify([self.swrve markMessageCenterCampaignAsSeen:campaignMock]);
}

- (void) testPushBufferingWontSendYet {
  [SwrvePluginPushHandler.sharedInstance resetStateForTesting];
  
  id observerMock = OCMObserverMock();
  [NSNotificationCenter.defaultCenter addMockObserver:observerMock name:PUSH_EVENT_NAME object:nil];
  
  [SwrvePlugin initWithAppID:123 apiKey:@"345" config:nil];
  
  [SwrvePluginPushHandler.sharedInstance handleNotificationUserInfo:@{@"key": @"value"}];
  
  // No expectations so should be fine
  OCMVerifyAllWithDelay((OCMockObject*)self.swrve, 1.0);
  OCMVerifyAll(observerMock);
}

- (void) testPushBufferingSendsWhenAttached {
  [SwrvePluginPushHandler.sharedInstance resetStateForTesting];
  
  id observerMock = OCMObserverMock();
  [NSNotificationCenter.defaultCenter addMockObserver:observerMock name:PUSH_EVENT_NAME object:nil];
  [[observerMock expect] notificationWithName:PUSH_EVENT_NAME object:[OCMArg any] userInfo:[OCMArg any]];
  [SwrvePlugin initWithAppID:123 apiKey:@"345" config:nil];
  
  [SwrvePluginPushHandler.sharedInstance handleNotificationUserInfo:@{@"key": @"value"}];
  [self.plugin startedListening];
  
  // Wait a little before verifying
  OCMVerifyAllWithDelay((OCMockObject*)self.swrve, 1.0);
  OCMVerifyAll(observerMock);
}

- (void) testEmbeddedMessageWasShownToUser {
  NSDictionary *cache = @{
    @"campaigns": @[
        @{
          @"id": @55,
          @"start_date": @1630510173000,
          @"end_date": @2145920400000,
          @"rules": @{
              @"delay_first_message": @180,
              @"dismiss_after_views": @99999,
              @"display_order": @"random",
              @"min_delay_between_messages": @60
          },
          @"message_center": @true,
          @"embedded_message": @{
              @"id": @5,
              @"name": @"Test message",
              @"data": @"Hello test message",
              @"type": @"other",
              @"rules": @{},
              @"priority": @9999,
              @"subject": @""
          }
        }
    ]
  };
  OCMStub([self.plugin getCache]).andReturn([cache mutableCopy]);
  
  [self.plugin markEmbeddedMessageCampaignAsSeenWithId:55];
  
  OCMVerify([self.swrve embeddedMessageWasShownToUser:[OCMArg isKindOfClass:[SwrveEmbeddedMessage class]]]);
}

- (void)testEmbeddedControlMessageImpressionEvent {
  NSDictionary *cache = @{
    @"campaigns": @[
        @{
          @"id": @55,
          @"start_date": @1630510173000,
          @"end_date": @2145920400000,
          @"rules": @{
              @"delay_first_message": @180,
              @"dismiss_after_views": @99999,
              @"display_order": @"random",
              @"min_delay_between_messages": @60
          },
          @"message_center": @true,
          @"embedded_message": @{
              @"id": @5,
              @"name": @"Test message",
              @"data": @"Hello test message",
              @"type": @"other",
              @"rules": @{},
              @"priority": @9999,
              @"subject": @""
          }
        }
    ]
  };
  OCMStub([self.plugin getCache]).andReturn([cache mutableCopy]);
  
  [self.plugin embeddedControlMessageImpressionEventWithId: 55];
  
  OCMVerify([self.swrve embeddedControlMessageImpressionEvent:[OCMArg isKindOfClass:[SwrveEmbeddedMessage class]]]);
}

- (void) testMarkEmbeddedMessageButtonAsPressed {
  NSDictionary *cache = @{
    @"campaigns": @[
        @{
          @"id": @55,
          @"start_date": @1630510173000,
          @"end_date": @2145920400000,
          @"rules": @{
              @"delay_first_message": @180,
              @"dismiss_after_views": @99999,
              @"display_order": @"random",
              @"min_delay_between_messages": @60
          },
          @"message_center": @true,
          @"embedded_message": @{
              @"id": @5,
              @"name": @"Test message",
              @"data": @"Hello test message",
              @"type": @"other",
              @"rules": @{},
              @"priority": @9999,
              @"subject": @""
          }
        }
    ]
  };
  OCMStub([self.plugin getCache]).andReturn([cache mutableCopy]);
  
  [self.plugin markEmbeddedMessageButtonAsPressedWithId:55 forButton:@"Button one"];
  
  OCMVerify([self.swrve embeddedButtonWasPressed:[OCMArg isKindOfClass:[SwrveEmbeddedMessage class]] buttonName:[OCMArg isEqual:(@"Button one")]]);
}

- (void) testGetPersonalizedText {
  [self.plugin getPersonalizedTextWithText:@"test string" andPersonalization:@{@"key": @"value"} resolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve personalizeText:[OCMArg any] withPersonalization:[OCMArg any]]);
}

- (void) testGetPersonalizedEmbeddedMessageData {
  NSDictionary *cache = @{
    @"campaigns": @[
        @{
          @"id": @55,
          @"start_date": @1630510173000,
          @"end_date": @2145920400000,
          @"rules": @{
              @"delay_first_message": @180,
              @"dismiss_after_views": @99999,
              @"display_order": @"random",
              @"min_delay_between_messages": @60
          },
          @"message_center": @true,
          @"embedded_message": @{
              @"id": @5,
              @"name": @"Test message",
              @"data": @"Hello test message",
              @"type": @"other",
              @"rules": @{},
              @"priority": @9999,
              @"subject": @""
          }
        }
    ]
  };
  OCMStub([self.plugin getCache]).andReturn([cache mutableCopy]);
  
  [self.plugin getPersonalizedEmbeddedMessageDataWithId:55 andPersonalization:@{@"key": @"value"} resolver:^(id result) {
    // doesn't have to do anything we are verifying it was called.
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
    
  }];

  OCMVerify([self.swrve personalizeEmbeddedMessageData:[OCMArg any] withPersonalization:[OCMArg any]]);
}


+ (void)createDummyAsset:(NSString *)asset {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    [fileManager createDirectoryAtPath:[SwrvePluginTests campaignCacheDirectory] withIntermediateDirectories:YES attributes:nil error:nil];
    NSData *dummyData = [@"TestData" dataUsingEncoding:NSASCIIStringEncoding];
    NSString *path = [[SwrvePluginTests campaignCacheDirectory] stringByAppendingPathComponent:asset];
    [fileManager createFileAtPath:path contents:dummyData attributes:nil];
}

+ (NSString *)campaignCacheDirectory {
    return [[SwrvePluginTests rootCacheDirectory] stringByAppendingPathComponent:@"com.ngt.msgs"];
}

+ (NSString *)rootCacheDirectory {
    static NSString *_dir = nil;
    if (!_dir) {
        _dir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
    }
    return _dir;
}

+ (void)removeDummyAsset:(NSString *)asset {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSString *path = [[SwrvePluginTests campaignCacheDirectory] stringByAppendingPathComponent:asset];
    [fileManager removeItemAtPath:path error:nil];
}

@end
