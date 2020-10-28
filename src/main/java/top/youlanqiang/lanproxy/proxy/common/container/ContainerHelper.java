package top.youlanqiang.lanproxy.proxy.common.container;




import java.util.List;

/**
 * 容器启动工具类.
 *
 * @author fengfei
 *
 */
public class ContainerHelper {



    private static volatile boolean running = true;

    private static List<Container> cachedContainers;

    public static void start(List<Container> containers) {

        cachedContainers = containers;

        // 启动所有容器
        startContainers();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                synchronized (top.youlanqiang.lanproxy.proxy.common.container.ContainerHelper.class) {

                    // 停止所有容器.
                    stopContainers();
                    running = false;
                    top.youlanqiang.lanproxy.proxy.common.container.ContainerHelper.class.notify();
                }
            }
        });

        synchronized (top.youlanqiang.lanproxy.proxy.common.container.ContainerHelper.class) {
            while (running) {
                try {
                    top.youlanqiang.lanproxy.proxy.common.container.ContainerHelper.class.wait();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void startContainers() {
        for (Container container : cachedContainers) {
            container.start();
        }
    }

    public static void stopContainers() {
        for (Container container : cachedContainers) {

            try {
                container.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
