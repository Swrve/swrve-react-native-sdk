#import <Foundation/Foundation.h>
@import SwrveSDK;

NS_ASSUME_NONNULL_BEGIN

@interface SwrvePluginUtils : NSObject

+ (NSDictionary<NSString *, NSString *> *)toDictionaryJustStrings:(nonnull NSDictionary *)dictionary;

+ (NSString *)serializeDictonaryToJson:(NSDictionary *)dic withKey:(nullable NSString *)key;

+ (NSError *)produceErrorFromException:(NSException *)exception;

- (void)sendSwrveUserUpdate;

@end

NS_ASSUME_NONNULL_END
