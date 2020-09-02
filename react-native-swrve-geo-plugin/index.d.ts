export function start();
export function stop();

export function isStarted(): Promise<boolean>;
export function getVersion(): Promise<string>;
