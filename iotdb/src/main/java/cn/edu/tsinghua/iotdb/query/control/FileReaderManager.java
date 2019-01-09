package cn.edu.tsinghua.iotdb.query.control;

import cn.edu.tsinghua.iotdb.concurrent.IoTThreadFactory;
import cn.edu.tsinghua.iotdb.conf.TsfileDBDescriptor;
import cn.edu.tsinghua.tsfile.read.TsFileSequenceReader;
import cn.edu.tsinghua.tsfile.read.UnClosedTsFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p> Singleton pattern, to manage all file reader.
 * Manage all opened file streams, to ensure that each file will be opened at most once.
 */
public class FileReaderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderManager.class);

    /**
     * max file stream storage number, must be lower than 65535
     */
    private static final int MAX_CACHED_FILE_SIZE = 30000;

    /**
     * key of fileReaderMap file path, value of fileReaderMap is its unique reader.
     */
    private ConcurrentHashMap<String, TsFileSequenceReader> fileReaderMap;

    /**
     * key of fileReaderMap file path, value of fileReaderMap is its reference count.
     */
    private ConcurrentHashMap<String, AtomicInteger> referenceMap;

    private FileReaderManager() {
        fileReaderMap = new ConcurrentHashMap<>();
        referenceMap = new ConcurrentHashMap<>();

        clearUnUsedFilesInFixTime();
    }

    /**
     * Given a file path, tsfile or unseq tsfile, return a <code>TsFileSequenceReader</code> which
     * opened this file.
     */
    public synchronized TsFileSequenceReader get(String filePath, boolean isUnClosed) throws IOException {

        if (!fileReaderMap.containsKey(filePath)) {

            if (fileReaderMap.size() >= MAX_CACHED_FILE_SIZE) {
                LOGGER.warn("Query has opened {} files !", fileReaderMap.size());
            }

            TsFileSequenceReader tsFileReader = isUnClosed ? new UnClosedTsFileReader(filePath) : new TsFileSequenceReader(filePath);

            fileReaderMap.put(filePath, tsFileReader);
            return tsFileReader;
        }

        return fileReaderMap.get(filePath);
    }

    /**
     * Increase the usage reference of given file path.
     * Only when the reference of given file path equals to zero, the corresponding file reader can be closed and remove.
     */
    public void increaseFileReaderReference(String filePath) {
        referenceMap.computeIfAbsent(filePath, k -> new AtomicInteger()).getAndIncrement();
    }

    /**
     * Decrease the usage reference of given file path.
     * This method doesn't need lock.
     * Only when the reference of given file path equals to zero, the corresponding file reader can be closed and remove.
     */
    public synchronized void decreaseFileReaderReference(String filePath) {
        referenceMap.get(filePath).getAndDecrement();
    }

    /**
     * This method is used when the given file path is deleted.
     */
    public synchronized void closeFileAndRemoveReader(String filePath) throws IOException {
        if (fileReaderMap.containsKey(filePath)) {
            referenceMap.remove(filePath);
            fileReaderMap.get(filePath).close();
            fileReaderMap.remove(filePath);
        }
    }

    /**
     * Only used for <code>EnvironmentUtils.cleanEnv</code> method.
     * To make sure that unit test and integration test will not make conflict.
     */
    public synchronized void closeAndRemoveAllOpenedReaders() throws IOException {
        for (Map.Entry<String, TsFileSequenceReader> entry : fileReaderMap.entrySet()) {
            entry.getValue().close();
            referenceMap.remove(entry.getKey());
            fileReaderMap.remove(entry.getKey());
        }
    }

    /**
     * This method is only used for unit test
     */
    public synchronized boolean contains(String filePath) {
        return fileReaderMap.containsKey(filePath);
    }

    private void clearUnUsedFilesInFixTime() {
        long examinePeriod = TsfileDBDescriptor.getInstance().getConfig().cacheFileReaderClearPeriod;

        ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1,
                new IoTThreadFactory("opended-files-manager"));

        service.scheduleAtFixedRate(() -> {
            synchronized (FileReaderManager.class) {
                for (Map.Entry<String, TsFileSequenceReader> entry : fileReaderMap.entrySet()) {
                    TsFileSequenceReader reader = entry.getValue();
                    int referenceNum = referenceMap.get(entry.getKey()).get();

                    if (referenceNum == 0) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            LOGGER.error("Can not close TsFileSequenceReader {} !", reader.getFileName());
                        }
                        fileReaderMap.remove(entry.getKey());
                        referenceMap.remove(entry.getKey());
                    }
                }
            }
        },0, examinePeriod, TimeUnit.MILLISECONDS);
    }

    private static class FileReaderManagerHelper {
        public static FileReaderManager INSTANCE = new FileReaderManager();
    }

    public static FileReaderManager getInstance() {
        return FileReaderManagerHelper.INSTANCE;
    }
}