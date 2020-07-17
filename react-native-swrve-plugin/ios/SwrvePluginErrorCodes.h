NSString* CREATE_INSTANCE_FAILED = @"CREATE_INSTANCE_FAILED";
NSString* INVALID_ARGUMENT = @"CREATE_INSTANCE_FAILED";
NSString* EXCEPTION = @"EXCEPTION";

NSString* swrveResponseCode(int swrveError) {
    return [NSString stringWithFormat:@"SWRVE_RESPONSE %d", swrveError];
}

NSError* generalSwrveError(NSInteger code) {
    return [NSError errorWithDomain:NSCocoaErrorDomain code:code userInfo:nil];
}
