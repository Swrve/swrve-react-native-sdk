#import "SwrveCallbackStateManager.h"

@implementation SwrveCallbackStateManager

+ (instancetype)sharedInstance
{
    static SwrveCallbackStateManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [SwrveCallbackStateManager new];
    });
    return sharedInstance;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.isListeningCustom = NO;
    }
    return self;
}

@end
