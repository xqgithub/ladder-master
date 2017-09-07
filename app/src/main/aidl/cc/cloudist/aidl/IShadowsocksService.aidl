package cc.cloudist.aidl;

import cc.cloudist.aidl.Config;
import cc.cloudist.aidl.IShadowsocksServiceCallback;

interface IShadowsocksService {
  int getState();

  oneway void registerCallback(IShadowsocksServiceCallback cb);
  oneway void unregisterCallback(IShadowsocksServiceCallback cb);

  oneway void use(in Config config);
}
