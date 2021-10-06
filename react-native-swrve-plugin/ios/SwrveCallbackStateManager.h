#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface SwrveCallbackStateManager : NSObject

@property (nonatomic) bool isListeningCustom;

+ (instancetype)sharedInstance;

@end

NS_ASSUME_NONNULL_END
