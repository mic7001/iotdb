package cn.edu.tsinghua.tsfile.timeseries.read;

import cn.edu.tsinghua.tsfile.compress.UnCompressor;
import cn.edu.tsinghua.tsfile.file.metadata.enums.CompressionTypeName;
import cn.edu.tsinghua.tsfile.file.utils.ReadWriteThriftFormatUtils;
import cn.edu.tsinghua.tsfile.format.PageHeader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jinrui Zhang
 * PageReader is used to read a page in a column.
 */
public class PageReader {

    // constructed when reading one column data in one rowGroup
    private ByteArrayInputStream bis;

    // PageHeader of the page to be read
    private PageHeader pageHeader = null;

    // used to uncompress the page
    private UnCompressor unCompressor = null;

    /**
     * init by ByteArrayInputStream and CompressionTypeName
     * @param bis
     * @param compressionTypeName
     */
    public PageReader(ByteArrayInputStream bis, CompressionTypeName compressionTypeName) {
        this.bis = bis;
        unCompressor = UnCompressor.getUnCompressor(compressionTypeName);
    }

    /**
     * if unread data exists in {@code bis}
     * @return
     */
    public boolean hasNextPage() {
        if (bis.available() > 0)
            return true;
        return false;
    }

    /**
     * get next PageHeader from cache or read from disk
     * @return next PageHeader
     * @throws IOException exception in reading PageHeader
     */
    public PageHeader getNextPageHeader() throws IOException {
        // if current PageHeader exists, return it
        if (pageHeader != null) {
            return pageHeader;
        }
        // else read a new PageHeader from {@code bis}
        if (bis.available() > 0) {
            pageHeader = ReadWriteThriftFormatUtils.readPageHeader(bis);
            return pageHeader;

        }
        return null;
    }


    /**
     * read next page(PageHeader, Page data) from disk
     * @return uncompressed page data in a stream
     * @throws IOException exception when read page from disk
     */
    public ByteArrayInputStream getNextPage() throws IOException {
        if (bis.available() > 0) {
            // get PageHeader first
            pageHeader = getNextPageHeader();
            // read Page in bytes array form
            int pageSize = pageHeader.getCompressed_page_size();

            // the raw data read from disk
            byte[] pageContent = new byte[pageSize];
            bis.read(pageContent, 0, pageSize);

            // uncompress the raw data
            pageContent = unCompressor.uncompress(pageContent);
            // reset current PageHeader to null
            pageHeader = null;
            // put bytes array into new ByteArrayInputStream and return it
            return new ByteArrayInputStream(pageContent);
        }
        return null;
    }

    /**
     * read {@code pageSize} bytes from {@code in}
     * @param in
     * @param buf
     * @param pageSize
     * @throws IOException
     */
    public void readPage(InputStream in, byte[] buf, int pageSize) throws IOException {
        in.read(buf, 0, pageSize);
    }

    /**
     * skip the current page to next page
     */
    public void skipCurrentPage() {
        long skipSize = this.pageHeader.getCompressed_page_size();
        bis.skip(skipSize);
        // reset current PageHeader to null
        pageHeader = null;
    }
}
