package com.iisquare.fs.web.cron.core;

public class StageWorker extends Thread {

    private Stage stage;
    private StagePool pool;

    public StageWorker(StagePool pool) {
        this.pool = pool;
    }

    public void call(Stage stage) {
        this.stage = stage;
        this.start();
    }

    public Stage stage() {
        return stage;
    }

    @Override
    public void run() {
        do {
            try {
                stage.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while ((stage = pool.take()) != null);
        pool.clear(this);
    }
}
