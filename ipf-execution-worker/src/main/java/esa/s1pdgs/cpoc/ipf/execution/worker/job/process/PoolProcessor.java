package esa.s1pdgs.cpoc.ipf.execution.worker.job.process;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfExecutionWorkerProcessExecutionException;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobPoolDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobTaskDto;
import esa.s1pdgs.cpoc.report.ReportingFactory;

/**
 * Launch tasks in parallel of a job pool
 */
public class PoolProcessor {

    /**
     * Logger
     */
    private static final Logger LOGGER =
            LogManager.getLogger(PoolProcessor.class);

    /**
     * List of pool
     */
    private final LevelJobPoolDto pool;

    /**
     * Tasks executor service
     */
    private final ExecutorService execSrv;

    /**
     * Tasks completion service
     */
    private final CompletionService<TaskResult> completionSrv;

    /**
     * Number of task in the pool
     */
    private final int nbTasks;

    /**
     * Path of the job order to given in argument of each tasks
     */
    private final String jobOrderPath;

    /**
     * Working directory: directory for launching tasks
     */
    private final String workDirectory;

    /**
     * Prefix for monitor logs
     */
    private final String prefixLogs;

    /**
     * Timeout for processing one tasks. After this time the tasks is considered
     * as failed and all tasks are shutdown
     */
    private final long tmProcessOneTaskS;

    /**
     * @param pool
     * @param jobOrderPath
     * @param workDirectory
     * @param prefixMonitorLogs
     * @param timeoutProcessOneTaskS
     */
    public PoolProcessor(final LevelJobPoolDto pool, final String jobOrderPath,
            final String workDirectory, final String prefixLogs,
            final long tmProcessOneTaskS) {
        this.pool = pool;
        this.nbTasks = pool.getTasks().size();
        this.execSrv = Executors.newFixedThreadPool(this.nbTasks);
        this.completionSrv = new ExecutorCompletionService<>(execSrv);
        this.jobOrderPath = jobOrderPath;
        this.workDirectory = workDirectory;
        this.prefixLogs = prefixLogs;
        this.tmProcessOneTaskS = tmProcessOneTaskS;
    }

    /**
     * Launch all tasks and wait for the completion of each tasks. <br/>
     * W>hen one fails (exit code > 127 or exception raised), all the other
     * running tasks are shutdown and an exception is raised
     * 
     * @throws AbstractCodedException
     */
    public void process(final ReportingFactory reportingFactory) throws AbstractCodedException {
        boolean stopAllProcessCall = false;     
        try {
            try {
                LOGGER.info("{} 1 - Submitting tasks {}", prefixLogs,
                        pool.getTasks());
                for (final LevelJobTaskDto task : pool.getTasks()) {               	
                    completionSrv.submit(new TaskCallable(task.getBinaryPath(),
                            jobOrderPath, workDirectory, reportingFactory));
                }
                LOGGER.info("{} 2 - Waiting for tasks execution", prefixLogs);
                for (int i = 0; i < nbTasks; i++) {
                    // Check if interrupted
                    if (isInterrupted()) {
                        stopAllProcessCall = true;
                        throw new InternalErrorException(
                                "Pool Processing thread has been interrupted");
                    }
                    // Wait for a task
                    waitNextTaskResult();
                }
            } catch (InterruptedException | TimeoutException e) {
                stopAllProcessCall = true;
                throw new InternalErrorException(e.getMessage(), e);
            } catch (final ExecutionException e) {
                stopAllProcessCall = true;
                if (e.getCause() instanceof AbstractCodedException) {
                    throw (AbstractCodedException) e.getCause();
                } else {
                    throw new InternalErrorException(e.getMessage(), e);
                }
            } finally {
                if (stopAllProcessCall) {

                    this.stopAllTasks();

                }
            }
        } catch (final InterruptedException ie) {
            throw new InternalErrorException(ie.getMessage(), ie);
        }
    }

    /**
     * Return if thread is interrupted
     * 
     * @return
     */
    private boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * Wait for the result of the next completed task:<br/>
     * if code = 0 => OK <br/>
     * if 0 < code <= 127 => warning <br/>
     * if 127 < code => raise exception
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @throws IpfExecutionWorkerProcessExecutionException
     */
    private void waitNextTaskResult() throws InterruptedException,
            ExecutionException, TimeoutException, IpfExecutionWorkerProcessExecutionException {
        final TaskResult r = completionSrv.take().get(this.tmProcessOneTaskS,
                TimeUnit.SECONDS);
        final int exitCode = r.getExitCode();
        final String task = r.getBinary();
        if (exitCode == 0) {
            LOGGER.info("{} 2 - Task {} successfully executed", this.prefixLogs,
                    task);
        } else if ((exitCode >= 0 && exitCode < 128) || exitCode == 255 && task.contains("S1AIOProcessor")/* special case aio single channel session S1PRO-1512 FIXME*/) {
            LOGGER.warn("{} 2 - Task {} exit with warning code {}",
                    this.prefixLogs, task, exitCode);
        } else {
            this.stopAllTasks();
            throw new IpfExecutionWorkerProcessExecutionException(exitCode,
                    "Task " + task + " failed");
        }
    }

    /**
     * Stop all tasks
     * @throws InterruptedException
     */
    private void stopAllTasks() throws InterruptedException {
        this.execSrv.shutdownNow();
        if (!this.execSrv.awaitTermination(this.tmProcessOneTaskS,
                TimeUnit.SECONDS)) {
            // TODO send kill
        }
    }

}