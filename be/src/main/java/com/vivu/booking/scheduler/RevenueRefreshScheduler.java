package com.vivu.booking.scheduler;

import com.vivu.booking.service.impl.RevenueServiceImpl;
import com.vivu.booking.utils.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lam moi matview doanh thu dinh ky — tuong tu {@link HoldExpireScheduler}: scheduler chi goi,
 * logic refresh nam trong {@link com.vivu.booking.service.impl.RevenueServiceImpl#refreshMatView}.
 *
 * <h2>Vì sao can matview rieng ma VIEW chua du?</h2>
 * <p>
 * VIEW tinh lai moi lan doc — hop cho chi tiet ngay. MATVIEW la <b>ban sao vat ly</b>:
 * doc nhanh cho bieu do thang/nam (chi vai chuc dong, khong aggregate hang trieu night),
 * nhung phai REFRESH sau moi lan data doi. Chi lam moi dinh ky thay vi sau moi don hang
 * (qua ton) cung la mot bai hoc thiet ke.
 *
 * <h2>Vì sao khong {@code @Scheduled} hay Timer</h2>
 * Day la Servlet/Tomcat thuan — Executor giam chu ky song, Timer chet im neu task nem exception.
 */
public class RevenueRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(RevenueRefreshScheduler.class);
    private static final long INITIAL_DELAY_SECONDS = 30;
    private static final int PERIOD_MINUTES = Integer.parseInt(
            com.vivu.booking.utils.AppProperties.get("report.refresh-interval-minutes", "15"));
    private static final String JOB_LOCK = "job:revenue-refresh";

    private final RevenueServiceImpl revenueService;
    private ScheduledExecutorService executor;

    public RevenueRefreshScheduler(RevenueServiceImpl revenueService) { this.revenueService = revenueService; }
    public RevenueRefreshScheduler() { this(new RevenueServiceImpl()); }

    public void start() {
        if (executor != null && !executor.isShutdown()) {
            log.warn("RevenueRefreshScheduler dang chay — bo qua lan start tiep theo");
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "revenue-refresh");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::runQuietly, INITIAL_DELAY_SECONDS, PERIOD_MINUTES, TimeUnit.MINUTES);
        log.info("RevenueRefreshScheduler khoi dong (initialDelay={}s, period={}m)", INITIAL_DELAY_SECONDS, PERIOD_MINUTES);
    }

    public void stop() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RevenueRefreshScheduler da dung");
    }

    /** REFRESH matview — nhieu node Tomcat thi chi 1 node gianh duoc job lock. */
    private void runQuietly() {
        try {
            int done = RedisLockUtil.withLock(JOB_LOCK, 120, 0L,
                    () -> {
                        revenueService.refreshMatView();
                        return 1;
                    }, () -> 0);
            if (done > 0) log.info("Revenue matview da refresh (job lock gianh duoc)");
        } catch (Exception e) {
            log.error("Revenue matview refresh lot so (giu nguyen chu ky): {}", e.toString(), e);
        }
    }
}
