package cn.edu.tsinghua.iotdb.engine.bufferwrite;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import cn.edu.tsinghua.iotdb.engine.memtable.PrimitiveMemTable;
import cn.edu.tsinghua.tsfile.file.metadata.ChunkGroupMetaData;
import cn.edu.tsinghua.tsfile.file.metadata.TsDeviceMetadata;
import cn.edu.tsinghua.tsfile.utils.BytesUtils;
import cn.edu.tsinghua.tsfile.utils.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.edu.tsinghua.iotdb.engine.memtable.IMemTable;
import cn.edu.tsinghua.iotdb.engine.memtable.MemTableTestUtils;
import cn.edu.tsinghua.iotdb.utils.EnvironmentUtils;
import cn.edu.tsinghua.tsfile.write.schema.FileSchema;

public class BufferWriteRestoreManagerTest {

	private BufferWriteRestoreManager bufferwriteResource;
	private String processorName = "processor";
	private String insertPath = "insertfile";
	private String insertRestorePath = insertPath + ".restore";

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
		EnvironmentUtils.cleanDir(insertPath);
		EnvironmentUtils.cleanDir(insertRestorePath);
	}

	@Test
	public void testInitResource() throws IOException {
		bufferwriteResource = new BufferWriteRestoreManager(processorName, insertPath);
		Pair<Long, List<ChunkGroupMetaData>> pair = bufferwriteResource.readRestoreInfo();
		assertEquals(true, new File(insertRestorePath).exists());
		assertEquals(0, (long) pair.left);
		assertEquals(0, pair.right.size());
		FileSchema fileSchema = new FileSchema();
		bufferwriteResource.close(fileSchema);
		assertEquals(false, new File(insertRestorePath).exists());
	}
	
	@Test
	public void testAbnormalRecover() throws IOException{
		bufferwriteResource = new BufferWriteRestoreManager(processorName, insertPath);
		File insertFile = new File(insertPath);
		File restoreFile = new File(insertPath+".restore");
		FileOutputStream fileOutputStream = new FileOutputStream(insertFile);
		// mkdir
		fileOutputStream.write(new byte[400]);
		fileOutputStream.close();
		assertEquals(true, insertFile.exists());
		assertEquals(true, restoreFile.exists());
		assertEquals(400, insertFile.length());
		bufferwriteResource.close(new FileSchema());
		FileOutputStream out = new FileOutputStream(new File(insertRestorePath));
		// write tsfile position using byte[8] which is present one long
		writeRestoreFile(out, 2);
		writeRestoreFile(out, 3);
		byte[] lastPositionBytes = BytesUtils.longToBytes(200);
		out.write(lastPositionBytes);
		out.close();
		bufferwriteResource = new BufferWriteRestoreManager(processorName, insertPath);
		assertEquals(true, insertFile.exists());
		assertEquals(200, insertFile.length());
		assertEquals(insertPath, bufferwriteResource.getInsertFilePath());
		assertEquals(insertRestorePath, bufferwriteResource.getRestoreFilePath());
		bufferwriteResource.close(new FileSchema());
	}

	@Test
	public void testRecover() throws IOException {
		bufferwriteResource = new BufferWriteRestoreManager(processorName, insertPath);
		File insertFile = new File(insertPath);
		File restoreFile = new File(insertPath+".restore");
		FileOutputStream fileOutputStream = new FileOutputStream(insertFile);
		// mkdir
		fileOutputStream.write(new byte[200]);
		fileOutputStream.close();
		FileOutputStream out = new FileOutputStream(new File(insertRestorePath));
		// write tsfile position using byte[8] which is present one long
		writeRestoreFile(out, 2);
		writeRestoreFile(out, 3);
		byte[] lastPositionBytes = BytesUtils.longToBytes(200);
		out.write(lastPositionBytes);
		out.close();
		assertEquals(true, insertFile.exists());
		assertEquals(true, restoreFile.exists());
		BufferWriteRestoreManager tempbufferwriteResource = new BufferWriteRestoreManager(processorName, insertPath);
		assertEquals(true, insertFile.exists());
		assertEquals(200, insertFile.length());
		assertEquals(insertPath, tempbufferwriteResource.getInsertFilePath());
		assertEquals(insertRestorePath, tempbufferwriteResource.getRestoreFilePath());
		tempbufferwriteResource.close(new FileSchema());
		bufferwriteResource.close(new FileSchema());
	}

	@Test
	public void testFlushAndGetMetadata() throws IOException {
		bufferwriteResource = new BufferWriteRestoreManager(processorName, insertPath);
		assertEquals(0, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		IMemTable memTable = new PrimitiveMemTable();
		MemTableTestUtils.produceData(memTable, 10, 100, MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0);
		bufferwriteResource.flush(MemTableTestUtils.getFileSchema(), memTable);
		assertEquals(0, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		bufferwriteResource.appendMetadata();
		assertEquals(1, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		MemTableTestUtils.produceData(memTable, 200, 300, MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0);
		bufferwriteResource.appendMetadata();
		assertEquals(1, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());

		bufferwriteResource.close(MemTableTestUtils.getFileSchema());
	}

	private void writeRestoreFile(OutputStream out, int metadataNum) throws IOException {
		TsDeviceMetadata tsDeviceMetadata = new TsDeviceMetadata();
		List<ChunkGroupMetaData> appendRowGroupMetaDatas = new ArrayList<>();
		for (int i = 0; i < metadataNum; i++) {
			appendRowGroupMetaDatas.add(new ChunkGroupMetaData("d1", new ArrayList<>()));
		}
		tsDeviceMetadata.setChunkGroupMetadataList(appendRowGroupMetaDatas);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		tsDeviceMetadata.serializeTo(baos);
		// write metadata size using int
		int metadataSize = baos.size();
		out.write(BytesUtils.intToBytes(metadataSize));
		// write metadata
		out.write(baos.toByteArray());
	}
}
