package hev.htproxy;

public final class TProxyService {
    static {
        System.loadLibrary("hev-socks5-tunnel"); // libhev-socks5-tunnel.so
    }

    public static native void TProxyStartService(String configPath, int tunFd);
    public static native void TProxyStopService();
    public static native long[] TProxyGetStats();

    private TProxyService() {}
}
