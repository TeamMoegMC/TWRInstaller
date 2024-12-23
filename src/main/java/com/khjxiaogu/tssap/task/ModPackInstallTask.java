package com.khjxiaogu.tssap.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

import com.khjxiaogu.tssap.entity.ModPackFile;
import com.khjxiaogu.tssap.ui.Lang;
import com.khjxiaogu.tssap.util.FileUtil;
import com.khjxiaogu.tssap.util.HashUtil;
import com.khjxiaogu.tssap.util.LogUtil;

public class ModPackInstallTask extends AbstractFileTask {
	ModPackFile packfile;
	long size=1000;
	public ModPackInstallTask(ModPackFile packfile) {
		super(new File(new File("./"),packfile.file));
		this.packfile = packfile;
		if(packfile.size>0)
			size=packfile.size;
	}

	@Override
	public void runTask() {
		Path curfile = file.toPath();
		Path mainloc = new File("./").toPath();
		if (!curfile.startsWith(mainloc)) {// found path outside minecraft, ignore.
			LogUtil.addLog("illegal path "+curfile.toAbsolutePath()+" found, download failed.");
			this.setFailed();
			return;
		}
		if (file.exists()) {
			String sha = HashUtil.SHA256(file);
			if (sha.equalsIgnoreCase(packfile.hash)) {// same file, ignore
				this.setCompleted();
				return;
			}
		}
		file.getParentFile().mkdirs();
		super.backup();
		if (!isFailed()) {
			try {
				HttpURLConnection netConn = FileUtil.fetchWithRetryAndSize(packfile.link, 3);
				long ctl=netConn.getContentLengthLong();
				if(ctl>0)
					size=ctl;
				InputStream netFile=netConn.getInputStream();
				if (packfile.compressed)
					netFile = new InflaterInputStream(netFile);
				FileUtil.transfer(netFile, file);
				this.setCompleted();
				return;
			} catch (IOException e) {
				LogUtil.addError("can not download or create file", e);
			}
			this.setFailed();
		}

	}

	@Override
	public String getTaskDesc() {
		return Lang.getLang("file.update", file);
	}

	@Override
	public String getBackupEntry() {
		return packfile.file;
	}

	@Override
	public long getTaskDifficulty() {
		return size;
	}

}
