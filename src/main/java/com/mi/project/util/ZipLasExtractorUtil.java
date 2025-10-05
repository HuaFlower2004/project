package com.mi.project.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ZipLasExtractorUtil {
    /**
     * 解压zip文件中的第一个las文件到指定目录，并返回该las文件的路径。
     * @param zipFile zip文件路径
     * @param destDir 解压目标目录
     * @return 第一个las文件的路径，如果没有则返回null
     * @throws IOException
     */
    public static Path extractFirstLasFileFromZip(Path zipFile, Path destDir) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".las")) {
                    Path outPath = destDir.resolve(entry.getName());
                    Files.createDirectories(outPath.getParent());
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                    return outPath;
                }
            }
        }
        return null;
    }
}
